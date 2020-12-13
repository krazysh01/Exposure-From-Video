package com.krazysoft.expovid.Exposures;

import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

public class ExperimentalColourExposureMethod extends ExposureMethod{

    private long[][][] sumFrame;
    private int sampleCount;
    private int width;
    private int height;
    private int channels;
    private int type;

    @Override
    public void Combine(Mat frame) {
        width = frame.cols();
        height = frame.rows();
        channels = frame.channels();
        if(sumFrame == null) {
            type = frame.type();
            sumFrame = new long[height][width][channels];
        }
        UByteRawIndexer frameIndexer = frame.createIndexer();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for(int z = 0; z < channels; z++) {
                    sumFrame[y][x][z] += frameIndexer.get(y, x, z);
                }
            }
        }
        sampleCount++;
        return;
    }

    @Override
    public Mat finalizeExposure() {
        Mat outputMat = new Mat(height, width, type);
        UByteRawIndexer outputIndexer = outputMat.createIndexer();
        for (int y = 0; y < outputMat.rows(); y++) {
            for (int x = 0; x < outputMat.cols(); x++) {
                for(int z = 0; z < channels; z++) {
                    int averagePixel = (int) (sumFrame[y][x][z] / sampleCount);
                    outputIndexer.put(y, x, z, averagePixel);
                }
            }
        }
        return outputMat;
    }
}
