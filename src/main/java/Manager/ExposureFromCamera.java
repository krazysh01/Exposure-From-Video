package Manager;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.awt.image.BufferedImage;

public class ExposureFromCamera extends Exposure implements Updatable{

    private OpenCVFrameGrabber grabber;

    public ExposureFromCamera(int device){
        grabber = new OpenCVFrameGrabber(device);
        ApplicationTimer.getInstance().register(this);
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

    @Override
    public void destroy() {
        if(grabber != null){
            try {
                grabber.close();
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
        ApplicationTimer.getInstance().unregister(this);
    }
}
