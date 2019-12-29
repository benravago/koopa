package koopa.app.components.tabs;

import java.nio.file.Paths;

import javafx.event.Event;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleObjectProperty;

import koopa.app.WorkingSet;
import koopa.app.CobolParserFactory;
import koopa.app.components.overview.Overview;
import koopa.app.components.sourceview.SourceView;

import static fx.mvc.util.Lookup.$;

public class TabSettings {

    final WorkingSet ws;

    public TabSettings(WorkingSet ws) {
        this.ws = ws;
    }

    public void run() {

        ws.cwd = Paths.get(".");

        ws.parserFactory = new CobolParserFactory(null);
        ws.parserFactory.setKeepingTrackOfTokens(true);
        ws.parserFactory.setBuildTrees(true);

        ws.selected = new SimpleObjectProperty();

        var root = $(ws.root);

        TableView table = root.get("overviewTable");
        Overview.getInstance().bind(table);

        TabPane tabPane = root.get("mainTabPane");
        tabPane.addEventHandler(SourceView.SOURCE_AST, e -> {
            var pane = (TabPane)e.getTarget();
            var item = pane.getSelectionModel().getSelectedItem();
            if (item != null) {
                var tab = item.getContent();
                var text = tab.lookup("#cobolText");
                if (text != null) {
                    Event.fireEvent(text,e); // tab control should consume() event
                }
            }
        });
    }

}
