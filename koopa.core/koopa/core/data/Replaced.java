package koopa.core.data;

public class Replaced {

    private final Replaced context;
    private final Position start;
    private final Position end;

    public Replaced(Position start, Position end, Replaced context) {
        this.context = context;
        this.start = start;
        this.end = end;
    }

    public Replaced getContext() {
        return context;
    }

    public Position getStart() {
        return start;
    }

    public Position getEnd() {
        return end;
    }

}
