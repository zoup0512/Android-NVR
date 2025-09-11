package com.example.nvr.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nvr.R;
import com.example.nvr.model.CameraDevice;
import com.example.nvr.utils.DatabaseHelper;
import com.example.nvr.utils.VideoStreamManager;
import com.example.nvr.utils.StorageManager;

import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.List;
import java.io.File;

public class LiveViewFragment extends Fragment {
    private static final String TAG = "LiveViewFragment";
    private VLCVideoLayout videoLayout;
    private MediaPlayer mediaPlayer;
    private VideoStreamManager streamManager;
    private DatabaseHelper dbHelper;
    private StorageManager storageManager;
    private CameraDevice currentCamera;
    private boolean isRecording = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_view, container, false);
        
        videoLayout = view.findViewById(R.id.video_layout);
        Button switchCameraBtn = view.findViewById(R.id.switch_camera_btn);
        Button recordBtn = view.findViewById(R.id.record_btn);
        
        dbHelper = new DatabaseHelper(getContext());
        storageManager = new StorageManager(getContext());
        streamManager = VideoStreamManager.getInstance(getContext());
        
        // 加载第一个摄像头
        loadFirstCamera();
        
        // 切换摄像头按钮点击事件
        switchCameraBtn.setOnClickListener(v -> switchToNextCamera());
        
        // 录制按钮点击事件
        if (recordBtn != null) {
            recordBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleRecording();
                }
            });
        }
        
        return view;
    }
    
    private void loadFirstCamera() {
        if (dbHelper == null) return;
        
        List<CameraDevice> cameras = dbHelper.getAllCameras();
        if (cameras != null && !cameras.isEmpty()) {
            currentCamera = cameras.get(0);
            // Don't start the stream immediately in onCreateView
            // Instead, wait for the view to be fully attached to the window
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "请先添加摄像头设备", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Check if view is attached to window
        if (videoLayout != null && videoLayout.getWindowToken() != null) {
            // If already attached, start the stream immediately
            if (currentCamera != null) {
                startCameraStream(currentCamera);
            }
        } else {
            // If not attached, add a window focus change listener
            videoLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, 
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    // Check if window token is now available
                    if (videoLayout.getWindowToken() != null) {
                        // Remove the listener to avoid multiple calls
                        videoLayout.removeOnLayoutChangeListener(this);
                        // Start the stream
                        if (currentCamera != null) {
                            startCameraStream(currentCamera);
                        }
                    }
                }
            });
        }
    }
    
    private void switchToNextCamera() {
        if (currentCamera == null || dbHelper == null) return;
        
        // 停止当前流
        if (mediaPlayer != null) {
            streamManager.stopStream(mediaPlayer, currentCamera);
        }
        
        // 获取下一个摄像头
        List<CameraDevice> cameras = dbHelper.getAllCameras();
        if (cameras != null && !cameras.isEmpty()) {
            int currentIndex = cameras.indexOf(currentCamera);
            int nextIndex = (currentIndex + 1) % cameras.size();
            currentCamera = cameras.get(nextIndex);
            
            // 启动新的流
            if (videoLayout != null && videoLayout.getWindowToken() != null) {
                startCameraStream(currentCamera);
            }
        }
    }
    
    private void startCameraStream(CameraDevice camera) {
        if (camera == null || getContext() == null || streamManager == null || videoLayout == null) return;
        
        // Additional safety check to ensure the view is attached to a window
        if (videoLayout.getWindowToken() == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "视图尚未完全加载，请稍候重试", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
//        camera.setRtspUrl("rtsp://admin:0512@192.168.110.174:8554/live");
        mediaPlayer = streamManager.startStream(camera, videoLayout);
        if (mediaPlayer != null) {
            Toast.makeText(getContext(), "正在连接到 " + camera.getName(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void toggleRecording() {
        if (currentCamera == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "没有连接的摄像头", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        if (isRecording) {
            // 停止录制
            if (streamManager != null) {
                streamManager.stopRecording(currentCamera);
            }
            isRecording = false;
            if (getContext() != null) {
                Toast.makeText(getContext(), "录制已停止", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 开始录制
            if (storageManager != null) {
                String fileName = "REC_" + System.currentTimeMillis() + ".mp4";
                String filePath = storageManager.getRecordingDirectoryPath() + File.separator + fileName;
                boolean started = streamManager.startRecording(currentCamera, filePath);
                
                if (started) {
                    isRecording = true;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "开始录制: " + fileName, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "录制启动失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null && currentCamera != null && streamManager != null) {
            streamManager.stopStream(mediaPlayer, currentCamera);
        }
    }
}
