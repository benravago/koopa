package koopa.core.parsers;

import java.util.logging.Logger;

import koopa.core.util.IndentingLogger;

public class Trace extends IndentingLogger {

    public Trace() {
        super(Logger.getLogger("grammar"));
    }
}
