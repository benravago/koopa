package koopa.cobol.parser.preprocessing.replacing;

import java.util.ArrayList;
import java.util.List;

import koopa.cobol.parser.preprocessing.replacing.ReplacingPhrase.Mode;
import koopa.core.trees.Tree;
import koopa.core.trees.Trees;

public final class Replacing {
    private Replacing() {}

    public static List<ReplacingPhrase> allPhrasesFrom(List<Tree> definitions) {
        var replacingPhrases = new ArrayList<ReplacingPhrase>(definitions.size());
        for (var replacement : definitions) {
            replacingPhrases.add(phraseFrom(replacement));
        }
        return replacingPhrases;
    }

    private static ReplacingPhrase phraseFrom(Tree definition) {
        var mode = Mode.from(definition);
        var replacing = ReplacingPhraseOperand.from(Trees.getMatch(definition,1,"replacementOperand"));
        var by = ReplacingPhraseOperand.from(Trees.getMatch(definition,2,"replacementOperand"));

        // "For purposes of matching, text-1, word-1, and literal-3 are treated as pseudo-text
        //  containing only text-1, word-1, or literal-3, respectively."

        // So we can match everything the same way we match pseudo-text.
        switch (mode) {
            case MATCHING: return new ReplaceMatching(replacing, by);
            case LEADING:  return new ReplaceLeading(replacing, by);
            case TRAILING: return new ReplaceTrailing(replacing, by);
            default:       return new ReplaceNone();
        }
    }

}
