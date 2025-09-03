package com.example.nvr.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.nvr.MainActivity;
import com.example.nvr.R;
import com.example.nvr.utils.StorageManager;

public class RecordingService extends Service {

    private static final String TAG = "RecordingService";
    private static final String CHANNEL_ID = "NVR_RECORDING_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    private StorageManager storageManager;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        storageManager = new StorageManager(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        startRecording();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "NVR Recording",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("NVR视频录制服务");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NVR应用")
                .setContentText("正在录制视频...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void startRecording() {
        if (!isRecording) {
            isRecording = true;
            Log.d(TAG, "开始录制视频");
            // 在这里实现实际的视频录制逻辑
            // 这部分代码将与Camera API或第三方库集成
        }
    }

    private void stopRecording() {
        if (isRecording) {
            isRecording = false;
            Log.d(TAG, "停止录制视频");
            // 在这里实现停止视频录制的逻辑
        }
    }
}