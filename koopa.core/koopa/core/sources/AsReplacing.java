package koopa.core.sources;

import koopa.core.data.Data;
import koopa.core.data.Replaced;
import koopa.core.data.Token;

/**
 * Marks all {@linkplain Token}s as replacing something {@linkplain Replaced}.
 */
public class AsReplacing extends ChainingSource implements Source {

    private final Replaced replaced;

    public AsReplacing(Source source, Replaced replaced) {
        super(source);
        this.replaced = replaced;
    }

    @Override
    protected Data nextElement() {
        var d = source.next();
        if (d == null || !(d instanceof Token)) {
            return d;
        }
        var t = (Token) d;
        return t.asReplacing(replaced);
    }

}
