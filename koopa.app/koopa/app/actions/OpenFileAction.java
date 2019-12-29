package koopa.app.actions;

import java.nio.file.Path;
import java.util.ArrayList;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;

import koopa.app.WorkingSet;
import koopa.app.components.detail.Detail;

import fx.mvc.util.Views;
import static fx.mvc.util.Lookup.*;
import java.nio.file.Files;

import javafx.concurrent.Task;

import koopa.app.components.progress.ProgressDialog;
import koopa.cobol.parser.ParseResults;

public class OpenFileAction {

    final WorkingSet ws;

    Path cobolFile;
    ParseResults cobolResults;
    String cobolSource;

    public OpenFileAction(WorkingSet ws) {
        this.ws = ws;
    }

    public void run() {
        if (askUserForFile()) {
            if (parseFile()) {
                openFileTab();
            }
        }
    }

    boolean askUserForFile() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Open File...");
        fileChooser.setInitialDirectory​(ws.cwd.toFile());
        fileChooser.getExtensionFilters().addAll(
            filter("COBOL Source Files","cbl","cob"),
            filter("COBOL Copybook Files","cpy","copy")
        );
        var file = fileChooser.showOpenDialog(ws.root.getScene().getWindow());
        if (file != null) {
            cobolFile = file.toPath();
            return true;
        } else {
            return false;
        }
    }

    static FileChooser.ExtensionFilter filter(String tag, String... exts) {
        var l = new ArrayList<String>();
        var t = new StringBuilder();
        t.append(tag).append(' ');
        var c = '(';
        for (var e : exts) {
            if (! e.isBlank()) {
                e = e.trim();
                t.append(c).append(e);
                l.add("*."+e.toLowerCase());
                l.add("*."+e.toUpperCase());
                c = ',';
            }
        }
        return new FileChooser.ExtensionFilter(
            t.append(')').toString(), l.toArray(new String[l.size()])
        );
    }

    boolean parseFile() {
        var task = parseTask();
        new ProgressDialog("Parse file",task).run();

        if (task.isRunning()) {
            task.cancel(true);
        }

        // TODO: check for exception
        var state = task.getState();
        System.out.println("state "+state);

        return true;
        // return results or show exception
    }

    Task<Void> parseTask() {
        return new ProgressDialog.Work<Void>() {
            @Override
            protected void init() {
                updateTitle("Parsing "+cobolFile+" ...");
            }
            @Override
            protected Void call() throws Exception {
                var parser = ws.parserFactory.getParser();
                updateMessage("parsing");
                cobolResults = parser.parse(cobolFile);
                updateMessage("got results");
                cobolSource = Files.readString(cobolFile);
                updateMessage("got source");
                return null;
            }
        };
    }

    void openFileTab() {

        var id = "pr"+cobolResults.hashCode();
        var dir = cobolFile.getParent().toString();
        var file = cobolFile.getFileName().toString();

        var model = new Detail(id,dir,file,cobolSource,cobolResults);
        var cv = Views.loadController(model);

        TabPane mainTabPane = $(ws.root).get("mainTabPane");
        var tab = (Tab)cv.getValue();
        mainTabPane.getTabs().add(tab);
        mainTabPane.getSelectionModel().select(tab);
    }

}
