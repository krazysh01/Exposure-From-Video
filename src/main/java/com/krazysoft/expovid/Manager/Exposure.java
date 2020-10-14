package com.krazysoft.expovid.Manager;

import com.krazysoft.expovid.Interfaces.VideoPlayer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatExpr;
import org.bytedeco.opencv.opencv_core.MatVector;

import java.awt.image.BufferedImage;

public abstract class Exposure {

    public enum ExposureMethod {Additive, Average}

    protected VideoPlayer player;
    protected FFmpegFrameGrabber grabber;
    private long startTime;
    private boolean running;
    private boolean complete;
    private volatile boolean interrupted;
    protected Mat exposure;
    private double exposureProgress;
    private long elapsedTime;
    private long estimatedDuration;

    private boolean playing = false;

    public void setVideoPlayer(VideoPlayer player) {
        this.player = player;
    }

    public void play() {
        playing = true;
    }

    public void stop() {
        playing = false;
    }

    public boolean isPlaying() {
        return playing;
    }

    public abstract void destroy();

    public long getRuntime() {
        if (grabber != null) {
            return grabber.getLengthInTime();
        } else {
            return -1;
        }
    }

    public double getFramerate() {
        if (grabber != null) {
            return grabber.getFrameRate();
        } else {
            return -1;
        }
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public Thread createExposure(long duration, int sampleRate, ExposureMethod method) {
        running = true;
        complete = false;
        interrupted = false;
        exposure = null;
        Runnable exposureRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    grabber.restart();
                    if(startTime != 0)
                        grabber.setTimestamp(startTime);

                    long sampleCount = 0;

                    double skipFrames = (int) getFramerate() - (sampleRate * 1.0);
                    double jumpFrames = skipFrames / (sampleRate * 1.0);
                    double waitFrames = 1;
//                    System.out.println("Frame Rate: " + getFramerate());
//                    System.out.println("After every " + waitFrames + " frame/s skip " + jumpFrames + " frame/s");
                    if(jumpFrames < 1){
                        waitFrames = waitFrames / jumpFrames;
                        jumpFrames = 1;
                    }
//                    System.out.println("After every " + waitFrames + " frame/s skip " + jumpFrames + " frame/s");
                    int sinceLastSkip = 0;
                    double skipBank = jumpFrames;
                    double waitBank = waitFrames;

                    long totalSample = (long) (duration / (getFramerate() / sampleRate));
                    if (grabber.getLengthInFrames() < totalSample) {
                        totalSample = grabber.getLengthInFrames();
                    }
//                    System.out.println("Total Sample Frames: " + totalSample);
                    OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                    exposureProgress = 0;
                    elapsedTime = 0;
                    estimatedDuration = 0;
                    long lastLoop = System.currentTimeMillis();
//                    System.out.println("Starting Creation");
                    while (!complete && !Thread.interrupted() && !interrupted) {
                        elapsedTime += System.currentTimeMillis() - lastLoop;
                        lastLoop = System.currentTimeMillis();
                        Frame _frame = grabber.grabImage();
                        if (_frame == null || _frame.image == null) {
//                            System.out.println("No Image");
                            continue;
                        }
                        Mat frame = matConverter.convert(_frame);
                        sampleCount++;
//                        System.out.println(grabber.getFrameNumber());
//                        System.out.println("Frames Identical: "+areMatIdentical(exposure, frame));
                        switch (method) {
                            case Average:
                                exposure = CombineAverage(exposure, frame, sampleCount);
                                break;
                            case Additive:
                                exposure = CombineAdditive(exposure, frame);
                                break;
                        }
                        exposureProgress = (sampleCount * 1.0) / totalSample;
                        estimatedDuration = (long) (elapsedTime / exposureProgress);
                        if (sampleCount >= totalSample) {
                            complete = true;
                            running = false;
                            break;
                        }
                        sinceLastSkip++;
                        if(sinceLastSkip >= (int) waitBank) {
                            int skipCount = (int) Math.round(skipBank);
//                            System.out.println("Skipping " + skipCount + " Frames after " + sinceLastSkip+ " frames");
                            for(int i = 0; i < skipCount; i++){
                                grabber.grabImage();
                            }
                            skipBank -= skipCount;
                            skipBank += jumpFrames;
                            waitBank -= (int) waitBank;
                            waitBank += waitFrames;
//                            System.out.println("Next Skip: " + skipBank);
                            sinceLastSkip = 0;
                        }
                    }
                    if (complete) {
//                        System.out.println("Exposure Complete");
                    } else {
//                        System.out.println("Exposure Interrupted");
                    }
                } catch (FrameGrabber.Exception e) {
                    System.err.println(e.getLocalizedMessage());
                }
            }

            Mat CombineAdditive(Mat exposure, Mat frame) {
                if(exposure == null){
                    return frame.clone();
                }
                if(areMatIdentical(exposure, frame)) {
                    //System.out.println("Frames are Identical");
                    return exposure.clone();
                }
                Mat brightest = exposure.clone();
//                System.out.println("New Frame");
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
                        double exposureBrightness = calculatePixelBrightness(exposurePixel);
                        double frameBrightness = calculatePixelBrightness(framePixel);
                        if (frameBrightness > exposureBrightness) {
                            for (int c = 0; c < exposure.channels(); c++) {
                                brightestIndexer.put(y, x, c, framePixel[c]);
                            }
                        }
                    }
                }
                return brightest;
            }

            Mat CombineAverage(Mat exposure, Mat frame, long frameCount) {
                if(exposure == null){
                    return frame.clone();
                }
                double frame_average = 1.0 / frameCount;
                Mat result = new Mat();
                opencv_core.addWeighted(frame, frame_average, exposure, 1 - frame_average, 0.0, result);
                return result;
            }

        };
        Thread exposureThread = new Thread(exposureRunnable);
        exposureThread.start();
        return exposureThread;
    }

    public Mat getExposure() {
        return exposure;
    }

    public double getExposureProgress() {
        return exposureProgress;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isComplete() {
        return complete;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public String getElapsedTimeStamp() {
        return getTimeStamp(elapsedTime);
    }

    public long getEstimatedDuration() {
        return estimatedDuration;
    }

    public String getEstimatedDurationTimeStamp() {
        return getTimeStamp(estimatedDuration);
    }

    public long getEstimatedRemainingTime() {
        return estimatedDuration - elapsedTime;
    }

    public String getEstimatedRemainingTimeStamp() {
        return getTimeStamp(getEstimatedRemainingTime());
    }

    public void completeExposureEarly() {
        complete = true;
    }

    public static BufferedImage convertFrameToBufferedImage(Frame frame) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.convert(frame);
    }

    public static Image convertFrameToJFXImage(Frame frame) {
        BufferedImage image = convertFrameToBufferedImage(frame);
        if (image != null)
            return SwingFXUtils.toFXImage(image, null);
        else
            return null;
    }

    private static double calculatePixelBrightness(int[] pixel) {
        return (pixel[0] * 0.3) + (pixel[1] * 0.59) + (pixel[2] * 0.11);
    }

    private static boolean areMatIdentical(Mat a, Mat b) {
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

    private static String getTimeStamp(long time) {
        int hours = (int) (time / (1000 * 60 * 60));
        int minutes = (int) ((time / (1000 * 60)) - (hours * 60));
        int seconds = (int) ((time / 1000) - (hours * 60 * 60) - (minutes * 60));
        String timestamp = "";
        if (hours > 0) {
            timestamp += hours + " hours ";
        }
        if (minutes > 0) {
            timestamp += minutes + " minutes ";
        }
        timestamp += seconds + " seconds";
        return timestamp;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

}
