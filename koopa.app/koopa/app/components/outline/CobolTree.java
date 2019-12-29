package koopa.app.components.outline;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeCell;
import javafx.scene.text.Text;

import koopa.core.trees.Tree;
import koopa.app.components.sourceview.SourceView;

import fx.mvc.View;
import fx.mvc.util.Events;
import javafx.stage.Modality;

@View("koopa.app.components.outline.TreeFrame")
public class CobolTree {

    Node target;
    Tree ast;

    TreeView<Tree> tree;

    public CobolTree(EventTarget target, Tree ast) {
        this.target = (Node)target;
        this.ast = ast;
    }

    public void run() {
        Events.openDialog(this);
    }

    public void onLoad(Event e) {
        System.out.println("onLoad "+e);

        tree = (TreeView) e.getSource();

        var root = ASTtoTreeModelAdapter.wrap(ast);
        root.setExpanded(true);
        tree.setRoot(root);
        tree.setCellFactory(ASTtoTreeModelAdapter::makeCell);
    }

    void onShowing(DialogEvent e) {
        System.out.println("onShowing "+e);
        var dialog = (Dialog) e.getTarget();
        dialog.setTitle("AST Tree");
        dialog.setHeaderText("AST Path");
        dialog.initModality(Modality.NONE);
        dialog.initOwner​(target.getScene().getWindow());
    }

    void close(ActionEvent e) {
        System.out.println("close");
    }

    void treeSelected(MouseEvent e) {
        var t = e.getTarget();
        if (!sendReference(t)) {
            if (t instanceof Text) {
                sendReference(((Text)t).getParent());
            }
        }
    }

    boolean sendReference(Object cell) {
        if (cell instanceof TreeCell) {
            var item  = ((TreeCell)cell).getItem();
            if (item instanceof Tree) {
                SourceView.sendReference(target,item);
                return true;
            }
        }
        return false;
    }

}
