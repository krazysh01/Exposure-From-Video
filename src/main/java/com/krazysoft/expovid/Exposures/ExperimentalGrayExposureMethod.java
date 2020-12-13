package com.krazysoft.expovid.Exposures;

import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;

import java.util.ArrayList;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

public class ExperimentalGrayExposureMethod extends ExposureMethod{

    private long[][] sumFrame;
    private int sampleCount;
    private int width;
    private int height;

    @Override
    public void Combine(Mat frame) {
        width = frame.cols();
        height = frame.rows();
        if(sumFrame == null) {
            sumFrame = new long[height][width];
        }
        Mat grayImage = new Mat(height, width, CV_8UC1);
        cvtColor(frame, grayImage, CV_BGR2GRAY);
        UByteRawIndexer grayImageIndexer = grayImage.createIndexer();
        for (int y = 0; y < grayImage.rows(); y++) {
            for (int x = 0; x < grayImage.cols(); x++) {
                sumFrame[y][x] += grayImageIndexer.get(y, x);
            }
        }
        sampleCount++;
        return;
    }

    @Override
    public Mat finalizeExposure() {
        Mat outputMat = new Mat(height, width, CV_8UC1);
        UByteRawIndexer outputIndexer = outputMat.createIndexer();
        for (int y = 0; y < outputMat.rows(); y++) {
            for (int x = 0; x < outputMat.cols(); x++) {
                int averagePixel = (int) (sumFrame[y][x]/sampleCount);
                outputIndexer.put(y, x, averagePixel);
            }
        }
        return outputMat;
    }
}
