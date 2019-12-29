package koopa.app.batchit;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;

import koopa.cobol.parser.ParseResults;

public class BatchResults {

    String key;
    ParseResults results;

    public BatchResults(String key, ParseResults results) {
        this.key = key;
        this.results = results;
    }
    
    public String getKey() { return key; }

    public static void setCellValueFactory(TableColumn c) {
        Callback<CellDataFeatures<BatchResults,String>, ObservableValue<String>> cb = null;
        switch (c.getText()) {
            case "Status" : {
                cb = p -> wrap(p.getValue().getStatus());
                break;
            }
            case "Errors": {
                cb = p -> wrap(p.getValue().results
                                .getParse().getMessages().getErrorCount());
                break;
            }
            case "Warnings": {
                cb = p -> wrap(p.getValue().results
                                .getParse().getMessages().getWarningCount());
                break;
            } 
            case "Tokens": {
                cb = p -> wrap(p.getValue().getTokens());
                break;
            }
            case "Coverage": {
                cb = p -> wrap(p.getValue().getCoverage());
                break;
            }
            case "LOC": {
                cb = p -> wrap(p.getValue().results.getNumberOfLines());
                break;
            }
            case "SLOC": {
                cb = p -> wrap(p.getValue().results.getNumberOfLinesWithCode());
                break;
            }
            case "CLOC": {
                cb = p -> wrap(p.getValue().results.getNumberOfLinesWithComments());
                break;
            }
            case "File" : {
                cb = p -> wrap(p.getValue().results.getFile().getFileName().toString());
                break;
            }
            case "Path" : {
                cb = p -> wrap(p.getValue().results.getFile().toString());
                break;
            }
            case "Time (ms)": {
                cb = p -> wrap(p.getValue().results.getTime());
                break;
            }
        }
        c.setCellValueFactory(cb);
    }

    static ObservableValue<String> wrap(long n) {
        return new SimpleStringProperty(String.valueOf(n));
    }
    
    static ObservableValue<String> wrap(int n) {
        return new SimpleStringProperty(String.valueOf(n));
    }
    
    static ObservableValue<String> wrap(String s) {
        return new SimpleStringProperty(s);
    }
    
    String getStatus()   { return "status"; }
    String getTokens()   { return "tokens"; }
    String getCoverage() { return "coverage"; }

    // TODO:
    //   see BatchResults.txt for other columns
    //   add onHover() to Path column to show full Path
    //   add onSelect() to show corresponding Tab
}
