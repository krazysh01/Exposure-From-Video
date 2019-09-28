package Manager;

import Interfaces.VideoPlayer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;

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
                    grabber.setFrameNumber((int) (startTime * getFramerate()));
                    long sampleCount = 0;
                    int skipInterval = (int) Math.round(getFramerate() / sampleRate);
                    System.out.println("Skip Frames: " + skipInterval);
                    long totalSample = (long) (duration / (getFramerate() / sampleRate));
                    if (grabber.getLengthInFrames() < totalSample) {
                        totalSample = grabber.getLengthInFrames();
                    }
                    System.out.println("Total Sample Frames: " + totalSample);
                    OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                    exposureProgress = 0;
                    elapsedTime = 0;
                    estimatedDuration = 0;
                    long lastLoop = System.currentTimeMillis();
                    System.out.println("Starting Creation");
                    while (!complete && !Thread.interrupted() && !interrupted) {
                        elapsedTime += System.currentTimeMillis() - lastLoop;
                        lastLoop = System.currentTimeMillis();
                        Frame _frame = grabber.grabImage();
                        if (_frame == null || _frame.image == null) {
                            continue;
                        }
                        Mat frame = matConverter.convert(_frame);

                        sampleCount++;
                        if (exposure == null) {
                            exposure = frame;
                        } else {
                            switch (method) {
                                case Average:
                                    exposure = CombineAverage(exposure, frame, sampleCount);
                                    break;
                                case Additive:
                                    exposure = CombineAdditive(exposure, frame);
                                    break;
                            }
                        }
                        exposureProgress = (sampleCount * 1.0) / totalSample;
                        estimatedDuration = (long) (elapsedTime / exposureProgress);
                        if (skipInterval > 1)
                            grabber.setVideoFrameNumber(grabber.getFrameNumber() + skipInterval);
                        if (sampleCount >= totalSample) {
                            complete = true;
                            running = false;
                            break;
                        }
                    }
                    if (complete) {
                        System.out.println("Exposure Complete");
                    } else {
                        System.out.println("Exposure Interrupted");
                    }
                } catch (FrameGrabber.Exception e) {
                    System.err.println(e.getLocalizedMessage());
                }
            }

            Mat CombineAdditive(Mat exposure, Mat frame) {
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
                        System.out.print(exposurePixel.toString() + ":" + exposureBrightness);
                        System.out.print(" - ");
                        System.out.println(framePixel.toString() + ":" + frameBrightness);
                        System.out.flush();
                        if (frameBrightness > exposureBrightness) {
                            for (int c = 0; c < exposure.channels(); c++) {
                                exposureIndexer.put(y, x, c, exposurePixel[c]);
                            }
                        }
                    }
                }
                return exposure;
            }

            Mat CombineAverage(Mat exposure, Mat frame, long frameCount) {
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
