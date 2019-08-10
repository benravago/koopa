package koopa.templates.partials;

import java.util.LinkedList;
import java.util.List;

import koopa.templates.TemplateLogic;

public class Partial {

    private final List<Part> parts = new LinkedList<>();

    public void add(Part part) {
        parts.add(part);
    }

    public void applyTo(StringBuilder builder, String indent, TemplateLogic logic) {
        for (var part : parts) {
            part.applyTo(builder, indent, logic);
        }
    }

}
