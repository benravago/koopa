package koopa.app;

import java.nio.file.Path;

import javafx.scene.Parent;
import javafx.beans.property.ObjectProperty;

import koopa.core.data.Data;

public class WorkingSet {

    public Parent root;
    
    public Path cwd;
    public CobolParserFactory parserFactory;
    
    public ObjectProperty<Data> selected;

}

// import javafx.beans.property.ObjectProperty;
// import koopa.core.data.Data;
//     public ObjectProperty<Data> selected;

// import javafx.beans.property.SimpleObjectProperty;
//     ws.selected = new SimpleObjectProperty();



// https://github.com/dlsc-software-consulting-gmbh/PreferencesFX
// https://dlsc.com/2014/07/10/javafx-tip-5-be-observable/
// https://code.makery.ch/library/javafx-tutorial/
