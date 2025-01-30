package logbook.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class LogBookMessageFlowView extends Application {
    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("JavaFX Application");
        StackPane root = new StackPane();
        root.getChildren().add(label);
        Scene scene = new Scene(root, 400, 100);
        primaryStage.setScene(scene);
        primaryStage.setTitle("航海日誌改MessageFlow");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
