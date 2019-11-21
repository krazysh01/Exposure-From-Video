package com.krazysoft.expovid.Manager;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;

public class ExposureFromVideo extends Exposure {

    public ExposureFromVideo(File file) {
        grabber = new FFmpegFrameGrabber(file);
        try {
            grabber.start();
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        if (grabber != null) {
            try {
                grabber.close();
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
