package koopa.templates;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class TemplateLogic {

    private final Map<String, String> values = new HashMap<>();

    public void setValue(String name, String value) {
        values.put(name, value);
    }

    public String getValue(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        } else {
            throw new NoSuchElementException("Missing value for '" + name + "'.");
        }
    }

    public void call(String target, StringBuilder builder, String indent) {
        throw new InternalError("Don't know how to handle call to '" + target + "'");
    }
}
