package com.yjm.camera.model;


import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.yjm.shared.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * Created by Liang on 2016/4/5.
 */
public class MyCameraEx extends MyCamera implements Camera.PreviewCallback {
    private static final String TAG = "MyCameraEx";
    private Socket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private String mIP;
    private int mPort;
    private String mIdentity;
    private boolean mWorking;
    private WaitRequestRunnable mThread;
    private long mLastSend;
    private Context mContext;

    public MyCameraEx(Context context, SurfaceHolder surfaceHolder, String IP, int port, String password, String identity) {
        super(context, surfaceHolder, password);
        this.mContext = context;
        this.mIP = IP;
        this.mPort = port;
        this.mIdentity = identity;
    }

    public void connToServer() {
        Log.i(TAG, "连接到服务器" + this);
        try {
            mSocket = new Socket();
            mSocket.setKeepAlive(true);
            SocketAddress address = new InetSocketAddress(mIP, mPort);
            mSocket.connect(address, 2000);
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
            Data data = new Data(Data.TAG_CAM_CONN_TO_SERVER);
            data.setData(mIdentity.getBytes("utf-8"));
            mOutputStream.write(data.toBytes());
            mOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
            //连接失败，重试
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            connToServer();
        }
    }

    private boolean isWorking() {
        return mWorking;
    }

    private void startWorking() {
        mWorking = true;
    }

    private void stopWorking() {
        mWorking = false;
    }


    public void checkHeartbeat() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "心跳检测线程开始" + MyCameraEx.this);
                while (isWorking()) {
                    if (mLastSend != 0 && System.currentTimeMillis() - mLastSend > 10 * 1000) {
                        mLastSend = System.currentTimeMillis();
                        Log.i(TAG, "超过10秒未收到心跳包");
                        finishCamera();
                        mThread.onPause();
                        reconnect();
                        mThread.onResume();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.i(TAG, "心跳检测线程结束" + MyCameraEx.this);
            }
        }).start();
    }


    class WaitRequestRunnable implements Runnable {
        private Object mPauseLock;
        private boolean mPaused;

        public WaitRequestRunnable() {
            mPauseLock = new Object();
            mPaused = false;
            startWorking();
        }

        public void run() {
            Log.i(TAG, "监听线程开始 " + MyCameraEx.this);
            while (isWorking()) {//死循环，用于等待请求
                try {
                    //等待请求
                    if (!mSocket.isConnected() || mInputStream.available() == 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    Log.i(TAG, "waiting for request" + MyCameraEx.this);
                    Data data = Data.fromInputStream(mInputStream);
                    if (data != null) {
                        if (data.getTag() == Data.TAG_HELLO) {
                            //正在传输图像，不回送心跳包
                            if(isPreview()) {
                                Data helloData = new Data(Data.TAG_HELLO);
                                mOutputStream.write(helloData.toBytes());
                                mOutputStream.flush();
                            }
                            //回送成功，则更新发送时间
                            mLastSend = System.currentTimeMillis();
                            Log.i(TAG, "TAG_HELLO");
                        } else if (data.getTag() == Data.TAG_START_FRONT || data.getTag() == Data.TAG_START_BACK) {
                            Log.i(TAG, "start");
                            if (data.getPassword().equals(mPassword)) {                  //验证密码是否正确
                                finishCamera();//关闭相机
                                //启动前置相机
                                if (data.getTag() == Data.TAG_START_FRONT) {
                                    initCamera(CAMERA_FRONT);//初始化相机准备捕获
                                } else if (data.getTag() == Data.TAG_START_BACK) {//启动后置相机
                                    initCamera(CAMERA_BACK);//初始化相机准备捕获
                                }
                                Thread.sleep(2000);                                 //等待相机启动完成
                                new SenderThread(mOutputStream).start();//开启新线程发送图像
                            } else {//  密码错误
                                data = new Data(Data.TAG_ERR_PASSWORD);
                                mOutputStream.write(data.toBytes());
                                mOutputStream.flush();
                                Log.i(TAG, "密码错误");
                            }
                        } else if (data.getTag() == Data.TAG_STOP) {                //结束监控
                            Log.i(TAG, "stop");
                            finishCamera();
                            reconnect();
                        }
                    } else {
                        Log.e(TAG, "Data is null");
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
//                    reconnect();
                } finally {
                    synchronized (mPauseLock) {
                        while (mPaused) {
                            try {
                                mPauseLock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "监听线程停止 " + MyCameraEx.this);
        }

        /**
         * Call this on pause.
         */
        public void onPause() {
            synchronized (mPauseLock) {
                mPaused = true;
            }
        }

        /**
         * Call this on resume.
         */
        public void onResume() {
            synchronized (mPauseLock) {
                mPaused = false;
                mPauseLock.notifyAll();
            }
        }

    }

    /*
    * 等待网络请求，该方法阻塞
    * */
    public void waitRequest() {
        mThread = new WaitRequestRunnable();
        new Thread(mThread).start();
    }

    /*
    * 断开服务器连接
    * @param stopWait为true时关闭工作线程
    * */
    public void disConn(boolean stopWait) {
        Log.i(TAG, "停止监听" + this);
        //停止发送心跳包
        //停止等待请求
        if (stopWait) {
            stopWorking();
        }
        try {
//            if (mInputStream != null) {
//                mInputStream.close();
//            }
//            if (mOutputStream != null) {
//                mOutputStream.close();
//            }
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void reconnect() {
        Log.e(TAG, "reconnect");
        disConn(false);
        connToServer();
    }

    /*
    * 发送数据子线程
    * 从阻塞队列取出帧，发送到监控端
    * */
    private class SenderThread extends Thread {
        private OutputStream outputStream;

        SenderThread(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        private void sendFrames(byte[] data) throws IOException {
            Data dataPacket = new Data(Data.TAG_VIDEO);
            dataPacket.setData(data);
            outputStream.write(dataPacket.toBytes());
            outputStream.flush();
        }

        @Override
        public void run() {
            while (isPreview()) {
                try {
                    //队列空，等待一下
                    if (mFrames.size() == 0) {
                        Thread.sleep(10);
                        continue;
                    }
                    Log.e(TAG, "send..");
                    byte[] data = mFrames.take();
                    if (mSocket.isConnected()) {
                        sendFrames(data);
                    } else {
                        new Exception("连接断开");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    finishCamera();
                    reconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                    //Broken pipe:有一方连接断开，则重新连接
                    if (e.getCause() != null && e.getCause().toString().contains("Broken pipe")) {
                        finishCamera();
                        reconnect();
                    }
                }
            }
            Log.i(TAG, "Sender线程结束");
        }
    }

}
