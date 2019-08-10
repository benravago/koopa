package koopa.core.util;

import java.nio.charset.Charset;

import java.util.logging.Logger;
import static java.util.logging.Level.*;

public class Koopa {
    private Koopa() {}
    
    private static final Logger LOGGER = Logger.getLogger(Koopa.class.getName());
    
    private static final Charset charset;
    
    static {
        var encoding = System.getProperty("koopa.encoding");
        if (encoding == null) {
            charset = Charset.defaultCharset();
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer("Using default charset: " + charset + ".");
            }
        } else if (!Charset.isSupported(encoding)) {
            charset = Charset.defaultCharset();
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer("Encoding not supported: '" + encoding
                    + "'. Using default charset instead: " + charset + ".");
            }
        } else {
            charset = Charset.forName(encoding);
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.finer("Using specified charset: " + charset + ".");
            }
        }
    }

    public static Charset charset() {
        return charset;
    }

}
