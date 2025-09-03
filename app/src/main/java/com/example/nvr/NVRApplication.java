package com.example.nvr;

import android.app.Application;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class NVRApplication extends Application {
    private static final String TAG = "NVRApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 设置全局异常处理器来捕获未处理的异常
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "未捕获的异常: " + throwable.getMessage());
                
                // 输出完整的堆栈跟踪
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                String stackTrace = sw.toString();
                
                Log.e(TAG, "堆栈跟踪:\n" + stackTrace);
                
                // 特别检查CameraDevice相关的空指针异常
                if (throwable instanceof NullPointerException && stackTrace != null && stackTrace.contains("CameraDevice$1")) {
                    Log.e(TAG, "检测到CameraDevice$1空指针异常，可能与Camera2 API相关");
                    
                    // 分析异常来源
                    for (StackTraceElement element : throwable.getStackTrace()) {
                        if (element != null && element.getClassName() != null && element.getClassName().contains("CameraDevice$1")) {
                            Log.e(TAG, "异常发生在: " + element.getClassName() + ":" + element.getMethodName() + " 行: " + element.getLineNumber());
                        }
                    }
                }
                
                // 让系统默认处理器处理崩溃
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, throwable);
            }
        });
    }
}