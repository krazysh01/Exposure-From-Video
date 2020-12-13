package com.krazysoft.expovid.Manager;

import com.krazysoft.expovid.Exposures.*;
import com.krazysoft.expovid.Interfaces.VideoPlayer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Mat;

import java.awt.image.BufferedImage;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

public abstract class Exposure {

    public enum ExposureType {Additive, Average, ExperimentalGray, ExperimentalColour}

    protected VideoPlayer player;
    protected FFmpegFrameGrabber grabber;
    private long startTime;
    private boolean running;
    private boolean complete;
    private volatile boolean interrupted;
    protected ExposureMethod exposureMethod;
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

    public Thread createExposure(long duration, int sampleRate, ExposureType method) {
        running = true;
        complete = false;
        interrupted = false;
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
                    if(jumpFrames < 1){
                        waitFrames = waitFrames / jumpFrames;
                        jumpFrames = 1;
                    }
                    int sinceLastSkip = 0;
                    double skipBank = jumpFrames;
                    double waitBank = waitFrames;

                    long totalSample = (long) (duration / (getFramerate() / sampleRate));
                    if (grabber.getLengthInFrames() < totalSample) {
                        totalSample = grabber.getLengthInFrames();
                    }
                    OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                    exposureProgress = 0;
                    elapsedTime = 0;
                    estimatedDuration = 0;
                    long lastLoop = System.currentTimeMillis();
                    switch (method) {
                        case Average:
                            exposureMethod = new AverageExposureMethod();
                            break;
                        case Additive:
                            exposureMethod = new AdditiveExposureMethod();
                            break;
                        case ExperimentalGray:
                            exposureMethod = new ExperimentalGrayExposureMethod();
                            break;
                        case ExperimentalColour:
                            exposureMethod = new ExperimentalColourExposureMethod();
                            break;
                    }
                    while (!complete && !Thread.interrupted() && !interrupted) {
                        elapsedTime += System.currentTimeMillis() - lastLoop;
                        lastLoop = System.currentTimeMillis();
                        Frame _frame = grabber.grabImage();
                        if (_frame == null || _frame.image == null) {
                            continue;
                        }
                        Mat frame = matConverter.convert(_frame);
                        sampleCount++;
                        exposureMethod.Combine(frame);
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
                            for(int i = 0; i < skipCount; i++){
                                grabber.grabImage();
                            }
                            skipBank -= skipCount;
                            skipBank += jumpFrames;
                            waitBank -= (int) waitBank;
                            waitBank += waitFrames;
                            sinceLastSkip = 0;
                        }
                    }
                } catch (FrameGrabber.Exception e) {
                    System.err.println(e.getLocalizedMessage());
                }
            }

        };
        Thread exposureThread = new Thread(exposureRunnable);
        exposureThread.start();
        return exposureThread;
    }

    public Mat getExposure() {
        return exposureMethod.finalizeExposure();
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
