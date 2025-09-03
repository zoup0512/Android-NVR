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
        recordBtn.setOnClickListener(v -> toggleRecording());
        
        return view;
    }
    
    private void loadFirstCamera() {
        List<CameraDevice> cameras = dbHelper.getAllCameras();
        if (!cameras.isEmpty()) {
            currentCamera = cameras.get(0);
            startCameraStream(currentCamera);
        } else {
            Toast.makeText(getContext(), "请先添加摄像头设备", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void switchToNextCamera() {
        if (currentCamera == null) return;
        
        // 停止当前流
        if (mediaPlayer != null) {
            streamManager.stopStream(mediaPlayer, currentCamera);
        }
        
        // 获取下一个摄像头
        List<CameraDevice> cameras = dbHelper.getAllCameras();
        int currentIndex = cameras.indexOf(currentCamera);
        int nextIndex = (currentIndex + 1) % cameras.size();
        currentCamera = cameras.get(nextIndex);
        
        // 启动新的流
        startCameraStream(currentCamera);
    }
    
    private void startCameraStream(CameraDevice camera) {
        if (camera == null || getContext() == null) return;
        
        mediaPlayer = streamManager.startStream(camera, videoLayout);
        Toast.makeText(getContext(), "正在连接到 " + camera.getName(), Toast.LENGTH_SHORT).show();
    }
    
    private void toggleRecording() {
        if (currentCamera == null) {
            Toast.makeText(getContext(), "没有连接的摄像头", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isRecording) {
            // 停止录制
            streamManager.stopRecording(currentCamera);
            isRecording = false;
            Toast.makeText(getContext(), "录制已停止", Toast.LENGTH_SHORT).show();
        } else {
            // 开始录制
            String fileName = "REC_" + System.currentTimeMillis() + ".mp4";
            String filePath = storageManager.getRecordingDirectoryPath() + File.separator + fileName;
            boolean started = streamManager.startRecording(currentCamera, filePath);
            
            if (started) {
                isRecording = true;
                Toast.makeText(getContext(), "开始录制: " + fileName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "录制启动失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null && currentCamera != null) {
            streamManager.stopStream(mediaPlayer, currentCamera);
        }
    }
}
