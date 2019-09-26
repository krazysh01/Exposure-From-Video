import Controller.ControlPanel;
import Controller.HomeScreen;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        ControlPanel controlPanel = new ControlPanel();
        Scene scene = new Scene(controlPanel);
//        HomeScreen homeScreen = new HomeScreen();
//        Scene scene = new Scene(homeScreen);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.out.println("Finishing");
                try {
                    controlPanel.shutdown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        });
        primaryStage.show();
    }

    public static void launch(String[] args){
        Application.launch(args);
    }
}
