package koopa.app;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public enum Icons {

    PROGRAM("/koopa/app/resources/splashy/document_letter_marked.png"),
    DECLARATIVES("/koopa/app/resources/splashy/document_letter_new.png"),
    SECTION("/koopa/app/resources/splashy/document_copy.png"),
    PARAGRAPH("/koopa/app/resources/splashy/document_small.png"),
    STATEMENT("/koopa/app/resources/splashy/comment.png"),
    TOKEN("/koopa/app/resources/splashy/diamonds_1.png");

    Icons(String url) {
        icon = new Image(url);
    }

    private final Image icon;

    public Image icon() {
        return icon;
    }

    public ImageView image() {
        return new ImageView(icon);
    }

}
