package koopa.core.parsers;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

import koopa.core.grammars.Grammar;
import koopa.core.grammars.combinators.Dispatched;
import koopa.core.parsers.combinators.Choice;

public class Optimizer {

    private static final Logger LOGGER = Logger.getLogger("optimization");

    private static final boolean SHOULD_RUN;

    static {
        SHOULD_RUN = !"false".equalsIgnoreCase(System.getProperty("koopa.optimize", "true"));

        if (!SHOULD_RUN && LOGGER.isLoggable(INFO)) {
            LOGGER.info("Optimizer has been turned off.");
        }
    }

    public static boolean shouldRun() {
        return SHOULD_RUN;
    }

    /**
     * Starting with the first parser, count the number of parsers
     * which we could use lookahead (with {@linkplain Grammar#keyword()}) for.
     * Start counting as soon as we see one which does not support lookahead.
     */
    public static int countLeadingParsersAllowingLookahead(ParserCombinator... parsers) {
        var count = 0;
        for (var p : parsers) {
            if (p.allowsLookahead()) {
                count += 1;
            } else {
                return count;
            }
        }
        return count;
    }

    /**
     * Build a dispatch table for the given parsers, mapping keywords to (a choice) of possible parsers.
     * <p>
     * The dispatch table's keys and choices respect the order in which
     * the  grammar writer has defined the different choices in the grammar.
     * This is an essential feature as Koopa grammars rely on manual ordering to resolve conflicts.
     */
    private static Map<String, ParserCombinator> dispatchTable(ParserCombinator... parsers) {
        var mapping = new LinkedHashMap<String, List<ParserCombinator>>();
        for (var p : parsers) {
            var keywords = new LinkedHashSet<String>();
            p.addAllLeadingKeywordsTo(keywords);
            for (var kw : keywords) {
                if (!mapping.containsKey(kw)) {
                    mapping.put(kw, new LinkedList<ParserCombinator>());
                }
                mapping.get(kw).add(p);
            }
        }
        var dispatchTable = new LinkedHashMap<String, ParserCombinator>();
        for (var kw : mapping.keySet()) {
            var choices = mapping.get(kw);
            if (choices.size() == 1) {
                dispatchTable.put(kw, choices.get(0));
            } else {
                dispatchTable.put(kw, new Choice(choices.toArray(new ParserCombinator[choices.size()])));
            }
        }
        return dispatchTable;
    }

    /**
     * Build a dispatch table for all parsers, and return a {@linkplain Dispatched} parser based on it.
     */
    public static Dispatched dispatched(Grammar grammar, ParserCombinator[] parsers) {
        return new Dispatched(grammar, dispatchTable(parsers));
    }

    /**
     * Build a dispatch table for the selected parsers, and return a {@linkplain Dispatched} parser based on it.
     */
    public static Dispatched dispatched(Grammar grammar, ParserCombinator[] parsers, int start, int length) {
        var selected = new ParserCombinator[length];
        for (var i = 0; i < length; i++) {
            selected[i] = parsers[start + i];
        }
        return new Dispatched(grammar, dispatchTable(selected));
    }

}
