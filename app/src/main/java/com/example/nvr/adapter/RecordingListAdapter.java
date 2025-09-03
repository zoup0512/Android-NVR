package com.example.nvr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.nvr.R;
import com.example.nvr.model.RecordingFile;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecordingListAdapter extends BaseAdapter {
    private Context context;
    private List<RecordingFile> recordingFiles;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public RecordingListAdapter(Context context, List<RecordingFile> recordingFiles) {
        this.context = context;
        this.recordingFiles = recordingFiles;
    }

    @Override
    public int getCount() {
        return recordingFiles != null ? recordingFiles.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return recordingFiles != null ? recordingFiles.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_recording_file, parent, false);
            holder = new ViewHolder();
            holder.cameraNameTextView = convertView.findViewById(R.id.camera_name_text_view);
            holder.startTimeTextView = convertView.findViewById(R.id.start_time_text_view);
            holder.durationTextView = convertView.findViewById(R.id.duration_text_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RecordingFile recordingFile = recordingFiles.get(position);
        holder.cameraNameTextView.setText(recordingFile.getCameraName());
        holder.startTimeTextView.setText(dateFormat.format(recordingFile.getStartTime()));
        holder.durationTextView.setText(formatDuration(recordingFile.getDurationSeconds() * 1000));

        return convertView;
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }

    public void updateData(List<RecordingFile> newRecordingFiles) {
        this.recordingFiles = newRecordingFiles;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView cameraNameTextView;
        TextView startTimeTextView;
        TextView durationTextView;
    }
}