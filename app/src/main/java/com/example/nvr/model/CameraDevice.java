package com.example.nvr.model;

import android.os.Parcel;
import android.os.Parcelable;

public class CameraDevice implements Parcelable {
    private String id;
    private String name;
    private String ipAddress;
    private int port;
    private String username;
    private String password;
    private String rtspUrl;
    private boolean isRecording;
    private boolean isConnected;

    public CameraDevice(String id, String name, String ipAddress, int port, 
                       String username, String password, String rtspUrl) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
        this.password = password;
        this.rtspUrl = rtspUrl;
        this.isRecording = false;
        this.isConnected = false;
    }

    protected CameraDevice(Parcel in) {
        id = in.readString();
        name = in.readString();
        ipAddress = in.readString();
        port = in.readInt();
        username = in.readString();
        password = in.readString();
        rtspUrl = in.readString();
        isRecording = in.readByte() != 0;
        isConnected = in.readByte() != 0;
    }

    public static final Creator<CameraDevice> CREATOR = new Creator<CameraDevice>() {
        @Override
        public CameraDevice createFromParcel(Parcel in) {
            return new CameraDevice(in);
        }

        @Override
        public CameraDevice[] newArray(int size) {
            return new CameraDevice[size];
        }
    };

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRtspUrl() { return rtspUrl; }
    public void setRtspUrl(String rtspUrl) { this.rtspUrl = rtspUrl; }

    public boolean isRecording() { return isRecording; }
    public void setRecording(boolean recording) { isRecording = recording; }

    public boolean isConnected() { return isConnected; }
    public void setConnected(boolean connected) { isConnected = connected; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(ipAddress);
        dest.writeInt(port);
        dest.writeString(username);
        dest.writeString(password);
        dest.writeString(rtspUrl);
        dest.writeByte((byte) (isRecording ? 1 : 0));
        dest.writeByte((byte) (isConnected ? 1 : 0));
    }
}
