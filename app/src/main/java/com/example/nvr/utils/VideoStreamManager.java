package com.example.nvr.utils;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import com.example.nvr.model.CameraDevice;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

public class VideoStreamManager {
    private static final String TAG = "VideoStreamManager";
    private static VideoStreamManager instance;
    private final Context context;
    private final LibVLC libVLC;
    private final ArrayList<MediaPlayer> mediaPlayers = new ArrayList<>();
    private MediaMuxer mediaMuxer;
    private int videoTrackIndex = -1;
    private boolean isMuxerStarted = false;

    private VideoStreamManager(Context context) {
        this.context = context.getApplicationContext();
        ArrayList<String> options = new ArrayList<>();
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");
        options.add("--rtsp-tcp");
        options.add("--network-caching=1500");
        options.add("--clock-jitter=0");
//        options.add("--clock-synchronization=0");
//        try {
//            libVLC = new LibVLC(context, options);
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to create LibVLC instance", e);
//            throw new RuntimeException("Unable to initialize VLC library", e);
//        }
        libVLC = new LibVLC(context, options);
    }

    public static synchronized VideoStreamManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoStreamManager(context);
        }
        return instance;
    }

    // 初始化并开始播放视频流
    public MediaPlayer startStream(CameraDevice camera, VLCVideoLayout videoLayout) {
        MediaPlayer mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(videoLayout, null, false, false);

        Media media = new Media(libVLC, camera.getRtspUrl());
        Log.d(TAG, "RTSP URL: " + camera.getRtspUrl());
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=1500");
        mediaPlayer.setMedia(media);
        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                switch (event.type) {
                    case MediaPlayer.Event.EncounteredError:
                        Log.e(TAG, "Error encountered while playing stream");
                        break;
                    case MediaPlayer.Event.EndReached:
                        Log.d(TAG, "End of stream reached");
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
}
