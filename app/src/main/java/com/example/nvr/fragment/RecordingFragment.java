package com.example.nvr.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nvr.R;
import com.example.nvr.adapter.RecordingListAdapter;
import com.example.nvr.model.RecordingFile;
import com.example.nvr.utils.StorageManager;

import java.util.ArrayList;
import java.util.List;

public class RecordingFragment extends Fragment {

    private ListView recordingListView;
    private RecordingListAdapter adapter;
    private List<RecordingFile> recordingFiles;
    private StorageManager storageManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recording, container, false);

        recordingListView = view.findViewById(R.id.recording_list);
        storageManager = new StorageManager(getContext());
        recordingFiles = new ArrayList<>();
        adapter = new RecordingListAdapter(getContext(), recordingFiles);
        recordingListView.setAdapter(adapter);

        loadRecordings();

        // 设置列表项点击事件
        recordingListView.setOnItemClickListener((parent, view1, position, id) -> {
            RecordingFile file = recordingFiles.get(position);
            Toast.makeText(getContext(), "播放录制文件: " + file.getFileName(), Toast.LENGTH_SHORT).show();
            // 实际播放逻辑将在这里实现
        });

        return view;
    }

    private void loadRecordings() {
        if (getContext() == null) return;

        try {
            List<RecordingFile> files = storageManager.getAllRecordings();
            recordingFiles.clear();
            recordingFiles.addAll(files);
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "加载录制文件失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecordings(); // 重新加载录制文件，确保列表是最新的
    }
}