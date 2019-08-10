package koopa.core.targets;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import koopa.core.data.Data;
import koopa.core.data.Token;

public class TokenTracker implements Target {

    private final List<Token> tokens = new ArrayList<>();

    @Override
    public void push(Data data) {
        if (data instanceof Token) {
            tokens.add((Token) data);
        }
    }

    @Override
    public void done() {}

    public List<Token> getTokens() {
        return tokens;
    }

    // TODO This logic breaks when applying preprocessing !
    public Token getTokenAt(int position) {
        var size = tokens.size();
        if (size == 0) {
            return null;
        }
        var left = 0;
        var right = size - 1;
        for (;;) {
            if (left >= right) {
                break;
            }
            var mid = (left + right) >> 1;
            var t = tokens.get(mid);
            if (position < t.getStart().getPositionInFile()) {
                right = mid - 1;
                continue;
            }
            if (position > t.getEnd().getPositionInFile()) {
                left = mid + 1;
                continue;
            }
            return t;
        }
        if (left > right) {
            return null;
        }
        var t = tokens.get(left);
        if (position >= t.getStart().getPositionInFile() &&
            position <= t.getEnd().getPositionInFile())
        {
            return t;
        }
        return null;
    }

    public int getTokenCount() {
        return tokens.size();
    }

    public List<Token> getTokensAfter(Token last) {
        var additionalTokens = new LinkedList<Token>();
        var foundLast = false;
        for (var token : tokens) {
            if (foundLast) {
                additionalTokens.add(token);
            } else if (token == last) {
                foundLast = true;
            }
        }
        return additionalTokens;
    }

}
