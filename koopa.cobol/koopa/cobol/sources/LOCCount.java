package koopa.cobol.sources;

import static koopa.core.data.tags.AreaTag.COMMENT;
import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;
import static koopa.core.data.tags.AreaTag.SKIPPED;
import static koopa.core.data.tags.SyntacticTag.END_OF_LINE;

import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.sources.ChainingSource;
import koopa.core.sources.Source;

public class LOCCount extends ChainingSource implements Source {

    private final Source local; // TODO: is this needed?

    private int lines;
    private int code;
    private int comments;

    private boolean done = false;
    private boolean sawCode = false;
    private boolean sawComment = false;

    public LOCCount(Source source) {
        super(source);
        assert (source != null);
        this.local = source;
        this.lines = 0;
        this.code = 0;
        this.comments = 0;
    }

    @Override
    public Data nextElement() {
        if (done) {
            return null;
        }
        var d = local.next();
        if (d != null && !(d instanceof Token)) {
            return d;
        }
        var t = (Token) d;
        if (t == null || t.hasTag(END_OF_LINE)) {
            lines += 1;
            if (sawCode) {
                code += 1;
            }
            if (sawComment) {
                comments += 1;
            }
            sawCode = false;
            sawComment = false;
            done = (t == null);
        } else if (t.hasTag(SKIPPED)) {
            // no-op
        } else if (t.hasTag(COMMENT)) {
            sawComment = true;
        } else if (t.hasTag(PROGRAM_TEXT_AREA)) {
            sawCode = true;
        }
        return t;
    }

    @Override
    public void close() {
        local.close(); //
    }

    public int getNumberOfLines() {
        return lines;
    }

    public int getNumberOfLinesWithCode() {
        return code;
    }

    public int getNumberOfLinesWithComments() {
        return comments;
    }

    @Override
    public String toString() {
        return "LOC { lines: " + lines + ", code: " + code + ", comments: " + comments + " }";
    }

}
