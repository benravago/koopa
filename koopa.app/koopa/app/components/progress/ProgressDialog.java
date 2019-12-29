package koopa.app.components.progress;

import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;

public class ProgressDialog {

    Task task;
    String title;

    public ProgressDialog(String title, Task task) {
        this.task = task;
        this.title = title;
    }

    public void run() {
        var alert = new Alert(AlertType.INFORMATION,"",
            new ButtonType("Cancel",ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        alert.setTitle(title);
        alert.setGraphic(new ProgressIndicator());

        alert.headerTextProperty().bind(task.titleProperty());
        alert.contentTextProperty().bind(task.messageProperty());

        task.stateProperty().addListener((observable,oldState,newState) -> {
            if (newState == Worker.State.SUCCEEDED) alert.close();
        });

        alert.setOnShown(e -> new Thread(task).start());

        alert.showAndWait();
    }

    public static abstract class Work<T> extends Task<T> {
        { init(); }
        protected void init() {};
        @Override protected void scheduled() { updateMessage("scheduled"); }
        @Override protected void running() { updateMessage("running"); }
        @Override protected void cancelled() { updateMessage("cancelled"); }
        @Override protected void failed() { updateMessage("failed"); }
        @Override protected void succeeded() { updateMessage("succeeded"); }
    }

}
