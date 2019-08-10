package koopa.core.sources.test;

import java.util.LinkedList;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.core.data.Data;
import koopa.core.data.Token;
import koopa.core.data.tags.AreaTag;
import koopa.core.sources.ChainingSource;
import koopa.core.sources.Source;

/**
 * A {@linkplain Source} which can tell you whether or not it has reached an expected endpoint.
 * That point is either indicated by an explicit marker (see {@linkplain #MARKER_TEXT}, or just the end of regular input.
 */
public class TestTokenizer extends ChainingSource implements Source {

    // final boolean TRACE = false;
    final Logger LOGGER = Logger.getLogger("source.test");

    public static final String MARKER_TEXT = "^";

    Token marker;
    LinkedList<Data> dataSinceMarker = new LinkedList<>();

    public TestTokenizer(Source source) {
        super(source);
    }

    @Override
    protected Data nextElement() {
        var d = source.next();
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("%% " + d);
        }
        return d;
    }

    @Override
    public Data next() {
        var d = super.next();
        if (d == null) {
            return null;
        }
        if (d instanceof Token && MARKER_TEXT.equals(((Token) d).getText())) {
            marker = (Token) d;
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer("+> MARKER ");
            }
            d = super.next();
        }

        if (marker != null && d != null) {
            dataSinceMarker.add(d);
        }
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("> " + d);
        }
        return d;
    }

    @Override
    public void close() {
        source.close();
    }

    @Override
    public void unshift(Data token) {
        if (LOGGER.isLoggable(FINER)) {
            LOGGER.finer("<<< " + token);
        }
        super.unshift(token);
        if (marker != null) {
            var last = dataSinceMarker.removeLast();
            assert (token == last);
            if (dataSinceMarker.isEmpty()) {
                if (LOGGER.isLoggable(FINER)) {
                    LOGGER.finer("<- MARKER");
                }
                super.unshift(marker);
                marker = null;
            }
        }
    }

    public boolean isWhereExpected() {
        if (marker == null) {
            return isAtMarkerOrEndOfSource();
        } else {
            return isAtMarker();
        }
    }

    boolean isAtMarkerOrEndOfSource() {
        for (;;) {
            var d = next();
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer("@ " + (marker == null ? "" : "#") + d);
            }
            if (marker != null) {
                return true;
            }
            if (d == null) {
                return true;
            }
            // TODO Would prefer to fall back on KoopaGrammar.isProgramText/isSeparator.
            if (d instanceof Token) {
                var t = (Token)d;
                if (!t.hasTag(AreaTag.PROGRAM_TEXT_AREA)) {
                    continue;
                }
                var text = t.getText();
                if (!text.isBlank()) { // text.trim().length() > 0
                    return false;
                }
            }
        }
    }

    boolean isAtMarker() {
        for (var d : dataSinceMarker) {
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer("M@ " + d);
            }
            if (!(d instanceof Token)) {
                continue;
            }
            // TODO Would prefer to fall back on KoopaGrammar.isProgramText/isSeparator.
            var t = (Token) d;
            if (!t.hasTag(AreaTag.PROGRAM_TEXT_AREA)) {
                continue;
            }
            if (!t.getText().isBlank()) { // t.getText().trim().length() > 0
                return false;
            }
        }
        return true;
    }

}
