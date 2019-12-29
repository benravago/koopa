package koopa.app.components.sourceview;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

public class SourceView extends Event {

    public static final EventType<SourceView> ANY =
            new EventType<SourceView>(Event.ANY, "SOURCE");

    public static final EventType<SourceView> SOURCE_REF =
        new EventType<SourceView>(SourceView.ANY,"SOURCE_REF");

    public static final EventType<SourceView> SOURCE_AST =
        new EventType<SourceView>(SourceView.ANY,"SOURCE_AST");

    private final Object reference;

    public <T> T reference() {
        return (T) reference;
    }

    public SourceView(Object source) {
        this(source,ANY);
    }

    public SourceView(Object source, EventType<? extends SourceView> eventType) {
        super(eventType);
        reference = source;
    }

    public static void sendReference(EventTarget target, Object source) {
        Event.fireEvent(target, new SourceView(source,SourceView.SOURCE_REF));
    }

    public static void sendASTRequest(EventTarget target) {
        Event.fireEvent(target, new SourceView(null,SourceView.SOURCE_AST));
    }

}
