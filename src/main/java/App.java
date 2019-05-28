import Controller.HomeScreen;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        HomeScreen home = new HomeScreen();
        Scene scene = new Scene(home);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if(event.equals(WindowEvent.WINDOW_CLOSE_REQUEST)){
                    System.exit(0);
                }
            }
        });
        primaryStage.show();
    }

    public static void launch(String[] args){
        Application.launch(args);
    }
}
