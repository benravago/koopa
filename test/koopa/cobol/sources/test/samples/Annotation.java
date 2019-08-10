package koopa.cobol.sources.test.samples;

import java.util.LinkedHashSet;
import java.util.Set;

class Annotation {

    final Set<String> required = new LinkedHashSet<>();
    final Set<String> forbidden = new LinkedHashSet<>();

    Annotation() {}

    Annotation(Annotation annotation) {
        required.addAll(annotation.required);
        forbidden.addAll(annotation.forbidden);
    }

    void add(String category, boolean required) {
        if (required) {
            this.required.add(category);
        } else {
            this.forbidden.add(category);
        }
    }

    Set<String> getRequired() {
        return required;
    }

    Set<String> getForbidden() {
        return forbidden;
    }

    void mergeWith(Annotation other) {
        // Offsets are not overwritten.
        required.addAll(other.required);
        forbidden.addAll(other.forbidden);
    }
}