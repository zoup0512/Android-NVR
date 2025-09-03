package com.example.nvr.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.nvr.R;

public class SettingsFragment extends Fragment {

    private EditText storagePathEditText;
    private EditText recordingQualityEditText;
    private EditText recordingDurationEditText;
    private Button saveButton;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        storagePathEditText = view.findViewById(R.id.storage_path);
        recordingQualityEditText = view.findViewById(R.id.recording_quality);
        recordingDurationEditText = view.findViewById(R.id.recording_duration);
        saveButton = view.findViewById(R.id.save_button);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        // 加载保存的设置
        loadSettings();

        // 保存按钮点击事件
        saveButton.setOnClickListener(v -> saveSettings());

        // 清理存储空间按钮点击事件
        Button clearStorageButton = view.findViewById(R.id.clear_storage_button);
        clearStorageButton.setOnClickListener(v -> showClearStorageDialog());

        // 关于应用按钮点击事件
        Button aboutButton = view.findViewById(R.id.about_button);
        aboutButton.setOnClickListener(v -> showAboutDialog());

        return view;
    }

    private void loadSettings() {
        String storagePath = sharedPreferences.getString("storage_path", "/sdcard/NVR");
        String recordingQuality = sharedPreferences.getString("recording_quality", "720p");
        String recordingDuration = sharedPreferences.getString("recording_duration", "30");

        storagePathEditText.setText(storagePath);
        recordingQualityEditText.setText(recordingQuality);
        recordingDurationEditText.setText(recordingDuration);
    }

    private void saveSettings() {
        String storagePath = storagePathEditText.getText().toString().trim();
        String recordingQuality = recordingQualityEditText.getText().toString().trim();
        String recordingDuration = recordingDurationEditText.getText().toString().trim();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("storage_path", storagePath);
        editor.putString("recording_quality", recordingQuality);
        editor.putString("recording_duration", recordingDuration);
        editor.apply();

        Toast.makeText(getContext(), "设置已保存", Toast.LENGTH_SHORT).show();
    }

    private void showClearStorageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("清理存储空间");
        builder.setMessage("确定要删除所有录制文件吗？此操作不可恢复。");
        builder.setPositiveButton("确定", (dialog, which) -> {
            // 实现清理存储空间的逻辑
            Toast.makeText(getContext(), "存储空间清理完成", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("关于应用");
        builder.setMessage("NVR 应用程序\n版本 1.0\n\n用于监控摄像头和录制视频");
        builder.setPositiveButton("确定", null);
        builder.show();
    }
}