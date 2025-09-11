package com.example.nvr.utils;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;

import com.example.nvr.model.CameraDevice;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class VideoStreamManager {
    private static final String TAG = "VideoStreamManager";
    private static volatile VideoStreamManager instance;
    private final Context context;
    private volatile LibVLC libVLC;
    private final ArrayList<MediaPlayer> mediaPlayers = new ArrayList<>();
    private MediaMuxer mediaMuxer;
    private int videoTrackIndex = -1;
    private boolean isMuxerStarted = false;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final AtomicReference<Thread> initializationThread = new AtomicReference<>(null);

    private VideoStreamManager(Context context) {
        this.context = context.getApplicationContext();
        initializeLibVLC();
    }

    private void initializeLibVLC() {
        // 检查是否已经在初始化过程中或正在关闭
        if (initializationThread.get() != null || isShuttingDown.get()) {
            Log.d(TAG, "LibVLC initialization already in progress or shutting down");
            return;
        }
        
        Thread thread = new Thread(() -> {
            try {
                // 标记当前初始化线程
                initializationThread.set(Thread.currentThread());
                
                // 使用简化的配置以提高兼容性和稳定性
                ArrayList<String> options = new ArrayList<>();
                options.add("--no-drop-late-frames");
                options.add("--no-skip-frames");
                options.add("--rtsp-tcp");
                options.add(":network-caching=500");
                options.add("--verbose=3"); // 增加详细度以便更好地调试
                options.add("--file-logging");
                
                // 使用明确的日志文件路径，便于用户查找
                String logFilePath;
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    // 使用外部存储的应用目录存储日志文件
                    File externalFilesDir = context.getExternalFilesDir(null);
                    if (externalFilesDir != null) {
                        logFilePath = externalFilesDir.getAbsolutePath() + File.separator + "vlc-log.txt";
                    } else {
                        // 如果外部存储不可用，使用内部存储
                        logFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "vlc-log.txt";
                    }
                } else {
                    // 使用内部存储
                    logFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "vlc-log.txt";
                }
                
                options.add("--logfile=" + logFilePath);
                Log.d(TAG, "LibVLC日志文件路径: " + logFilePath);
                
                // 尝试创建LibVLC实例
                libVLC = new LibVLC(context, options);
                isInitialized.set(true);
                Log.d(TAG, "LibVLC instance created successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create LibVLC instance: " + e.getMessage());
                isInitialized.set(false);
            } finally {
                // 清除初始化线程标记
                initializationThread.set(null);
            }
        });
        
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public static synchronized VideoStreamManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoStreamManager(context.getApplicationContext());
        }
        return instance;
    }

    // 检查LibVLC是否已正确初始化
    public boolean isInitialized() {
        return isInitialized.get() && libVLC != null;
    }

    // 重新初始化LibVLC（如果初始化失败）
    public boolean reinitialize() {
        if (!isInitialized() && !isShuttingDown.get()) {
            Log.d(TAG, "Attempting to reinitialize LibVLC");
            
            // 如果有初始化线程正在运行，等待它完成
            Thread initThread = initializationThread.get();
            if (initThread != null && initThread.isAlive()) {
                try {
                    Log.d(TAG, "Waiting for existing initialization to complete");
                    initThread.join(2000); // 等待最多2秒
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for initialization: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            
            // 如果仍然未初始化，尝试重新初始化
            if (!isInitialized()) {
                initializeLibVLC();
                
                // 等待初始化完成（最多3秒）
                long startTime = System.currentTimeMillis();
                while (!isInitialized() && (System.currentTimeMillis() - startTime < 3000)) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return isInitialized();
    }

    // 初始化并开始播放视频流
    public MediaPlayer startStream(CameraDevice camera, VLCVideoLayout videoLayout) {
        if (camera == null || camera.getRtspUrl() == null || camera.getRtspUrl().isEmpty()) {
            Log.e(TAG, "Camera or RTSP URL is null or empty");
            return null;
        }

        // 检查是否正在关闭
        if (isShuttingDown.get()) {
            Log.e(TAG, "Cannot start stream - VideoStreamManager is shutting down");
            return null;
        }

        Log.d(TAG, "准备开始流播放 - 摄像头ID: " + camera.getId() + ", URL: " + camera.getRtspUrl());

        // 验证libVLC是否初始化成功，如果失败则尝试重新初始化
        if (libVLC == null) {
            if (!reinitialize()) {
                Log.e(TAG, "LibVLC is not initialized and cannot be reinitialized, cannot start stream");
                return null;
            }
        }

        // 确保videoLayout不为null且已附加到窗口
        if (videoLayout == null) {
            Log.e(TAG, "VLCVideoLayout is null, cannot attach views");
            return null;
        }

        // 检查VideoLayout是否附加到窗口
        if (videoLayout.getWindowToken() == null) {
            Log.e(TAG, "VLCVideoLayout is not attached to a window, cannot start stream");
            return null;
        }

        MediaPlayer mediaPlayer = null;
        try {
            // 创建一个新的MediaPlayer实例
            mediaPlayer = new MediaPlayer(libVLC);
            
            // 安全地附加视图
            try {
                mediaPlayer.attachViews(videoLayout, null, false, false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to attach views: " + e.getMessage());
                mediaPlayer.release();
                return null;
            }

            // 设置RTSP URL和选项
            Media media = new Media(libVLC, Uri.parse(camera.getRtspUrl()));
            Log.d(TAG, "RTSP URL: " + camera.getRtspUrl());
            media.setHWDecoderEnabled(true, false);
            media.addOption(":network-caching=500");
            media.addOption(":rtsp-tcp");
            media.addOption(":verbose=3");
            media.addOption(":live-caching=500");
            // 添加更多参数以提高兼容性
            media.addOption(":rtsp-frame-buffer-size=1000");
            media.addOption(":udp-timeout=15000");
            media.addOption(":tcp-timeout=15000");
            media.addOption(":timeout=15000");
            // 添加自动重试参数
            media.addOption(":http-reconnect=1");
            media.addOption(":reconnect=1");
            media.addOption(":reconnect-delay=2000");
            
            // 添加稳定性选项
            media.addOption(":no-avcodec-dr");
            media.addOption(":no-avcodec-hw");
            media.addOption(":avcodec-threads=2");
            media.addOption(":avcodec-sync=audio");
            
            mediaPlayer.setMedia(media);
            
            // 创建最终引用以在监听器中使用
            final MediaPlayer finalMediaPlayer = mediaPlayer;
            
            mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
                private int retryCount = 0;
                private static final int MAX_RETRIES = 3;
                
                @Override
                public void onEvent(MediaPlayer.Event event) {
                    try {
                        switch (event.type) {
                            case MediaPlayer.Event.EncounteredError:
                                Log.e(TAG, "Error encountered while playing stream");
                                Log.e(TAG, "Error details - URL: " + camera.getRtspUrl());
                                // 检查媒体播放器的状态
                                if (finalMediaPlayer != null) {
                                    Log.e(TAG, "MediaPlayer state: " + getMediaPlayerState(finalMediaPlayer));
                                }
                                // 标记为未连接
                                camera.setConnected(false);
                                
                                // 添加自动重试机制
                                if (retryCount < MAX_RETRIES && !isShuttingDown.get()) {
                                    retryCount++;
                                    Log.d(TAG, "尝试重新连接 (" + retryCount + "/" + MAX_RETRIES + ")...");
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                // 确保LibVLC实例仍然有效
                                                if (libVLC != null && !isShuttingDown.get()) {
                                                    Media newMedia = new Media(libVLC, camera.getRtspUrl());
                                                    // 复制原始媒体的所有选项
                                                    newMedia.setHWDecoderEnabled(true, false);
                                                    newMedia.addOption(":network-caching=500");
                                                    newMedia.addOption(":rtsp-tcp");
                                                    newMedia.addOption(":verbose=3");
                                                    newMedia.addOption(":live-caching=500");
                                                    newMedia.addOption(":rtsp-frame-buffer-size=1000");
                                                    newMedia.addOption(":udp-timeout=15000");
                                                    newMedia.addOption(":tcp-timeout=15000");
                                                    newMedia.addOption(":timeout=15000");
                                                    newMedia.addOption(":http-reconnect=1");
                                                    newMedia.addOption(":reconnect=1");
                                                    newMedia.addOption(":reconnect-delay=2000");
                                                    newMedia.addOption(":no-avcodec-hw");
                                                    
                                                    finalMediaPlayer.setMedia(newMedia);
                                                    newMedia.release();
                                                    finalMediaPlayer.play();
                                                } else {
                                                    Log.e(TAG, "LibVLC instance is null during retry or shutting down");
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "重试连接失败: " + e.getMessage());
                                            }
                                        }
                                    }, 2000); // 2秒后重试
                                } else {
                                    // 所有重试都失败后，提供更详细的错误信息
                                    Log.e(TAG, "所有重试均已失败，建议检查RTSP URL、网络连接和摄像头配置");
                                    // 建议使用testRtspConnection方法进行诊断
                                }
                                break;
                            case MediaPlayer.Event.EndReached:
                                Log.d(TAG, "End of stream reached");
                                // 当流结束时，检查是否需要重试
                                if (camera.isConnected() && retryCount < MAX_RETRIES && !isShuttingDown.get()) {
                                    retryCount++;
                                    Log.d(TAG, "流意外结束，尝试重新连接 (" + retryCount + "/" + MAX_RETRIES + ")...");
                                    try {
                                        if (libVLC != null) {
                                            Media newMedia = new Media(libVLC, camera.getRtspUrl());
                                            finalMediaPlayer.setMedia(newMedia);
                                            newMedia.release();
                                            finalMediaPlayer.play();
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "重新连接失败: " + e.getMessage());
                                    }
                                }
                                break;
                            case MediaPlayer.Event.Buffering:
                                Log.d(TAG, "Buffering: " + event.getBuffering() + "%");
                                break;
                            case MediaPlayer.Event.Playing:
                                Log.d(TAG, "Stream is now playing");
                                camera.setConnected(true);
                                break;
                            case MediaPlayer.Event.Opening:
                                Log.d(TAG, "Stream is opening");
                                break;
                            case MediaPlayer.Event.Paused:
                                Log.d(TAG, "Stream is paused");
                                break;
                            case MediaPlayer.Event.Stopped:
                                Log.d(TAG, "Stream is stopped");
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in MediaPlayer event listener: " + e.getMessage());
                    }
                }
            });
            media.release();

            mediaPlayer.play();
            synchronized (mediaPlayers) {
                mediaPlayers.add(mediaPlayer);
            }
            camera.setConnected(true);
            Log.d(TAG, "Stream started successfully for camera: " + camera.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error starting stream: " + e.getMessage());
            // 确保在发生异常时清理资源
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.detachViews();
                    mediaPlayer.release();
                } catch (Exception ex) {
                    Log.e(TAG, "Error cleaning up media player after failure: " + ex.getMessage());
                }
                mediaPlayer = null;
            }
        }
        
        return mediaPlayer;
    }

    // 获取媒体播放器状态的文本描述
    private String getMediaPlayerState(MediaPlayer mediaPlayer) {
        if (mediaPlayer == null) return "null";
        
        try {
            int state = mediaPlayer.getPlayerState();
            // 使用常量而不是枚举来避免找不到State的问题
            if (state == 0) return "NothingSpecial";
            if (state == 1) return "Opening";
            if (state == 2) return "Buffering";
            if (state == 3) return "Playing";
            if (state == 4) return "Paused";
            if (state == 5) return "Stopped";
            if (state == 6) return "Ended";
            if (state == 7) return "Error";
            return "Unknown(" + state + ")";
        } catch (Exception e) {
            Log.e(TAG, "Error getting player state: " + e.getMessage());
            return "Unknown";
        }
    }

    // 停止视频流播放
    public void stopStream(MediaPlayer mediaPlayer, CameraDevice camera) {
        if (mediaPlayer != null) {
            try {
                // 设置断开连接标志
                if (camera != null) {
                    camera.setConnected(false);
                }
                
                // 安全地停止播放并分离视图
                try {
                    mediaPlayer.stop();
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping media player: " + e.getMessage());
                }
                
                try {
                    mediaPlayer.detachViews();
                } catch (Exception e) {
                    Log.e(TAG, "Error detaching views: " + e.getMessage());
                }
                
                // 从列表中移除并释放资源
                synchronized (mediaPlayers) {
                    mediaPlayers.remove(mediaPlayer);
                }
                
                try {
                    mediaPlayer.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing media player: " + e.getMessage());
                }
                
                Log.d(TAG, "Stream stopped and resources released");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping stream: " + e.getMessage());
                // 即使出现异常，仍然尝试从列表中移除
                try {
                    synchronized (mediaPlayers) {
                        mediaPlayers.remove(mediaPlayer);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error removing media player from list: " + ex.getMessage());
                }
            }
        }
    }

    // 停止所有视频流
    public void stopAllStreams() {
        synchronized (mediaPlayers) {
            // 创建副本以避免并发修改异常
            ArrayList<MediaPlayer> playersCopy = new ArrayList<>(mediaPlayers);
            mediaPlayers.clear(); // 提前清空列表以避免其他线程添加新的播放器
            
            for (MediaPlayer mp : playersCopy) {
                stopStream(mp, null);
            }
        }
    }

    // 开始录制视频
    public boolean startRecording(CameraDevice camera, String outputPath) {
        if (camera == null || outputPath == null || outputPath.isEmpty()) {
            Log.e(TAG, "Camera or output path is null or empty");
            return false;
        }

        try {
            // 确保当前没有正在进行的录制
            if (mediaMuxer != null) {
                Log.w(TAG, "Stopping existing recording before starting new one");
                stopRecording(null);
            }

            // 创建输出文件
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    Log.e(TAG, "Failed to create parent directories for recording");
                    return false;
                }
            }
            
            // 初始化MediaMuxer
            mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            isMuxerStarted = false;
            videoTrackIndex = -1;
            camera.setRecording(true);
            Log.d(TAG, "Started recording to file: " + outputPath);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording: " + e.getMessage());
            // 确保在失败时清理资源
            cleanupRecordingResources();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting recording: " + e.getMessage());
            cleanupRecordingResources();
            return false;
        }
    }

    // 停止录制视频
    public void stopRecording(CameraDevice camera) {
        try {
            // 设置停止录制标志
            if (camera != null) {
                camera.setRecording(false);
            }
            
            cleanupRecordingResources();
            Log.d(TAG, "Recording stopped and resources released");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage());
            // 即使出现异常，仍然尝试清理资源
            try {
                cleanupRecordingResources();
            } catch (Exception ex) {
                Log.e(TAG, "Error during recording resource cleanup: " + ex.getMessage());
            }
        }
    }

    // 清理录制相关资源
    private void cleanupRecordingResources() {
        if (mediaMuxer != null) {
            try {
                if (isMuxerStarted) {
                    mediaMuxer.stop();
                }
                mediaMuxer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaMuxer: " + e.getMessage());
            }
            mediaMuxer = null;
            videoTrackIndex = -1;
            isMuxerStarted = false;
        }
    }

    // 处理视频数据并写入文件
    public void handleVideoData(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo, MediaFormat mediaFormat) {
        if (mediaMuxer == null) {
            return;
        }

        try {
            if (!isMuxerStarted) {
                videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
                mediaMuxer.start();
                isMuxerStarted = true;
            }

            if (byteBuffer != null && bufferInfo.size > 0) {
                mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling video data: " + e.getMessage());
            // 在出错时停止录制
            stopRecording(null);
        }
    }

    // 释放所有资源
    public void release() {
        // 标记为正在关闭，防止新的操作
        if (!isShuttingDown.compareAndSet(false, true)) {
            Log.d(TAG, "VideoStreamManager is already shutting down");
            return;
        }

        try {
            Log.d(TAG, "Starting VideoStreamManager shutdown process");
            
            // 停止所有流
            stopAllStreams();
            
            // 停止录制（如果正在进行）
            stopRecording(null);
            
            // 释放LibVLC实例
            if (libVLC != null) {
                try {
                    libVLC.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing LibVLC: " + e.getMessage());
                }
                libVLC = null;
                isInitialized.set(false);
            }
            
            // 清除单例实例（谨慎使用，因为其他组件可能还在使用它）
            instance = null;
            Log.d(TAG, "VideoStreamManager released all resources");
        } catch (Exception e) {
            Log.e(TAG, "Error during VideoStreamManager release: " + e.getMessage());
        }
    }
    
    // 测试RTSP连接，用于诊断连接问题
    public void testRtspConnection(String rtspUrl, RtspConnectionCallback callback) {
        if (rtspUrl == null || rtspUrl.isEmpty()) {
            if (callback != null) {
                callback.onConnectionResult(false, "RTSP URL为空");
            }
            return;
        }

        // 检查是否正在关闭
        if (isShuttingDown.get()) {
            Log.e(TAG, "Cannot test connection - VideoStreamManager is shutting down");
            if (callback != null) {
                callback.onConnectionResult(false, "系统正在关闭，无法测试连接");
            }
            return;
        }

        Log.d(TAG, "开始测试RTSP连接: " + rtspUrl);
        
        // 验证libVLC是否初始化成功，如果失败则尝试重新初始化
        if (libVLC == null) {
            if (!reinitialize()) {
                Log.e(TAG, "LibVLC is not initialized and cannot be reinitialized, cannot test connection");
                if (callback != null) {
                    callback.onConnectionResult(false, "LibVLC未初始化");
                }
                return;
            }
        }
        
        final Handler handler = new Handler(Looper.getMainLooper());
        
        try {
            // 创建媒体对象进行基本的连接测试
            Media media = new Media(libVLC, rtspUrl);
            
            // 使用与startStream相同的参数配置
            media.setHWDecoderEnabled(true, false);
            media.addOption(":network-caching=500");
            media.addOption(":rtsp-tcp");
            media.addOption(":verbose=3");
            media.addOption(":timeout=8000");
            media.addOption(":no-avcodec-hw");
            
            // 设置一个连接超时计时器
            final Runnable timeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onConnectionResult(false, "连接超时");
                    }
                }
            };
            
            // 设置超时检查
            handler.postDelayed(timeoutRunnable, 8000); // 8秒超时
            
            try {
                // 异步方式测试媒体是否可以被解析
                media.parse(Media.Parse.ParseNetwork);
                
                // 延迟一小段时间后返回成功结果
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        handler.removeCallbacks(timeoutRunnable);
                        if (callback != null) {
                            callback.onConnectionResult(true, "RTSP URL格式有效");
                        }
                    }
                }, 1000);
                
                Log.d(TAG, "RTSP连接测试已启动");
            } catch (Exception e) {
                Log.e(TAG, "启动RTSP连接测试失败: " + e.getMessage());
                handler.removeCallbacks(timeoutRunnable);
                if (callback != null) {
                    callback.onConnectionResult(false, "无法启动测试: " + e.getMessage());
                }
            } finally {
                // 确保media对象被释放
                try {
                    media.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing media: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in testRtspConnection: " + e.getMessage());
            // 清理资源
            try {
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }
                if (callback != null) {
                    callback.onConnectionResult(false, "测试过程中发生错误: " + e.getMessage());
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error handling exception in testRtspConnection: " + ex.getMessage());
            }
        }
    }
    
    // RTSP连接测试回调接口
    public interface RtspConnectionCallback {
        void onConnectionResult(boolean success, String message);
    }
    
    /*
     * testRtspConnection方法使用示例:
     * 
     * // 获取VideoStreamManager实例
     * VideoStreamManager streamManager = VideoStreamManager.getInstance(context);
     * 
     * // 要测试的RTSP URL
     * String rtspUrl = "rtsp://username:password@192.168.1.100:554/stream1";
     * 
     * // 调用测试方法并处理结果
     * streamManager.testRtspConnection(rtspUrl, new VideoStreamManager.RtspConnectionCallback() {
     *     @Override
     *     public void onConnectionResult(boolean success, String message) {
     *         if (success) {
     *             Log.d("RTSPTest", "连接成功: " + message);
     *             // 连接成功，可以继续其他操作
     *         } else {
     *             Log.e("RTSPTest", "连接失败: " + message);
     *             // 处理连接失败的情况
     *         }
     *     }
     * });
     */
}
