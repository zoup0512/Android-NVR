package com.example.nvr.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.nvr.R;
import com.example.nvr.adapter.CameraDeviceAdapter;
import com.example.nvr.model.CameraDevice;
import com.example.nvr.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class DeviceManagerFragment extends Fragment {

    private ListView deviceListView;
    private CameraDeviceAdapter adapter;
    private List<CameraDevice> cameraDevices;
    private DatabaseHelper dbHelper;
    private Button addDeviceButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_manager, container, false);

        deviceListView = view.findViewById(R.id.device_list);
        addDeviceButton = view.findViewById(R.id.add_device_button);
        dbHelper = new DatabaseHelper(getContext());
        cameraDevices = new ArrayList<>();
        adapter = new CameraDeviceAdapter(getContext(), cameraDevices);
        deviceListView.setAdapter(adapter);

        loadDevices();

        // 添加设备按钮点击事件
        addDeviceButton.setOnClickListener(v -> showAddDeviceDialog());

        // 设置列表项长按事件，用于编辑或删除设备
        deviceListView.setOnItemLongClickListener((parent, view1, position, id) -> {
            CameraDevice device = cameraDevices.get(position);
            showDeviceOptionsDialog(device, position);
            return true;
        });

        return view;
    }

    private void loadDevices() {
        if (getContext() == null || dbHelper == null) return;

        List<CameraDevice> devices = dbHelper.getAllCameras();
        if (cameraDevices != null) {
            cameraDevices.clear();
            if (devices != null) {
                cameraDevices.addAll(devices);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showAddDeviceDialog() {
        if (getContext() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("添加摄像头设备");

        // 创建对话框布局
        LayoutInflater inflater = getLayoutInflater();
        if (inflater == null) return;
        
        View dialogView = inflater.inflate(R.layout.dialog_add_device, null);
        builder.setView(dialogView);

        if (dialogView == null) return;
        
        EditText nameEditText = dialogView.findViewById(R.id.device_name);
        EditText rtspUrlEditText = dialogView.findViewById(R.id.device_rtsp_url);

        // 设置确定按钮
        builder.setPositiveButton("添加", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (nameEditText == null || rtspUrlEditText == null) {
                    return;
                }
                
                String name = nameEditText.getText() != null ? nameEditText.getText().toString().trim() : "";
                String rtspUrl = rtspUrlEditText.getText() != null ? rtspUrlEditText.getText().toString().trim() : "";

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(rtspUrl)) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "名称和RTSP URL不能为空", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                // 创建新设备
                // 生成唯一ID (这里使用时间戳+随机数)
                String id = String.valueOf(System.currentTimeMillis()) + (int)(Math.random() * 1000);
                
                // 创建新设备
                CameraDevice newDevice = new CameraDevice(id, name, rtspUrl);
                boolean result = dbHelper.addCamera(newDevice);

                if (result) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "设备添加成功", Toast.LENGTH_SHORT).show();
                    }
                    loadDevices(); // 重新加载设备列表
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "设备添加失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", null);

        // 显示对话框
        builder.show();
    }

    private void showDeviceOptionsDialog(final CameraDevice device, final int position) {
        if (getContext() == null || device == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("设备操作");
        builder.setItems(new CharSequence[]{"编辑设备", "删除设备"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // 编辑设备
                        showEditDeviceDialog(device);
                        break;
                    case 1: // 删除设备
                        showDeleteDeviceDialog(device.getId(), position);
                        break;
                }
            }
        });
        builder.show();
    }

    private void showEditDeviceDialog(final CameraDevice device) {
        if (getContext() == null || device == null) return;
        
        // 实现编辑设备的逻辑，类似于添加设备对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("编辑摄像头设备");

        LayoutInflater inflater = getLayoutInflater();
        if (inflater == null) return;
        
        View dialogView = inflater.inflate(R.layout.dialog_add_device, null);
        builder.setView(dialogView);
        
        if (dialogView == null) return;

        EditText nameEditText = dialogView.findViewById(R.id.device_name);
        EditText rtspUrlEditText = dialogView.findViewById(R.id.device_rtsp_url);

        // 填充现有设备信息
        if (nameEditText != null) nameEditText.setText(device.getName());
        if (rtspUrlEditText != null) rtspUrlEditText.setText(device.getRtspUrl());

        builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 实现保存编辑后的设备信息的逻辑
                if (nameEditText == null || rtspUrlEditText == null) {
                    return;
                }
                
                String name = nameEditText.getText() != null ? nameEditText.getText().toString().trim() : "";
                String rtspUrl = rtspUrlEditText.getText() != null ? rtspUrlEditText.getText().toString().trim() : "";

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(rtspUrl)) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "名称和RTSP URL不能为空", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                // 创建CameraDevice对象
                CameraDevice updatedDevice = new CameraDevice(device.getId(), name, rtspUrl);
                int rowsUpdated = dbHelper.updateCamera(updatedDevice);

                if (rowsUpdated > 0) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "设备更新成功", Toast.LENGTH_SHORT).show();
                    }
                    loadDevices();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "设备更新失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showDeleteDeviceDialog(final String deviceId, final int position) {
        if (getContext() == null || deviceId == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("确认删除");
        builder.setMessage("确定要删除这个设备吗？");
        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean deleted = dbHelper.deleteCamera(deviceId);
                if (deleted) {
                    if (cameraDevices != null && position >= 0 && position < cameraDevices.size()) {
                        cameraDevices.remove(position);
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "设备删除成功", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "设备删除失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}