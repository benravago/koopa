package koopa.app.components.outline;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Pair;

import koopa.app.Icons;
import koopa.core.trees.Tree;
import koopa.core.trees.TreeWalker;
import koopa.core.trees.Trees;

public class CobolOutline {

    final Tree ast;
    final TreeView view;

    public CobolOutline(Tree ast, TreeView view) {
        this.ast = ast;
        this.view = view;
    }

    public void run() {
        TreeItem root = new TreeItem("n/a");
        walk(ast,root);
        if (!root.isLeaf()) {
            root = (TreeItem) root.getChildren().get(0);
        }
        view.setRoot(root);
    }


    void walk(Tree tree, TreeItem parent) {
        var w = new TreeWalker(tree);
        w.next(); // Always skip the root.

        while ((tree = w.next()) != null) {

            if (tree.isNode("sourceUnit")) {
                w.skipRemainderOfTree(tree);
                var name = Trees.getAllText(tree,"programName");
                var ref = pair(tree,name);
                walk(tree,push(parent,ref,Icons.PROGRAM));
            }
            else if (tree.isNode("declaratives")) {
                w.skipRemainderOfTree(tree);
                var ref = pair(tree,"DECLARATIVES");
                walk(tree,push(parent,ref,Icons.DECLARATIVES));
            }
            else if (tree.isNode("declarativeSection") || tree.isNode("section")) {
                w.skipRemainderOfTree(tree);
                var name = Trees.getAllText(tree,"sectionName");
                var ref = pair(tree,name);
                walk(tree,push(parent,ref,Icons.SECTION));
            }
            else if (tree.isNode("paragraph")) {
                w.skipRemainderOfTree(tree);
                var name = Trees.getAllText(tree,"paragraphName");
                var ref = pair(tree,name);
                push(parent,ref,Icons.PARAGRAPH);
            }
        }
    }

    static TreeItem push(TreeItem parent, Pair<String,Tree> value, Icons icon) {
        var child = new TreeItem(value,icon.image());
        parent.getChildren().add(child);
        return child;
    }

    static Pair<String,Tree> pair(Tree value, String name) {
        return new Pair<>(name,value) {
            @Override public String toString() { return getKey(); }
        };
    }

}