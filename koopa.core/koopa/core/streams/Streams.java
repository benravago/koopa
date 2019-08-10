package koopa.core.streams;

import koopa.core.data.Token;
import koopa.core.grammars.Grammar;
import koopa.core.parsers.Stream;
import static koopa.core.util.Iterables.each;

public final class Streams {
    private Streams() {}

    public static String getProgramTextFromBookmark(Grammar grammar, Stream stream) {
        var programText = new StringBuilder();
        for (var d : each(stream.fromBookmarkIterator())) {
            if (!(d instanceof Token) || !grammar.isProgramText(d)) {
                continue;
            }
            programText.append(((Token) d).getText());
        }
        return programText.toString();
    }
}
