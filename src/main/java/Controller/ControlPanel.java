package Controller;

import Manager.ApplicationTimer;
import Manager.Exposure;
import Manager.ExposureFromVideo;
import Manager.Updatable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

public class ControlPanel extends AnchorPane {

    @FXML
    public AnchorPane pnStart;
    @FXML
    public AnchorPane pnProgress;
    @FXML
    public AnchorPane pnComplete;

    @FXML
    public Button btnStart;
    @FXML
    public TextField txtVideoName;
    @FXML
    public TextField txtRuntime;
    @FXML
    public TextField txtFrameRate;
    @FXML
    public Spinner<Double> spnExposureDuration;
    @FXML
    public Spinner<Double> spnStartTime;
    @FXML
    public Slider sldSampleRate;
    @FXML
    public Button btnSelectVideo;
    @FXML
    public ProgressBar pgbExposureProgress;
    @FXML
    public Label lblElapsed;
    @FXML
    public Label lblRemaining;
    @FXML
    public Label lblDuration;
    @FXML
    public Label lblCompleted;
    @FXML
    public Button btnSave;
    @FXML
    public Button btnView;

    private Exposure exposure;
    private boolean exposureCreationComplete;

    private File inputVideo;
    private Updatable progressUpdater;
    private Thread exposureThread;

    public ControlPanel() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/layouts/ControlPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialize() {
        btnStart.setOnAction(startExposureActionEvent);
        btnStart.setDisable(true);
        spnExposureDuration.setDisable(true);
        spnStartTime.setDisable(true);
        sldSampleRate.setDisable(true);
        btnSelectVideo.setOnAction(videoSelectActionEvent);
        pnStart.setVisible(true);
        pnComplete.setVisible(false);
        pnProgress.setVisible(false);
    }

    EventHandler<ActionEvent> videoSelectActionEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if (exposure != null) {
                if (exposure.isPlaying()) {
                    exposure.stop();
                }
                exposure.destroy();
            }
            FileChooser videoChooser = new FileChooser();
            videoChooser.setTitle("Select Video");
            videoChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi"));
            inputVideo = videoChooser.showOpenDialog(ControlPanel.this.getScene().getWindow());
            if (inputVideo != null) {
                exposure = new ExposureFromVideo(inputVideo);
                txtVideoName.setText(inputVideo.getName());
                txtFrameRate.setText(Math.round(exposure.getFramerate()) + "FPS");
                double runtime = exposure.getRuntime() / 1000000.0;
                System.out.println("Runtime:" + runtime);
                int hours = (int) runtime / 3600;
                int minutes = (int) ((runtime % 3600) / 60);
                double seconds = Math.round((runtime - (hours * 3600) - (minutes * 60)) * 100) / 100.0;
                txtRuntime.setText(hours + ":" + minutes + ":" + seconds);
                btnStart.setDisable(false);
                pnStart.setVisible(false);
                spnExposureDuration.setDisable(false);
                spnExposureDuration.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.00, runtime, runtime));
                spnStartTime.setDisable(false);
                spnStartTime.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.00, runtime, 0.00));
                spnStartTime.valueProperty().addListener((obs, oldVal, newVal) -> {
                    spnExposureDuration.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.00, runtime - newVal, spnExposureDuration.getValue() < runtime - newVal ? spnExposureDuration.getValue() : runtime - newVal));
                });
                sldSampleRate.setDisable(false);
                sldSampleRate.setValue(Math.round(exposure.getFramerate()));
                sldSampleRate.setMax(Math.round(exposure.getFramerate()));
                sldSampleRate.setMin(1);
                sldSampleRate.setShowTickMarks(true);
                sldSampleRate.setMajorTickUnit(10);
                sldSampleRate.setMinorTickCount(10);
                sldSampleRate.setSnapToTicks(true);
                sldSampleRate.setBlockIncrement(1);
                sldSampleRate.valueProperty().addListener((observable, oldValue, newValue) -> {
                    sldSampleRate.setValue(Math.round(newValue.doubleValue()));
                    txtFrameRate.setText(Math.round(newValue.doubleValue()) + " FPS");
                });
            }
        }
    };

    EventHandler<ActionEvent> startExposureActionEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if (progressUpdater != null) {
                ApplicationTimer.getInstance().unregister(progressUpdater);
                progressUpdater = null;
            }
            pnProgress.setVisible(true);
            pnComplete.setVisible(false);
            long durationFrames = (long) (spnExposureDuration.getValue() * exposure.getFramerate());
            int sampleRate = (int) sldSampleRate.getValue();
            exposure.setStartTime((long) (spnStartTime.getValue() * 1000000));
            exposureCreationComplete = false;
            System.out.println("Duration: " + spnExposureDuration.getValue() + "s");
            exposureThread = exposure.createExposure(durationFrames, sampleRate, Exposure.ExposureMethod.Average);
            progressUpdater = t -> {
                pgbExposureProgress.setProgress(exposure.getExposureProgress());
                lblElapsed.setText(exposure.getElapsedTimeStamp());
                lblRemaining.setText(exposure.getEstimatedRemainingTimeStamp());
                lblDuration.setText(exposure.getEstimatedDurationTimeStamp());
                if (exposure.isComplete() && !exposureCreationComplete) {
                    exposureCreationComplete = true;
                    pnProgress.setVisible(false);
                    pnComplete.setVisible(true);
                    btnSave.setOnAction(saveExposureEvent);
                    btnView.setOnAction(viewExposureEvent);
                }
            };
            ApplicationTimer.getInstance().register(progressUpdater);
        }
    };

    EventHandler<ActionEvent> saveExposureEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            Mat completedExposure = exposure.getExposure();
            FileChooser saveChooser = new FileChooser();
            String[] nameArr = inputVideo.getName().split("\\.");
            System.out.println(nameArr[0]);
            nameArr = Arrays.copyOf(nameArr, nameArr.length - 1);
            String outputName = String.join(".", nameArr) + ".png";
            saveChooser.setInitialFileName(outputName);
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, ...)", "*.png", "*.jpg", "*.bmp");
            saveChooser.getExtensionFilters().add(extFilter);
            File saveFile = saveChooser.showSaveDialog(ControlPanel.this.getScene().getWindow());
            if (saveFile != null) {
                imwrite(saveFile.getAbsolutePath(), completedExposure);
            }
        }
    };

    EventHandler<ActionEvent> viewExposureEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {

        }
    };

    public void shutdown() throws InterruptedException {
        System.out.println("Shutting Down");
        if (progressUpdater != null) {
            ApplicationTimer.getInstance().unregister(progressUpdater);
        }
        if (exposure.isRunning()) {
            exposure.setInterrupted(true);
        }
    }

}
