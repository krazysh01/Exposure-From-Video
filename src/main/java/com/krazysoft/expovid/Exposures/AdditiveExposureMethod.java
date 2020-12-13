package com.krazysoft.expovid.Exposures;

import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;

public class AdditiveExposureMethod extends ExposureMethod {

    @Override
    public void Combine(Mat frame) {
        if(exposure == null){
            exposure = frame.clone();
            return;
        }
        if(ExposureMethod.areMatIdentical(exposure, frame)) {
            return;
        }
        Mat brightest = exposure.clone();
        UByteRawIndexer brightestIndexer = brightest.createIndexer();
        UByteRawIndexer exposureIndexer = exposure.createIndexer();
        UByteRawIndexer frameIndexer = frame.createIndexer();
        for (int y = 0; y < frame.rows(); y++) {
            for (int x = 0; x < frame.cols(); x++) {
                int[] exposurePixel = new int[exposure.channels()];
                for (int c = 0; c < exposure.channels(); c++) {
                    exposurePixel[c] = exposureIndexer.get(y, x, c);
                }
                int[] framePixel = new int[frame.channels()];
                for (int c = 0; c < frame.channels(); c++) {
                    framePixel[c] = frameIndexer.get(y, x, c);
                }
                double exposureBrightness = ExposureMethod.calculatePixelBrightness(exposurePixel);
                double frameBrightness = ExposureMethod.calculatePixelBrightness(framePixel);
                if (frameBrightness > exposureBrightness) {
                    for (int c = 0; c < exposure.channels(); c++) {
                        brightestIndexer.put(y, x, c, framePixel[c]);
                    }
                }
            }
        }
        exposure = brightest;
        return;
    }
}
