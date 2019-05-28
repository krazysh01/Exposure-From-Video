package Controller;

import Interfaces.VideoPlayer;
import Manager.Exposure;
import Manager.ExposureFromCamera;
import Manager.ExposureFromVideo;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

public class HomeScreen extends AnchorPane implements VideoPlayer {

    @FXML
    public ImageView imgVideo;
    @FXML
    public Button btnVideo;
    @FXML
    public Button btnCamera;

    private Exposure exposure;

    public HomeScreen() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/layouts/HomeScreen.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialize(){
        btnVideo.setOnAction(videoSelectActionListener);
        btnCamera.setOnAction(cameraSelectActionListener);
    }

    public void showFrame(Image frame){
        imgVideo.setImage(frame);
        imgVideo.setPreserveRatio(true);
    }

    EventHandler<ActionEvent> videoSelectActionListener = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent actionEvent) {
            if(exposure != null){
                if(exposure.isPlaying()){
                    exposure.stop();
                }
                exposure.destroy();
            }
            FileChooser videoChooser = new FileChooser();
            videoChooser.setTitle("Select Video");
            videoChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi"));
            File video = videoChooser.showOpenDialog(HomeScreen.this.getScene().getWindow());
            if(video != null){
                exposure = new ExposureFromVideo(video);
                exposure.setVideoPlayer(HomeScreen.this);
                exposure.play();
            }
        }
    };

    EventHandler<ActionEvent> cameraSelectActionListener = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if(exposure != null){
                if(exposure.isPlaying()){
                    exposure.stop();
                }
                exposure.destroy();
            }
            exposure = new ExposureFromCamera(0);
            exposure.setVideoPlayer(HomeScreen.this);
            exposure.play();
        }
    };

}
