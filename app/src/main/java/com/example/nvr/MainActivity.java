package com.example.nvr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.example.nvr.fragment.LiveViewFragment;
import com.example.nvr.fragment.RecordingFragment;
import com.example.nvr.fragment.DeviceManagerFragment;
import com.example.nvr.fragment.SettingsFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewpager);
        if (viewPager != null) {
            setupViewPager(viewPager);
        }

        tabLayout = findViewById(R.id.tabs);
        if (tabLayout != null && viewPager != null) {
            tabLayout.setupWithViewPager(viewPager);
        }
        
        // 设置测试摄像头按钮
        FloatingActionButton testCameraFab = findViewById(R.id.test_camera_fab);
    }

    private void setupViewPager(ViewPager viewPager) {
        if (viewPager == null) return;
        
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        if (adapter != null) {
            adapter.addFragment(new LiveViewFragment(), "实时监控");
            adapter.addFragment(new RecordingFragment(), "录像回放");
            adapter.addFragment(new DeviceManagerFragment(), "设备管理");
            adapter.addFragment(new SettingsFragment(), "设置");
            viewPager.setAdapter(adapter);
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<Fragment>();
        private final List<String> mFragmentTitleList = new ArrayList<String>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            if (mFragmentList != null && position >= 0 && position < mFragmentList.size()) {
                return mFragmentList.get(position);
            }
            return null;
        }

        @Override
        public int getCount() {
            return mFragmentList != null ? mFragmentList.size() : 0;
        }

        public void addFragment(Fragment fragment, String title) {
            if (mFragmentList != null && fragment != null) {
                mFragmentList.add(fragment);
            }
            if (mFragmentTitleList != null && title != null) {
                mFragmentTitleList.add(title);
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (mFragmentTitleList != null && position >= 0 && position < mFragmentTitleList.size()) {
                String title = mFragmentTitleList.get(position);
                return title != null ? title : "";
            }
            return "";
        }
    }
}
