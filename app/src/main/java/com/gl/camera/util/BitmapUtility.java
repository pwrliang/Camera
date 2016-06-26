package com.gl.camera.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liang on 2016/5/5.
 */
public class BitmapUtility {
    public final static int WHITE = 0xFFFFFFFF;
    public final static int BLACK = 0xFF000000;
    public final static int WIDTH = 800;
    public final static int HEIGHT = 800;

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, WIDTH, HEIGHT, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }

        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * 将图片进行处理，变成灰图再转化为直方图
     */
    public static Mat histogram(Mat img) {
        Mat b_hist = null;
        Mat grayMat = new Mat();
        Imgproc.cvtColor(img, grayMat, Imgproc.COLOR_RGB2GRAY, 4);
        List<Mat> bgr_planes = new ArrayList<>();
        Core.split(grayMat, bgr_planes);
        MatOfInt histSize = new MatOfInt(256);
        final MatOfFloat histRange = new MatOfFloat(0f, 256f);
        boolean accumulate = false;
        b_hist = new Mat();
        //生成直方图
        Imgproc.calcHist(bgr_planes, new MatOfInt(0), new Mat(), b_hist, histSize, histRange, accumulate);
        grayMat.release();
        histSize.release();
        histRange.release();
        for (Mat mat : bgr_planes) {
            mat.release();
        }
        return b_hist;
    }

    /*
    * 比较两个直方图
    * @param hist1直方图1
    * @param hist2直方图2
    * @param similarity相似度
    * @return 两张图片的相似度高于Similarity时返回true，否则返回false
    * */
    public static boolean compareMat(Mat hist1, Mat hist2, int similarity) {
        //该算法约相似，值越小
        double rate = 0;
        rate = Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_BHATTACHARYYA);//巴氏系数
        if ((1 - rate) * 100 > similarity)
            return true;
        return false;
    }
}
