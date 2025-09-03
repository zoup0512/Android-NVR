package com.example.nvr.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.Date;

public class RecordingFile implements Parcelable {
    private String id;
    private String fileName;
    private String filePath;
    private long fileSize;
    private Date startTime;
    private Date endTime;
    private String cameraId;
    private String cameraName;

    public RecordingFile(String id, String fileName, String filePath, long fileSize, 
                        Date startTime, Date endTime, String cameraId, String cameraName) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.startTime = startTime;
        this.endTime = endTime;
        this.cameraId = cameraId;
        this.cameraName = cameraName;
    }

    protected RecordingFile(Parcel in) {
        id = in.readString();
        fileName = in.readString();
        filePath = in.readString();
        fileSize = in.readLong();
        long startTimeMillis = in.readLong();
        long endTimeMillis = in.readLong();
        startTime = startTimeMillis > 0 ? new Date(startTimeMillis) : null;
        endTime = endTimeMillis > 0 ? new Date(endTimeMillis) : null;
        cameraId = in.readString();
        cameraName = in.readString();
    }

    public static final Creator<RecordingFile> CREATOR = new Creator<RecordingFile>() {
        @Override
        public RecordingFile createFromParcel(Parcel in) {
            return new RecordingFile(in);
        }

        @Override
        public RecordingFile[] newArray(int size) {
            return new RecordingFile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(fileName);
        dest.writeString(filePath);
        dest.writeLong(fileSize);
        dest.writeLong(startTime != null ? startTime.getTime() : 0);
        dest.writeLong(endTime != null ? endTime.getTime() : 0);
        dest.writeString(cameraId);
        dest.writeString(cameraName);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    // 获取文件的可读大小
    public String getReadableFileSize() {
        if (fileSize <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1024));
        return String.format("%.2f %s", fileSize / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // 获取录制时长（秒）
    public long getDurationSeconds() {
        if (startTime == null || endTime == null) return 0;
        return (endTime.getTime() - startTime.getTime()) / 1000;
    }

    // 获取录制时长的可读字符串
    public String getReadableDuration() {
        long durationSeconds = getDurationSeconds();
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    // 删除文件
    public boolean deleteFile() {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }
}