package koopa.app.components.outline;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import koopa.core.trees.Tree;

class ASTtoTreeModelAdapter {

    static Image DATA = new Image("/koopa/app/resources/splashy/diamonds_1.png");

    static int kind(Tree tree) {
        if (tree.isToken()) {
            return 0;
        } else {
            return 1;
        }
    }

    static TreeItem<Tree> wrap(Tree tree) {
        tree.setKind(kind(tree));
        return new TreeItem<Tree>(tree) {
            @Override
            public boolean isLeaf() {
                return !getValue().hasChildren();
            }
            @Override
            public ObservableList<TreeItem<Tree>> getChildren() {
                if (firstGetChildren) {
                    firstGetChildren = false;
                    var parent = getValue();
                    if (parent.hasChildren()) {
                        super.getChildren().setAll(items(parent));
                    } else {
                        super.getChildren().clear();
                    }
                }
                return super.getChildren();
            }
            boolean firstGetChildren = true;
        };
    }

    @SuppressWarnings("unchecked")
    static TreeItem<Tree>[] items(Tree tree) {
        var n = tree.getChildCount();
        var list = new TreeItem[n];
        for (var i = 0; i < n; i++) {
            list[i] = wrap(tree.getChild(i));
        }
        return list;
    }

    static TreeCell<Tree> makeCell(TreeView<Tree> view) {
        return new TreeCell<>() {
            @Override
            public void updateItem(Tree item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    var kind = item.getKind();
                    if (kind == 0) {
                        setText(tokenValue(item));
                        setGraphic(null);
                    } else {
                        setText(String.valueOf(item));
                        setGraphic(new ImageView(DATA));
                    }
                }

            }
        };
    }

    static String tokenValue(Tree item) {
        var text = item.getText();
        if (text.isBlank()) {
            return "''*"+text.length();
        } else {
            return "'"+text+"'";
        }
    }

}
