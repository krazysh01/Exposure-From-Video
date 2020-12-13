package com.krazysoft.expovid.Exposures;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;

public class AverageExposureMethod extends ExposureMethod {

    private int frameCount = 0;

    @Override
    public void Combine(Mat frame) {
        if(exposure == null){
            exposure = frame.clone();
            frameCount = 1;
            return;
        }
        frameCount++;
        double frame_average = 1.0 / frameCount;
        Mat result = new Mat();
//                double frame_average = 0.5;
        opencv_core.addWeighted(frame, frame_average, exposure, 1 - frame_average, 0.0, result);
        exposure = result;
        return;
    }
}
