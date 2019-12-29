package koopa.app.components.overview;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.util.Pair;

import koopa.app.batchit.BatchResults;
import koopa.cobol.parser.ParseResults;
import koopa.core.data.Data;
import koopa.core.trees.Tree;

public class Overview {

    private Overview() {}

    private static class Singleton {
        static final Overview INSTANCE = new Overview();
    }

    public static Overview getInstance() {
        return Singleton.INSTANCE;
    }

    public final ObservableList<BatchResults> results =
        FXCollections.observableArrayList();

    public final ObjectProperty<Pair<Data,Tree>> current =
        new SimpleObjectProperty();

    public void bind(TableView<BatchResults> table) {
        table.setItems(results);
        table.getColumns().forEach(BatchResults::setCellValueFactory);
    }

    public static void addParseResult(String key, ParseResults parseResults) {
        getInstance().results.add(new BatchResults(key,parseResults)); // .copy()
    }

    public static void removeParseResult(String key) {
        getInstance().results.removeIf(r -> r.getKey().equals(key)); // .copy()
    }

    public static Pair<Data,Tree> getTree() {
        return getInstance().current.getValue();
    }

    public static void setTree(Data data, Tree tree) {
        getInstance().current.setValue(new Pair(data,tree));
    }

}