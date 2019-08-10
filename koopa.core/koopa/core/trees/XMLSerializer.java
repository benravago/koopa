package koopa.core.trees;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import koopa.core.data.Token;
import koopa.core.data.markers.Start;
import koopa.core.data.tags.AreaTag;

public class XMLSerializer {

    private static final boolean INCLUDE_POSITIONING =
        Boolean.getBoolean("koopa.xml.include_positioning");

    public static String serialize(Tree tree) throws IOException {
        StringWriter writer = new StringWriter();
        serialize(tree, writer);
        return writer.toString();
    }

    // UTF-8 is the default encoding for documents without prolog
    // See http://www.w3.org/TR/REC-xml/#charencoding
    // So it's necessary to add the XML declaration to ensure readability (if UTF-8 isn't used)
    // Usually ISO-8859-1, Windows-1252, and ASCII is also recognized
    // FileWriter uses default encoding, which might not be appropriate

    public static void serialize(Tree tree, Writer writer) throws IOException {
        // XML prolog/declaration
        writer.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        writer.append("<koopa>\n");
        if (tree != null) {
            walk(writer, tree, "  ");
        }
        writer.append("</koopa>\n");
        writer.flush();
        writer.close();
    }

    private static void walk(Writer writer, Tree tree, String dent) throws IOException {
        var data = tree.getData();
        if (data instanceof Start) {
            var start = (Start) data;
            if (tree.getChildCount() == 0) {
                writer.append(dent + "<" + start.getName());
                writePositions(writer, tree);
                writer.append(" />\n");
            } else {
                writer.append(dent + "<" + start.getName());
                writePositions(writer, tree);
                writer.append(">\n");
                for (var child : tree.getChildren()) {
                    walk(writer, child, dent + "  ");
                }
                writer.append(dent + "</" + start.getName() + ">\n");
            }
        } else if (data instanceof Token) {
            var token = (Token) data;
            if (token.hasTag(AreaTag.COMMENT)) {
                // TODO Should escape stuff where necessary.
                // According to http://www.w3.org/TR/REC-xml/#dt-comment -- is
                // not allowed Reading comments with double-hyphen will fail in
                // compliant XML parsers. For convenience '--' is replaced by
                // '-_'. Otherwise, we'd have to throw an error.
                writer.append(dent
                    + "<!-- "
                    + tree.getText().replaceAll("--", "-_")
                    + " -->\n" );
            } else {
                // TODO Should escape stuff where necessary.
                // A command like DISPLAY ']]>' would generate invalid XML
                // without substitution
                writer.append(dent
                    + "<t><![CDATA["
                    + tree.getText().replaceAll("]]>", "]]]]><![CDATA[>")
                    + "]]></t>\n" );
            }
        }
    }

    private static void writePositions(Writer writer, Tree tree) throws IOException {
        if (!INCLUDE_POSITIONING) {
            return;
        }
        var start = tree.getStartPosition();
        var end = tree.getEndPosition();
        if (start != null) {
            writer.append(" from=\"" + start.getPositionInFile() + "\"");
            writer.append(" from-line=\"" + start.getLinenumber() + "\"");
            writer.append(" from-column=\"" + start.getPositionInLine() + "\"");
        }
        if (end != null) {
            writer.append(" to=\"" + end.getPositionInFile() + "\"");
            writer.append(" to-line=\"" + end.getLinenumber() + "\"");
            writer.append(" to-column=\"" + end.getPositionInLine() + "\"");
        }
    }

}
