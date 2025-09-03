package com.example.nvr.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.nvr.R;

import java.util.Arrays;

public class CameraTestActivity extends AppCompatActivity {
    private static final String TAG = "CameraTestActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_test);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        Button testCameraButton = findViewById(R.id.test_camera_button);
        testCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndOpenCamera();
            }
        });
    }

    private void checkAndOpenCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        openCamera();
    }

    private void openCamera() {
        try {
            // 获取第一个可用摄像头ID
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length > 0) {
                String cameraId = cameraIds[0];
                Log.d(TAG, "尝试打开摄像头: " + cameraId);
                
                // 这里使用CameraDevice.StateCallback可能会产生CameraDevice$1匿名类
                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        Log.d(TAG, "摄像头已成功打开");
                        cameraDevice = camera;
                        // 尝试创建捕获会话，这可能会触发更多与CameraDevice相关的操作
                        createCaptureSession();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        Log.d(TAG, "摄像头连接已断开");
                        camera.close();
                        cameraDevice = null;
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        Log.e(TAG, "摄像头打开错误: " + error);
                        camera.close();
                        cameraDevice = null;
                    }
                }, null);
            } else {
                Toast.makeText(this, "没有找到可用的摄像头", Toast.LENGTH_SHORT).show();
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createCaptureSession() {
        if (cameraDevice == null) {
            Log.e(TAG, "createCaptureSession: cameraDevice为null");
            return;
        }

        try {
            // 创建一个空的Surface列表（仅用于测试）
            Surface[] surfaces = {};
            cameraDevice.createCaptureSession(Arrays.asList(surfaces), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "捕获会话已配置");
                    // 释放资源
                    session.close();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "捕获会话配置失败");
                    session.close();
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "创建捕获会话失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "需要摄像头权限来测试", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}