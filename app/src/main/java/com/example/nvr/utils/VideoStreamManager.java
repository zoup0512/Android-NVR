package com.example.nvr.utils;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.compose.runtime.State;

import com.example.nvr.model.CameraDevice;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class VideoStreamManager {
    private static final String TAG = "VideoStreamManager";
    private static VideoStreamManager instance;
    private final Context context;
    private  LibVLC libVLC;
    private final ArrayList<MediaPlayer> mediaPlayers = new ArrayList<>();
    private MediaMuxer mediaMuxer;
    private int videoTrackIndex = -1;
    private boolean isMuxerStarted = false;

    private VideoStreamManager(Context context) {
        this.context = context.getApplicationContext();
        try {
            // 使用简化的配置以提高兼容性和稳定性
            ArrayList<String> options = new ArrayList<>();
            options.add("--no-drop-late-frames");
            options.add("--no-skip-frames");
            options.add("--rtsp-tcp");
            options.add("--network-caching=500");
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
            Log.d(TAG, "LibVLC instance created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create LibVLC instance: " + e.getMessage());
        }
    }

    public static synchronized VideoStreamManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoStreamManager(context);
        }
        return instance;
    }

    // 初始化并开始播放视频流
    public MediaPlayer startStream(CameraDevice camera, VLCVideoLayout videoLayout) {
        if (camera == null || camera.getRtspUrl() == null || camera.getRtspUrl().isEmpty()) {
            Log.e(TAG, "Camera or RTSP URL is null or empty");
            return null;
        }

        Log.d(TAG, "准备开始流播放 - 摄像头ID: " + camera.getId() + ", URL: " + camera.getRtspUrl());

        // 验证libVLC是否初始化成功
        if (libVLC == null) {
            Log.e(TAG, "LibVLC is not initialized, cannot start stream");
            return null;
        }

        MediaPlayer mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(videoLayout, null, false, false);

        // 修复：使用实际的RTSP URL而不是字符串字面量
        Media media = new Media(libVLC, camera.getRtspUrl());
        Log.d(TAG, "RTSP URL: " + camera.getRtspUrl());
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=500");
        media.addOption(":rtsp-tcp");
        media.addOption(":verbose=3");
        media.addOption(":clock-synchronization=0");
        media.addOption(":live-caching=500");
        // 添加更多参数以提高兼容性
        media.addOption(":rtsp-frame-buffer-size=1000");
        media.addOption(":udp-timeout=15000");
        media.addOption(":tcp-timeout=15000");
        media.addOption(":timeout=15000");
        media.addOption(":rtsp-user-agent=VLC Android NVR Client");
        // 禁用硬件加速作为后备选项
        media.addOption(":no-avcodec-hw");
        
        // 添加自动重试参数
        media.addOption(":http-reconnect=1");
        media.addOption(":reconnect=1");
        media.addOption(":reconnect-delay=2000");
        
        mediaPlayer.setMedia(media);
        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            private int retryCount = 0;
            private static final int MAX_RETRIES = 3;
            
            @Override
            public void onEvent(MediaPlayer.Event event) {
                switch (event.type) {
                    case MediaPlayer.Event.EncounteredError:
                        Log.e(TAG, "Error encountered while playing stream");
                        Log.e(TAG, "Error details - URL: " + camera.getRtspUrl());
                        // 检查媒体播放器的状态
                        if (mediaPlayer != null) {
                            Log.e(TAG, "MediaPlayer state: " + getMediaPlayerState(mediaPlayer));
                        }
                        // 标记为未连接
                        camera.setConnected(false);
                        
                        // 添加自动重试机制
                        if (retryCount < MAX_RETRIES) {
                            retryCount++;
                            Log.d(TAG, "尝试重新连接 (" + retryCount + "/" + MAX_RETRIES + ")...");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Media newMedia = new Media(libVLC, camera.getRtspUrl());
                                        // 复制原始媒体的所有选项
                                        newMedia.setHWDecoderEnabled(true, false);
                                        newMedia.addOption(":network-caching=500");
                                        newMedia.addOption(":rtsp-tcp");
                                        newMedia.addOption(":verbose=3");
                                        newMedia.addOption(":clock-synchronization=0");
                                        newMedia.addOption(":live-caching=500");
                                        newMedia.addOption(":rtsp-frame-buffer-size=1000");
                                        newMedia.addOption(":udp-timeout=15000");
                                        newMedia.addOption(":tcp-timeout=15000");
                                        newMedia.addOption(":timeout=15000");
                                        newMedia.addOption(":http-reconnect=1");
                                        newMedia.addOption(":reconnect=1");
                                        newMedia.addOption(":reconnect-delay=2000");
                                        newMedia.addOption(":no-avcodec-hw");
                                        
                                        mediaPlayer.setMedia(newMedia);
                                        newMedia.release();
                                        mediaPlayer.play();
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
                        if (camera.isConnected() && retryCount < MAX_RETRIES) {
                            retryCount++;
                            Log.d(TAG, "流意外结束，尝试重新连接 (" + retryCount + "/" + MAX_RETRIES + ")...");
                            try {
                                Media newMedia = new Media(libVLC, camera.getRtspUrl());
                                mediaPlayer.setMedia(newMedia);
                                newMedia.release();
                                mediaPlayer.play();
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
            }
        });
        media.release();

        mediaPlayer.play();
        mediaPlayers.add(mediaPlayer);
        camera.setConnected(true);
        
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
            mediaPlayer.stop();
            mediaPlayer.detachViews();
            mediaPlayers.remove(mediaPlayer);
            mediaPlayer.release();
            camera.setConnected(false);
        }
    }

    // 开始录制视频
    public boolean startRecording(CameraDevice camera, String outputPath) {
        try {
            // 创建输出文件
            File outputFile = new File(outputPath);
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            
            // 初始化MediaMuxer
            mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            camera.setRecording(true);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording: " + e.getMessage());
            return false;
        }
    }

    // 停止录制视频
    public void stopRecording(CameraDevice camera) {
        if (mediaMuxer != null) {
            if (isMuxerStarted) {
                mediaMuxer.stop();
            }
            mediaMuxer.release();
            mediaMuxer = null;
            videoTrackIndex = -1;
            isMuxerStarted = false;
        }
        camera.setRecording(false);
    }

    // 处理视频数据并写入文件
    public void handleVideoData(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo, MediaFormat mediaFormat) {
        if (mediaMuxer == null) {
            return;
        }

        if (!isMuxerStarted) {
            videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
            mediaMuxer.start();
            isMuxerStarted = true;
        }

        if (byteBuffer != null) {
            mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo);
        }
    }

    // 释放所有资源
    public void release() {
        for (MediaPlayer mp : mediaPlayers) {
            mp.stop();
            mp.release();
        }
        mediaPlayers.clear();
        libVLC.release();
        instance = null;
    }
    
    // 测试RTSP连接，用于诊断连接问题
    public void testRtspConnection(String rtspUrl, RtspConnectionCallback callback) {
        if (rtspUrl == null || rtspUrl.isEmpty()) {
            if (callback != null) {
                callback.onConnectionResult(false, "RTSP URL为空");
            }
            return;
        }

        Log.d(TAG, "开始测试RTSP连接: " + rtspUrl);
        
        // 验证libVLC是否初始化成功
        if (libVLC == null) {
            Log.e(TAG, "LibVLC is not initialized, cannot test connection");
            if (callback != null) {
                callback.onConnectionResult(false, "LibVLC未初始化");
            }
            return;
        }
        
        // 创建一个临时的MediaPlayer用于测试连接
        MediaPlayer testPlayer = new MediaPlayer(libVLC);
        Media media = new Media(libVLC, rtspUrl);
        
        // 使用与startStream相同的参数配置，确保测试环境一致
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=500");
        media.addOption(":rtsp-tcp");
        media.addOption(":verbose=3");
        media.addOption(":timeout=8000");
        media.addOption(":no-avcodec-hw");
        
        testPlayer.setMedia(media);
        media.release();
        
        // 设置一个连接超时计时器
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (testPlayer != null) {
                    try {
                        // 使用getPlayerState方法而不是直接访问State枚举
                        int state = testPlayer.getPlayerState();
                        // 3代表Playing状态（从之前的映射得知）
                        if (state != 3) {
                            Log.e(TAG, "RTSP连接超时: " + rtspUrl + ", 当前状态: " + getMediaPlayerState(testPlayer));
                            if (callback != null) {
                                callback.onConnectionResult(false, "连接超时(8秒)，请检查网络和URL是否正确");
                            }
                            cleanupTestPlayer(testPlayer);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking player state: " + e.getMessage());
                        if (callback != null) {
                            callback.onConnectionResult(false, "无法获取播放器状态: " + e.getMessage());
                        }
                        cleanupTestPlayer(testPlayer);
                    }
                }
            }
        };
        
        // 设置超时检查
        handler.postDelayed(timeoutRunnable, 8000); // 8秒超时
        
        // 设置事件监听器
        testPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                try {
                    switch (event.type) {
                        case MediaPlayer.Event.Playing:
                            Log.d(TAG, "RTSP连接测试成功: " + rtspUrl);
                            handler.removeCallbacks(timeoutRunnable);
                            if (callback != null) {
                                callback.onConnectionResult(true, "连接成功");
                            }
                            cleanupTestPlayer(testPlayer);
                            break;
                        
                        case MediaPlayer.Event.EncounteredError:
                            Log.e(TAG, "RTSP连接测试失败: " + rtspUrl);
                            handler.removeCallbacks(timeoutRunnable);
                            if (callback != null) {
                                String errorMsg = "连接失败，可能的原因: URL错误、网络问题、认证失败或摄像头不可用";
                                callback.onConnectionResult(false, errorMsg);
                            }
                            cleanupTestPlayer(testPlayer);
                            break;
                        
                        case MediaPlayer.Event.EndReached:
                            Log.e(TAG, "RTSP连接测试意外结束: " + rtspUrl);
                            handler.removeCallbacks(timeoutRunnable);
                            if (callback != null) {
                                callback.onConnectionResult(false, "连接意外结束，可能是URL格式错误或摄像头拒绝连接");
                            }
                            cleanupTestPlayer(testPlayer);
                            break;
                        
                        case MediaPlayer.Event.Buffering:
                            Log.d(TAG, "RTSP连接测试中 - 缓冲: " + event.getBuffering() + "%");
                            break;
                        
                        case MediaPlayer.Event.Opening:
                            Log.d(TAG, "RTSP连接测试中 - 正在打开连接...");
                            break;
                        
                        default:
                            Log.d(TAG, "RTSP连接测试 - 事件类型: " + event.type);
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "测试连接时发生异常: " + e.getMessage());
                    handler.removeCallbacks(timeoutRunnable);
                    if (callback != null) {
                        callback.onConnectionResult(false, "测试过程中发生异常: " + e.getMessage());
                    }
                    cleanupTestPlayer(testPlayer);
                }
            }
        });
        
        try {
            testPlayer.play();
            Log.d(TAG, "RTSP连接测试已启动");
        } catch (Exception e) {
            Log.e(TAG, "启动RTSP连接测试失败: " + e.getMessage());
            handler.removeCallbacks(timeoutRunnable);
            if (callback != null) {
                callback.onConnectionResult(false, "无法启动测试: " + e.getMessage());
            }
            cleanupTestPlayer(testPlayer);
        }
    }
    
    // 清理测试用的MediaPlayer
    private void cleanupTestPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
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
