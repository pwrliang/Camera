package com.gl.camera.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;

import com.gl.camera.R;
import com.gl.camera.model.MyCamera;
import com.gl.camera.model.MyCameraEx;
import com.gl.camera.util.Preference;

public class FloatWindowSmallView extends LinearLayout {
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MyCamera myCamera;
    private MyCameraEx myCameraEx;
    private Preference mPreference;
    private LocalReceiver mReceiver;
    private LocalBroadcastManager mBroadcastManager;
    private static final String TAG = "MyCamera";
    private Context mContext;

    public FloatWindowSmallView(Context context) {
        super(context);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.float_window, this);
        View view = findViewById(R.id.small_window_layout);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);             // 设置该SurfaceView自己不维护缓冲
        mPreference = Preference.getInstance(getContext());
        mBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        mReceiver = new LocalReceiver();
        mBroadcastManager.registerReceiver(mReceiver, new IntentFilter("com.gl.camera.RESTART_LISTEN"));
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                startListen();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void startListen() {
        myCamera = new MyCamera(mContext, mSurfaceHolder, mPreference.getPort(), mPreference.getPassword());
        myCamera.setResolution(mPreference.getResolution());
        myCamera.setQuality(mPreference.getQuality());
        myCamera.setFPS(mPreference.getFPS());
        myCamera.setDrop(mPreference.getDrop());
        myCamera.setDropSimilarity(mPreference.getDropSimilarity());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "开始监听");
                myCamera.listen();
                myCamera.waitRequest();
            }
        }).start();
        myCameraEx = new MyCameraEx(getContext(), mSurfaceHolder, "43.224.34.90", 3295, mPreference.getPassword(), mPreference.getDeviceId());
        myCameraEx.setResolution(mPreference.getResolution());
        myCameraEx.setQuality(mPreference.getQuality());
        myCameraEx.setFPS(mPreference.getFPS());
        myCameraEx.setDrop(mPreference.getDrop());
        myCameraEx.setDropSimilarity(mPreference.getDropSimilarity());
        new Thread(new Runnable() {
            @Override
            public void run() {
                myCameraEx.connToServer();
                myCameraEx.waitRequest();
                myCameraEx.checkHeartbeat();
            }
        }).start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i(TAG, "onDetachedFromWindow");
        mBroadcastManager.unregisterReceiver(mReceiver);
        myCamera.stopWaiting();
        myCameraEx.disConn(true);
    }

    private class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.gl.camera.RESTART_LISTEN")) {
                Log.i(TAG, "重启服务");
                myCamera.stopWaiting();
                myCameraEx.disConn(true);
                startListen();
            } else if (intent.getAction().equals("com.gl.camera.STOP_LISTEN")) {
                myCamera.stopWaiting();
                myCameraEx.disConn(true);
            }
        }
    }
}
