package koopa.app.components.detail;

import koopa.app.components.outline.CobolOutline;
import koopa.app.components.overview.Overview;
import koopa.app.components.sourceview.SourceView;
import koopa.app.components.outline.CobolTree;

import koopa.core.trees.Tree;
import koopa.core.data.Token;

import javafx.event.Event;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Pair;

import fx.mvc.View;
import static fx.mvc.util.Lookup.$;

@View(value="koopa.app.components.detail.DetailFrame",
      nodeType="javafx.scene.control.Tab")
public class DetailController {

    final Detail detail;
    Tree AST;

    Text text;
    Font font;
    double ascent, descent, lineHeight, charWidth;

    ScrollPane scroll;
    Rectangle box;

    public DetailController(Detail detail) {
        this.detail = detail;
        this.AST = detail.results.getTree();
    }

    void tabOpen(Tab tab) {
        tab.setId(detail.id);
        tab.setText(detail.file);
        Overview.addParseResult(detail.id,detail.results);
    }

    void tabClosed(Event e) {
        var tab = (Tab) e.getSource();
        Overview.removeParseResult(tab.getId());
    }

    void tabSelection(Event e) {
        var tab = (Tab) e.getSource();
        System.out.println("onSelectionChanged "+tab);
    }

    void onLoad(Event e) {
        System.out.println("onLoad "+System.currentTimeMillis());
        var tab = (Tab) e.getSource();
        var c = $(tab);

        box  = c.get("cobolWord");
        text = c.get("cobolText");
        font = Font.font("Monospaced",14);
        getFontMetrics();

        scroll = c.get("cobolPane");
        fillTree(c.get("cobolTree"));
        fillText();

        tabOpen(tab);
    }

    void getFontMetrics() {
        text.setText(" ");
        text.setFont(font);
        var layoutBounds = text.getLayoutBounds();
        charWidth = layoutBounds.getWidth();
        lineHeight= layoutBounds.getHeight();
        ascent = -layoutBounds.getMinY();
        descent = layoutBounds.getMaxY();
    }

    void fillText() {
        text.setText(detail.source);
        text.addEventHandler(SourceView.SOURCE_REF,this::showText);
        text.addEventHandler(SourceView.SOURCE_AST,this::showAST);
   }

    void showText(SourceView e) {
        Tree v = e.reference();
        if (v.isToken()) {
            Token t = v.getData();
            var start = t.getStart();
            var line = start.getLinenumber();
            var off = start.getPositionInLine();
            var len = t.getLength();
            moveBox(line-1,off-1,len);
        } else {
            var line = v.getLine();
            moveBox(line-1,7,65);
        }
        showBox();
    }

    void moveBox(int line, int off, int len) {
        box.setY( ( line * lineHeight ) - ascent );
        box.setHeight( lineHeight );
        box.setX( off * charWidth );
        box.setWidth( len * charWidth );
    }

    void showBox() {
        var y = box.getY();
        var h = text.getBoundsInLocal();
        var z = y < lineHeight ? 0
              : (y + lineHeight) / h.getMaxY();
        scroll.setVvalue(z);
    }

    void showAST(SourceView e) {
        e.consume(); // to prevent a dispatch loop; otherwise it will bubble up back to TabPane
        new CobolTree(e.getTarget(),AST).run();
    }

    //  box.setY(-ascent);
    //  box.setHeight(lineHeight);
    //  box.setX(0);
    //  box.setWidth(charWidth);

    void textSelection(MouseEvent e) {

        var eY = e.getY() + ascent;
        var eX = e.getX();

        var y = (int)(eY/lineHeight);
        var x = (int)(eX/charWidth);

        var Y = (y*lineHeight) - ascent;
        var Height = lineHeight;
        var X = x*charWidth;
        var Width = charWidth*6;

        box.setY(Y);
        box.setHeight(Height);
        box.setX(X);
        box.setWidth(Width);

    }

    void print(double ... a) {
        var b = new StringBuilder();
        for (var d:a) b.append(", ").append(d);
        System.out.println(
            "ascent,descent,lineHeight,charWidth,eX,eY,y,x,Y,Height,X,Width\n"+
            b.substring(1));
    }

    void fillTree(TreeView view) {
        new CobolOutline(AST,view).run();
    }

    void treeSelection(MouseEvent e) {
        if (e.getTarget() instanceof Text) {
            var view = (TreeView<Pair<String,Tree>>) e.getSource();
            var item = view.getSelectionModel().getSelectedItem();
            var pair = item.getValue();
            var data = pair.getValue();
            SourceView.sendReference(text,data);
        }
    }

}
