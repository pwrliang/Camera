package com.gl.camera.model;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.gl.camera.util.BitmapUtility;
import com.gl.shared.Data;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Liang on 2016/4/5.
 */
public class MyCamera implements Camera.PreviewCallback {
    public static final int CAMERA_FRONT = 0;
    public static final int CAMERA_BACK = 1;
    private Camera mCamera;
    private int mPreviewFormat, mPreviewWidth, mPreviewHeight;
    private boolean mIsPreview;      //是否在预览中
    private int mCameraId;
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private int mPort;
    protected String mPassword;
    private static final String TAG = "MyCamera";
    protected BlockingQueue<byte[]> mFrames = new LinkedBlockingQueue<>();
    private Mat lastFrame;//记录上一张照片
    private Context mContext;
    private SurfaceHolder mSurfaceHolder;
    private int mQuality = 50;//画质
    private int mResolution = 50;//分辨率
    private int mFPS = 15;//帧率
    private boolean mDrop;//丢弃相似帧
    private int mDropSimilarity;//丢帧相似度

    public MyCamera(Context context, SurfaceHolder surfaceHolder, int port, String password) {
        this.mContext = context;
        this.mSurfaceHolder = surfaceHolder;
        this.mPort = port;
        this.mPassword = password;
    }

    public MyCamera(Context context, SurfaceHolder surfaceHolder, String password) {
        this.mContext = context;
        this.mSurfaceHolder = surfaceHolder;
        this.mPassword = password;
    }

    public void listen() {
        try {
            mServerSocket = new ServerSocket(mPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setQuality(int quality) {
        mQuality = quality;
    }

    public void setResolution(int resolution) {
        mResolution = resolution;
    }

    public void setFPS(int fps) {
        mFPS = fps;
    }

    public void setDrop(boolean drop) {
        mDrop = drop;
    }

    public void setDropSimilarity(int dropSimilarity) {
        mDropSimilarity = dropSimilarity;
    }

    private Camera openFrontFacingCamera() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }

    private Camera openBackFacingCamera() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }
        return cam;
    }

    public static int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    protected synchronized boolean isPreview() {
        return mIsPreview;
    }

    /*
    * 初始化相机
    * */
    protected synchronized void initCamera(int cameraId) {
        //非预览状态
        if (!isPreview()) {
            //打开前置或者后置摄像头
            if (cameraId == CAMERA_FRONT) {
                mCamera = openFrontFacingCamera();
            } else if (cameraId == CAMERA_BACK) {
                mCamera = openBackFacingCamera();
            }
            mCameraId = cameraId;
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                List<int[]> fps = parameters.getSupportedPreviewFpsRange();
                List<Camera.Size> prevSizes = parameters.getSupportedPreviewSizes();
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    parameters.set("orientation", "portrait");
                    parameters.set("rotation", 90);
                }
                if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    parameters.set("orientation", "landscape");
                    parameters.set("rotation", 90);
                }
                parameters.setPreviewFpsRange(fps.get(0)[0], fps.get(0)[1]);//限制预览帧数
                //设置预览大小
                if (prevSizes.size() <= 3) {//只有3个分辨率，选最大的吧
                    parameters.setPreviewSize(prevSizes.get(0).width, prevSizes.get(0).height);
                } else {//超过3个，选倒数第三的分辨率
                    parameters.setPreviewSize(prevSizes.get(prevSizes.size() - 3).width, prevSizes.get(prevSizes.size() - 3).height);
                }

                if (focusModes.size() == 1) {//只有一种对焦模式
                    parameters.setFocusMode(focusModes.get(0));
                } else {//选择无限远对焦模式
                    for (String focusMode : focusModes) {
                        if (focusMode.equals("infinity")) {
                            parameters.setFocusMode("infinity");
                            break;
                        }
                    }
                }
                mPreviewFormat = parameters.getPreviewFormat();
                mPreviewWidth = parameters.getPreviewSize().width;
                mPreviewHeight = parameters.getPreviewSize().height;
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(mSurfaceHolder);                 // 通过SurfaceView显示取景画面
                mCamera.setPreviewCallback(this);         // 设置回调的类
                mCamera.startPreview();                                   // 开始预览
            } catch (Exception e) {
                e.printStackTrace();
            }
            mIsPreview = true;
        }
    }

    /*
    * 关闭相机
    * */
    protected synchronized void finishCamera() {
        // 如果camera不为null ,释放摄像头
        if (mCamera != null) {
            if (isPreview()) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            }
            mCamera.release();
            mCamera = null;
        }
        mIsPreview = false;
    }

    /*
    * 等待网络请求
    * */
    public void waitRequest() {
        Log.i(TAG, "监听线程开始 " + this);
        while (mServerSocket != null) {//死循环，用于等待请求
            try {
                mSocket = mServerSocket.accept();//收到请求
                InputStream inputStream = mSocket.getInputStream();
                Data data = Data.fromInputStream(inputStream);
                if (data == null) {
                    Log.i(TAG, "no data");
                    mSocket.close();
                    continue;
                } else {
                    if (data.getTag() == Data.TAG_START_FRONT || data.getTag() == Data.TAG_START_BACK) {
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
                            new SenderThread(mSocket.getOutputStream()).start();//开启新线程发送图像
                        } else {//  密码错误
                            data = new Data(Data.TAG_ERR_PASSWORD);
                            mSocket.getOutputStream().write(data.toBytes());
                            mSocket.close();                                     //关闭Socket
                        }
                    } else if (data.getTag() == Data.TAG_STOP) {                //结束监控
                        Log.i(TAG, "stop");
                        finishCamera();
                        mSocket.close();                                     //关闭Socket
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        Log.i(TAG, "监听线程停止" + this);
    }

    public void stopWaiting() {
        Log.i(TAG, "停止监听");
        try {
            finishCamera();
            mServerSocket.close();
            mServerSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*相机预览
    * 向阻塞队列加入帧*/
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        int max = 30;
        int min = 1;
        Random random = new Random();
        //产生1~30的随机数
        int rnd = random.nextInt(max) % (max - min + 1) + min;

        //Yuv转换成Bitmap
        YuvImage image = new YuvImage(data, mPreviewFormat, mPreviewWidth, mPreviewHeight, null);              //封装到Yuv中
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, mPreviewWidth, mPreviewHeight), 50, baos);                //压缩成JPEG
        Bitmap bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
        //开启了丢弃相似帧
        if (mDrop) {
            if (lastFrame == null) {
                lastFrame = new Mat(mPreviewHeight, mPreviewWidth, CvType.CV_8UC1);
                Utils.bitmapToMat(bitmap, lastFrame);
            } else {
                Mat thisFrame = new Mat(mPreviewHeight, mPreviewWidth, CvType.CV_8UC1);
                Utils.bitmapToMat(bitmap, thisFrame);
                //YUV->Histogram
                Mat hist1 = BitmapUtility.histogram(lastFrame);
                Mat hist2 = BitmapUtility.histogram(thisFrame);
                //相似度超过了mDropSimilarity，则不再继续执行
                boolean isSimilar = BitmapUtility.compareMat(hist1, hist2, mDropSimilarity);
                thisFrame.release();
                hist1.release();
                hist2.release();
                if (isSimilar) {
                    return;
                }
                Utils.bitmapToMat(bitmap, lastFrame);
            }
        }

        //正在预览，且产生的随机数小于给定的帧率
        if (rnd < mFPS && isPreview()) {
            //队列里的帧的数量比给定帧率的小，则向队列添加新帧
            if (mFrames.size() < mFPS) {
                try {
                    //定义新图片的宽和高
                    float scale = mResolution / 100.0f;
                    int newWidth = (int) (mPreviewWidth * scale);
                    int newHeight = (int) (mPreviewHeight * scale);
                    bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
                    baos.reset();
                    //纠正前置摄像头
                    if (mCameraId == CAMERA_FRONT) {
                        bitmap = BitmapUtility.RotateBitmap(bitmap, 180);
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, mQuality, baos);                  //压缩bitmap
                    baos.close();
                    mFrames.offer(baos.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                //队列帧数量超过给定帧率，则清空队列
                mFrames.clear();
            }
        }
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
            Log.i(TAG, "image sent:" + data.length);
            outputStream.write(dataPacket.toBytes());
            outputStream.flush();
        }

        @Override
        public void run() {
            while (mIsPreview) {
                try {
                    //队列空，等待一下
                    if (mFrames.size() == 0) {
                        Thread.sleep(1);
                        continue;
                    }
                    byte[] data = mFrames.take();
                    sendFrames(data);
                } catch (Exception e) {
                    e.printStackTrace();
                    //客户端掉线，关闭相机
                    if (e.getCause() != null && e.getCause().toString().contains("Broken pipe")) {
                        Log.e(TAG, "disconnetced!!");
                        finishCamera();
                    }
                }
            }
            Log.i(TAG, "Sender线程结束");
        }
    }

}
