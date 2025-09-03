package com.example.nvr.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.nvr.R;
import com.example.nvr.model.RecordingFile;

import java.util.Date;
import java.util.List;

public class RecordingListAdapter extends BaseAdapter {
    private Context context;
    private List<RecordingFile> recordingFiles;
    private LayoutInflater inflater;

    public RecordingListAdapter(Context context, List<RecordingFile> recordingFiles) {
        this.context = context;
        this.recordingFiles = recordingFiles;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return recordingFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return recordingFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_recording_file, parent, false);
            holder = new ViewHolder();
            holder.cameraNameTextView = convertView.findViewById(R.id.camera_name_text_view);
            holder.startTimeTextView = convertView.findViewById(R.id.start_time_text_view);
            holder.durationTextView = convertView.findViewById(R.id.duration_text_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RecordingFile recordingFile = recordingFiles.get(position);
        holder.cameraNameTextView.setText(recordingFile.getFileName() != null ? recordingFile.getFileName() : "Unknown");
        holder.startTimeTextView.setText(formatDateTime(recordingFile.getStartTime()));
        holder.durationTextView.setText(recordingFile.getReadableDuration() != null ? recordingFile.getReadableDuration() : "N/A");

        return convertView;
    }

    private static class ViewHolder {
        TextView cameraNameTextView;
        TextView startTimeTextView;
        TextView durationTextView;
    }

    // 格式化日期时间
    private String formatDateTime(Date date) {
        if (date == null) return "N/A";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    // 更新数据列表
    public void setData(List<RecordingFile> recordingFiles) {
        this.recordingFiles = recordingFiles;
        notifyDataSetChanged();
    }
}