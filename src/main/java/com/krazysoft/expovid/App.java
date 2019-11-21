package com.krazysoft.expovid;

import com.krazysoft.expovid.Controller.ControlPanel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        ControlPanel controlPanel = new ControlPanel();
        Scene scene = new Scene(controlPanel);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Finishing");
            try {
                controlPanel.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }
}
