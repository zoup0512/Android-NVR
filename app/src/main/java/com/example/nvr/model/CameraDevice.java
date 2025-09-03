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
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.ipAddress = ipAddress != null ? ipAddress : "";
        this.port = port;
        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
        this.rtspUrl = rtspUrl != null ? rtspUrl : "";
        this.isRecording = false;
        this.isConnected = false;
    }

    protected CameraDevice(Parcel in) {
        id = in.readString();
        if (id == null) id = "";
        
        name = in.readString();
        if (name == null) name = "";
        
        ipAddress = in.readString();
        if (ipAddress == null) ipAddress = "";
        
        port = in.readInt();
        
        username = in.readString();
        if (username == null) username = "";
        
        password = in.readString();
        if (password == null) password = "";
        
        rtspUrl = in.readString();
        if (rtspUrl == null) rtspUrl = "";
        
        isRecording = in.readByte() != 0;
        isConnected = in.readByte() != 0;
    }

    public static final Creator<CameraDevice> CREATOR = new Creator<CameraDevice>() {
        @Override
        public CameraDevice createFromParcel(Parcel in) {
            if (in == null) {
                return null;
            }
            return new CameraDevice(in);
        }

        @Override
        public CameraDevice[] newArray(int size) {
            return new CameraDevice[size];
        }
    };

    // Getters and Setters
    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id != null ? id : ""; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name != null ? name : ""; }

    public String getIpAddress() { return ipAddress != null ? ipAddress : ""; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress != null ? ipAddress : ""; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username != null ? username : ""; }
    public void setUsername(String username) { this.username = username != null ? username : ""; }

    public String getPassword() { return password != null ? password : ""; }
    public void setPassword(String password) { this.password = password != null ? password : ""; }

    public String getRtspUrl() { return rtspUrl != null ? rtspUrl : ""; }
    public void setRtspUrl(String rtspUrl) { this.rtspUrl = rtspUrl != null ? rtspUrl : ""; }

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
        if (dest != null) {
            dest.writeString(id != null ? id : "");
            dest.writeString(name != null ? name : "");
            dest.writeString(ipAddress != null ? ipAddress : "");
            dest.writeInt(port);
            dest.writeString(username != null ? username : "");
            dest.writeString(password != null ? password : "");
            dest.writeString(rtspUrl != null ? rtspUrl : "");
            dest.writeByte((byte) (isRecording ? 1 : 0));
            dest.writeByte((byte) (isConnected ? 1 : 0));
        }
    }
}
