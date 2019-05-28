package Manager;

import Interfaces.VideoPlayer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;

public abstract class Exposure {

    protected VideoPlayer player;

    private boolean playing = false;

    public void setVideoPlayer(VideoPlayer player){
        this.player = player;
    }

    public void play(){
        playing = true;
    }

    public void stop(){
        playing = false;
    }

    public boolean isPlaying(){
        return playing;
    }

    public abstract void destroy();


    public static BufferedImage convertFrameToBufferedImage(Frame frame){
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.convert(frame);
    }

    public static Image convertFrameToJFXImage(Frame frame){
        BufferedImage image = convertFrameToBufferedImage(frame);
        if(image != null)
            return SwingFXUtils.toFXImage(image, null);
        else
            return null;
    }
}
