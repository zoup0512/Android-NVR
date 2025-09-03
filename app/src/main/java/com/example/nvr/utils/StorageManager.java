package com.example.nvr.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.nvr.model.RecordingFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StorageManager {

    private static final String TAG = "StorageManager";
    private static final String DEFAULT_STORAGE_PATH = Environment.getExternalStorageDirectory() + File.separator + "NVR";
    private static final String RECORDINGS_DIR = "recordings";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private String baseStoragePath;

    public StorageManager(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.baseStoragePath = getStoragePathFromPreferences();
        ensureDirectoriesExists();
    }

    private String getStoragePathFromPreferences() {
        return sharedPreferences.getString("storage_path", DEFAULT_STORAGE_PATH);
    }

    private void ensureDirectoriesExists() {
        File baseDir = new File(baseStoragePath);
        if (!baseDir.exists()) {
            boolean created = baseDir.mkdirs();
            Log.d(TAG, "Base directory created: " + created);
        }

        File recordingsDir = new File(baseStoragePath, RECORDINGS_DIR);
        if (!recordingsDir.exists()) {
            boolean created = recordingsDir.mkdirs();
            Log.d(TAG, "Recordings directory created: " + created);
        }
    }

    public String getRecordingDirectoryPath() {
        return new File(baseStoragePath, RECORDINGS_DIR).getAbsolutePath();
    }

    public String createNewRecordingFilePath(String cameraId, String cameraName) {
        ensureDirectoriesExists();
        String timestamp = DATE_FORMAT.format(new Date());
        String filename = String.format("recording_%s_%s.mp4", cameraId, timestamp);
        return new File(baseStoragePath, RECORDINGS_DIR + File.separator + filename).getAbsolutePath();
    }

    public List<RecordingFile> getAllRecordings() {
        List<RecordingFile> recordings = new ArrayList<>();
        File recordingsDir = new File(baseStoragePath, RECORDINGS_DIR);

        if (!recordingsDir.exists() || !recordingsDir.isDirectory()) {
            Log.w(TAG, "Recordings directory does not exist");
            return recordings;
        }

        File[] files = recordingsDir.listFiles();
        if (files == null || files.length == 0) {
            return recordings;
        }

        // 按文件创建时间排序（最新的在前）
        Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".mp4")) {
                try {
                    RecordingFile recordingFile = createRecordingFileFromFile(file);
                    recordings.add(recordingFile);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create RecordingFile from " + file.getName(), e);
                }
            }
        }

        return recordings;
    }

    private RecordingFile createRecordingFileFromFile(File file) {
        String id = UUID.randomUUID().toString();
        String fileName = file.getName();
        String filePath = file.getAbsolutePath();
        long fileSize = file.length();
        Date lastModified = new Date(file.lastModified());
        
        // 从文件名解析摄像头ID（这只是一个示例实现）
        String cameraId = "unknown";
        String cameraName = "未知摄像头";
        
        // 假设文件名格式为: recording_cameraId_timestamp.mp4
        if (fileName.startsWith("recording_")) {
            String[] parts = fileName.split("_");
            if (parts.length >= 3) {
                cameraId = parts[1];
                // 这里可以根据cameraId从数据库中获取摄像头名称
            }
        }

        return new RecordingFile(
                id,
                fileName,
                filePath,
                fileSize,
                lastModified,
                lastModified,
                cameraId,
                cameraName
        );
    }

    public boolean deleteRecording(RecordingFile recordingFile) {
        return recordingFile.deleteFile();
    }

    public boolean deleteAllRecordings() {
        List<RecordingFile> recordings = getAllRecordings();
        boolean allDeleted = true;

        for (RecordingFile recording : recordings) {
            if (!deleteRecording(recording)) {
                allDeleted = false;
            }
        }

        return allDeleted;
    }

    public long getTotalStorageUsed() {
        File recordingsDir = new File(baseStoragePath, RECORDINGS_DIR);
        return getDirectorySize(recordingsDir);
    }

    public long getDirectorySize(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getDirectorySize(file);
                }
            }
        }

        return size;
    }

    public String getReadableStorageUsed() {
        long size = getTotalStorageUsed();
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public boolean isStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public long getAvailableStorageSpace() {
        if (!isStorageAvailable()) {
            return 0;
        }

        File storageDir = Environment.getExternalStorageDirectory();
        return storageDir.getFreeSpace();
    }

    public String getReadableAvailableStorageSpace() {
        long space = getAvailableStorageSpace();
        if (space <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(space) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.2f %s", space / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public void setBaseStoragePath(String path) {
        this.baseStoragePath = path;
        ensureDirectoriesExists();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("storage_path", path);
        editor.apply();
    }

    public String getBaseStoragePath() {
        return baseStoragePath;
    }
}