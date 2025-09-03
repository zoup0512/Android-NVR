package com.example.nvr.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.nvr.model.CameraDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "NVRDatabase";
    private static final String TABLE_CAMERAS = "cameras";

    // 摄像头表字段
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_IP = "ip_address";
    private static final String KEY_PORT = "port";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_RTSP_URL = "rtsp_url";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CAMERAS_TABLE = "CREATE TABLE " + TABLE_CAMERAS + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_IP + " TEXT,"
                + KEY_PORT + " INTEGER,"
                + KEY_USERNAME + " TEXT,"
                + KEY_PASSWORD + " TEXT,"
                + KEY_RTSP_URL + " TEXT" + ")";
        db.execSQL(CREATE_CAMERAS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAMERAS);
        onCreate(db);
    }

    // 添加摄像头
    public boolean addCamera(CameraDevice camera) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, camera.getId());
        values.put(KEY_NAME, camera.getName());
        values.put(KEY_IP, camera.getIpAddress());
        values.put(KEY_PORT, camera.getPort());
        values.put(KEY_USERNAME, camera.getUsername());
        values.put(KEY_PASSWORD, camera.getPassword());
        values.put(KEY_RTSP_URL, camera.getRtspUrl());

        long result = db.insert(TABLE_CAMERAS, null, values);
        db.close();
        return result != -1; // 如果插入成功，返回true
    }

    // 获取单个摄像头
    public CameraDevice getCamera(String id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CAMERAS, new String[] { KEY_ID,
                        KEY_NAME, KEY_IP, KEY_PORT, KEY_USERNAME, KEY_PASSWORD, KEY_RTSP_URL }, KEY_ID + "=?",
                new String[] { id }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        CameraDevice camera = new CameraDevice(
                cursor.getString(0) != null ? cursor.getString(0) : "",
                cursor.getString(1) != null ? cursor.getString(1) : "",
                cursor.getString(2) != null ? cursor.getString(2) : "",
                cursor.getInt(3),
                cursor.getString(4) != null ? cursor.getString(4) : "",
                cursor.getString(5) != null ? cursor.getString(5) : "",
                cursor.getString(6) != null ? cursor.getString(6) : ""
        );
        
        cursor.close();
        return camera;
    }

    // 获取所有摄像头
    public List<CameraDevice> getAllCameras() {
        List<CameraDevice> cameraList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_CAMERAS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                CameraDevice camera = new CameraDevice(
                        cursor.getString(0) != null ? cursor.getString(0) : "",
                        cursor.getString(1) != null ? cursor.getString(1) : "",
                        cursor.getString(2) != null ? cursor.getString(2) : "",
                        cursor.getInt(3),
                        cursor.getString(4) != null ? cursor.getString(4) : "",
                        cursor.getString(5) != null ? cursor.getString(5) : "",
                        cursor.getString(6) != null ? cursor.getString(6) : ""
                );
                cameraList.add(camera);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return cameraList;
    }

    // 更新摄像头信息
    public int updateCamera(CameraDevice camera) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, camera.getName());
        values.put(KEY_IP, camera.getIpAddress());
        values.put(KEY_PORT, camera.getPort());
        values.put(KEY_USERNAME, camera.getUsername());
        values.put(KEY_PASSWORD, camera.getPassword());
        values.put(KEY_RTSP_URL, camera.getRtspUrl());

        return db.update(TABLE_CAMERAS, values, KEY_ID + " = ?",
                new String[] { camera.getId() });
    }

    // 删除摄像头
    public void deleteCamera(CameraDevice camera) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CAMERAS, KEY_ID + " = ?",
                new String[] { camera.getId() });
        db.close();
    }

    // 通过ID删除摄像头
    public boolean deleteCamera(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_CAMERAS, KEY_ID + " = ?",
                new String[] { id });
        db.close();
        return rowsAffected > 0;
    }

    // 获取摄像头数量
    public int getCamerasCount() {
        String countQuery = "SELECT  * FROM " + TABLE_CAMERAS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }
}
