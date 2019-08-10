package koopa.core.sources;

import koopa.core.data.Data;

public class NullSource extends BasicSource implements Source {

    @Override
    protected Data nextElement() {
        return null;
    }

    public void close() {}

}
