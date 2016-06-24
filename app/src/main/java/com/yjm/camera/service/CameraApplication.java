package com.yjm.camera.service;

import android.app.Application;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

/**
 * Created by Liang on 2016/5/30.
 */
public class CameraApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }
    }
}
