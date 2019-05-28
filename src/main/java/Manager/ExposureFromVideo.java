package Manager;

import Interfaces.VideoPlayer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.bytedeco.javacv.*;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

public class ExposureFromVideo extends Exposure  implements Updatable{

    private final File file;
    private FrameGrabber grabber;

    public ExposureFromVideo(File file){
        this.file = file;
        ApplicationTimer.getInstance().register(this);
        grabber = new FFmpegFrameGrabber(file);
        try {
            grabber.start();
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void Update(long dt){
        if(isPlaying()){
            try {
                Image frame = Exposure.convertFrameToJFXImage(grabber.grab());
                if(frame != null) {
                    this.player.showFrame(frame);
                }
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void destroy(){
        if(grabber != null) {
            try {
                grabber.close();
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
        ApplicationTimer.getInstance().unregister(this);
    }

}
