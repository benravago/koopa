package koopa.app.actions;

import javafx.scene.control.TabPane;

import koopa.app.WorkingSet;
import koopa.app.components.sourceview.SourceView;

import static fx.mvc.util.Lookup.*;

public class ShowASTAction {

    final WorkingSet ws;

    public ShowASTAction(WorkingSet ws) {
        this.ws = ws;
    }

    public void run() {
        TabPane pane = $(ws.root).get("mainTabPane");
        SourceView.sendASTRequest(pane);
    }

}
