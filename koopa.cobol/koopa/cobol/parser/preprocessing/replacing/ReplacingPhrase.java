package koopa.cobol.parser.preprocessing.replacing;

import static koopa.core.data.tags.AreaTag.COMMENT;
import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;
import static koopa.core.data.tags.AreaTag.SKIPPED;
import static koopa.core.data.tags.SyntacticTag.END_OF_LINE;
import static koopa.core.data.tags.SyntacticTag.SEPARATOR;
import static koopa.core.data.tags.SyntacticTag.WHITESPACE;

import java.util.LinkedList;
import java.util.List;

import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.data.tags.AreaTag;
import koopa.core.sources.Source;
import koopa.core.trees.Tree;
import koopa.core.trees.Trees;

public abstract class ReplacingPhrase {

    public static enum Mode {
        MATCHING, LEADING, TRAILING;

        public static Mode from(Tree instruction) {
            if (Trees.getMatch(instruction, "leading") != null) {
                return LEADING;
            } else if (Trees.getMatch(instruction, "trailing") != null) {
                return TRAILING;
            } else {
                return MATCHING;
            }
        }
    }

    protected final ReplacingPhraseOperand replacing;
    protected final ReplacingPhraseOperand by;

    public ReplacingPhrase(ReplacingPhraseOperand replacing, ReplacingPhraseOperand by) {
        this.replacing = replacing;
        this.by = by;
    }

    public ReplacingPhraseOperand getReplacing() {
        return replacing;
    }

    public ReplacingPhraseOperand getBy() {
        return by;
    }

    public abstract boolean appliedTo(Source source, LinkedList<Data> newTokens);

    protected List<Token> nextTextWord(Source library, LinkedList<Token> seen) {
        skipToNonBlankProgramText(library, seen);
        return nonBlankProgramText(library, seen);
    }

    private List<Token> nonBlankProgramText(Source library, LinkedList<Token> seen) {
        List<Token> textWord = null;
        for (;;) {
            var data = library.next();
            if (data == null) {
                return textWord;
            }
            // If we find non-Tokens in the data stream we assume they're there for a good reason
            // and let them indicate the end of the text word.
            if (!(data instanceof Token)) {
                library.unshift(data);
                return textWord;
            }
            var t = (Token) data;
            if (!isProgramText(t)) {
                seen.addLast(t);
                continue;
            }
            if (isBlank(t)) {
                library.unshift(t);
                return textWord;
            }
            if (t.hasTag(SEPARATOR)) {
                if (textWord == null) {
                    // SEP while building a word => complete word and ignore SEP.
                    seen.addLast(t);
                    assert (textWord == null);
                    textWord = new LinkedList<>();
                    textWord.add(t);
                    return textWord;
                } else {
                    // SEP while not building a word => return SEP.
                    library.unshift(t);
                    return textWord;
                }
            } else {
                seen.addLast(t);
                if (textWord == null) {
                    textWord = new LinkedList<>();
                }
                textWord.add(t);
            }
        }
    }

    private void skipToNonBlankProgramText(Source library, LinkedList<Token> seen) {
        for (;;) {
            var d = library.next();
            if (d == null) {
                return;
            }
            if (!(d instanceof Token)) {
                library.unshift(d);
                return;
            }
            var t = (Token) d;
            if (isProgramText(t) && !isBlank(t)) {
                library.unshift(t);
                return;
            } else {
                seen.addLast(t);
            }
        }
    }

    private boolean isProgramText(Token t) {
        return t.hasAnyTag(PROGRAM_TEXT_AREA, SKIPPED) && !t.hasAnyTag(COMMENT);
    }

    private boolean isBlank(Token t) {
        return isProgramText(t) && t.hasAnyTag(COMMENT, END_OF_LINE, WHITESPACE);
    }

    protected synchronized void unshiftStack(Source library, LinkedList<Token> seen) {
        while (!seen.isEmpty()) {
            library.unshift(seen.removeLast());
        }
    }

    public static boolean isConsideredSingleSpace(Data tw) {
        if (!(tw instanceof Token)) {
            return false;
        }
        var textWord = (Token) tw;

        // "Comments, if any, are treated as a single space."
        if (textWord.hasTag(AreaTag.COMMENT)) {
            return true;
        }

        // "Each occurrence of a separator comma, semicolon, or space in pseudo-text-1
        //  or in the library text is considered to be a single space."
        var text = textWord.getText();
        return ",".equals(text) || ";".equals(text) || text.isBlank();
    }

    protected boolean isNewline(Token token) {
        var text = token.getText();
        return "\n".equals(text) || "\r\n".equals(text);
    }

    protected String text(List<Token> words) {
        if (words == null || words.isEmpty()) {
            return null;
        }
        var text = new StringBuilder();
        for (var token : words) {
            text.append(token.getText());
        }
        return text.toString();
    }

    @Override
    public String toString() {
        return replacing + " BY " + by;
    }

}
