package com.krazysoft.expovid.Controller;

import com.krazysoft.expovid.Manager.ApplicationTimer;
import com.krazysoft.expovid.Manager.Exposure;
import com.krazysoft.expovid.Manager.ExposureFromVideo;
import com.krazysoft.expovid.Manager.Updatable;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bytedeco.javacv.OpenCVFrameConverter;
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
    public ComboBox cmbMethod;
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
    public Label lblFinalDuration;
    @FXML
    public Button btnSelectNewVideo;
    @FXML
    public Button btnSave;
    @FXML
    public Button btnView;
    @FXML
    public Button btnCompleteExposure;

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
        cmbMethod.setDisable(true);
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
//                System.out.println("Runtime:" + runtime);
                int hours = (int) runtime / 3600;
                int minutes = (int) ((runtime % 3600) / 60);
                double seconds = Math.round((runtime - (hours * 3600) - (minutes * 60)) * 100) / 100.0;
                txtRuntime.setText(hours + ":" + minutes + ":" + seconds);
                btnStart.setDisable(false);
                pnStart.setVisible(false);
                pnComplete.setVisible(false);
                pnProgress.setVisible(false);
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
                cmbMethod.setItems(FXCollections.observableArrayList(Exposure.ExposureType.values()));
                cmbMethod.setDisable(false);
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
            btnStart.setDisable(true);
            spnExposureDuration.setDisable(true);
            spnStartTime.setDisable(true);
            sldSampleRate.setDisable(true);
            cmbMethod.setDisable(true);
            pnProgress.setVisible(true);
            pnComplete.setVisible(false);
            btnCompleteExposure.setOnAction(completeExposureEvent);
            long durationFrames = (long) (spnExposureDuration.getValue() * exposure.getFramerate());
            int sampleRate = (int) sldSampleRate.getValue();
            exposure.setStartTime((long) (spnStartTime.getValue() * 1000000));
            exposureCreationComplete = false;
//            System.out.println("Duration: " + spnExposureDuration.getValue() + "s");
            Exposure.ExposureType method = Exposure.ExposureType.Average;
            method = (Exposure.ExposureType) cmbMethod.getValue();
            exposureThread = exposure.createExposure(durationFrames, sampleRate, method);
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
                    btnSelectNewVideo.setOnAction(videoSelectActionEvent);
                    lblFinalDuration.setText("Duration: " + exposure.getEstimatedDurationTimeStamp());
                    btnStart.setDisable(false);
                    spnExposureDuration.setDisable(false);
                    spnStartTime.setDisable(false);
                    sldSampleRate.setDisable(false);
                    cmbMethod.setDisable(true);
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
//            System.out.println(nameArr[0]);
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
            Mat exposureImage = exposure.getExposure();
            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
            Image image = Exposure.convertFrameToJFXImage(converter.convert(exposureImage));
            ImageView imageView = new ImageView(image);
            BorderPane pane = new BorderPane();
            pane.setCenter(imageView);
            Scene scene = new Scene(pane);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setOnCloseRequest(
                    e -> {
                        e.consume();
                        stage.close();
                    }
            );
            stage.showAndWait();
        }
    };

    EventHandler<ActionEvent> completeExposureEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if(exposure.isRunning()){
                exposure.completeExposureEarly();
            }
        }
    };

    public void shutdown() throws InterruptedException {
//        System.out.println("Shutting Down");
        if (progressUpdater != null) {
            ApplicationTimer.getInstance().unregister(progressUpdater);
        }
        if (exposure != null && exposure.isRunning()) {
            exposure.setInterrupted(true);
        }
    }

}
