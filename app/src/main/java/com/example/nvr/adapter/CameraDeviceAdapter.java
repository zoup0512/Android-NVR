package com.example.nvr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.nvr.R;
import com.example.nvr.model.CameraDevice;

import java.util.List;

public class CameraDeviceAdapter extends BaseAdapter {
    private Context context;
    private List<CameraDevice> cameraDevices;
    private LayoutInflater inflater;

    public CameraDeviceAdapter(Context context, List<CameraDevice> cameraDevices) {
        this.context = context;
        this.cameraDevices = cameraDevices;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return cameraDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return cameraDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_camera_device, parent, false);
            holder = new ViewHolder();
            holder.deviceNameTextView = convertView.findViewById(R.id.device_name_text_view);
            holder.deviceIpTextView = convertView.findViewById(R.id.device_ip_text_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CameraDevice cameraDevice = cameraDevices.get(position);
        holder.deviceNameTextView.setText(cameraDevice.getName());
        holder.deviceIpTextView.setText(cameraDevice.getIpAddress() + ":" + cameraDevice.getPort());

        return convertView;
    }

    private static class ViewHolder {
        TextView deviceNameTextView;
        TextView deviceIpTextView;
    }

    // 更新数据列表
    public void setData(List<CameraDevice> cameraDevices) {
        this.cameraDevices = cameraDevices;
        notifyDataSetChanged();
    }
}