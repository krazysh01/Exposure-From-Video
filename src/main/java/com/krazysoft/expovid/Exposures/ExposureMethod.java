package com.krazysoft.expovid.Exposures;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatExpr;
import org.bytedeco.opencv.opencv_core.MatVector;

public abstract class ExposureMethod {

    protected Mat exposure;
    public abstract void Combine(Mat frame);
    public Mat finalizeExposure() {
        return exposure;
    };

    public static boolean areMatIdentical(Mat a, Mat b) {
        if( a == null || b == null){
            return false;
        }
        if( a.cols() == b.cols() && a.rows() == b.rows() && a.channels() == b.channels()){
            MatExpr result = opencv_core.subtract(a, b);
            MatVector diff = new MatVector();
            opencv_core.split(result.asMat(), diff);
            if(opencv_core.countNonZero(diff.get(0)) == 0
                    && opencv_core.countNonZero(diff.get(1)) == 0
                    && opencv_core.countNonZero(diff.get(2)) == 0)
            {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static double calculatePixelBrightness(int[] pixel) {
        return (pixel[0] * 0.3) + (pixel[1] * 0.59) + (pixel[2] * 0.11);
    }

}
