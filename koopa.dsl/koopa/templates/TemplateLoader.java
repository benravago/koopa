package koopa.templates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateLoader {
    private TemplateLoader() {}

    public static Template fromResource(Class<?> clazz, String resourceName) {
        try ( var reader = new BufferedReader(new InputStreamReader(
                clazz.getResourceAsStream(resourceName) )) )
        {
            return from(reader);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static enum State {
        SCANNING, IN_DEFINITION
    }

    private static final String INDENT = "    ";
    private static final String COMMENT_CHARACTER = "#";
    private static final String MARKER_CHARACTER = "#";
    private static final Pattern DEF = Pattern.compile("^def\\s+(\\w+)\\s*$");
    private static final Pattern CALL = Pattern.compile("^>   (\\s*)(\\w+)\\s*$");
    private static final Pattern END = Pattern.compile("^end\\s*$");

    public static Template from(BufferedReader reader) throws IOException {
        var template = new Template();

        var state = State.SCANNING;
        for (;;) {
            var line = reader.readLine();
            Matcher matcher = null;

            // End-of-file ?
            if (line == null) {
                if (state != State.SCANNING) {
                    throw new IllegalStateException("Unexpected end of template.");
                }
                return template;
            }

            // Comment ?
            if (line.startsWith(COMMENT_CHARACTER)) {
                continue;
            }

            switch (state) {

                case SCANNING:
                    // Empty lines ? Ignored.
                    if (line.trim().length() == 0) {
                        continue;
                    }
                    // Start of a new definition ?
                    matcher = DEF.matcher(line);
                    if (matcher.matches()) {
                        template.definePartial(matcher.group(1));
                        state = State.IN_DEFINITION;
                        continue;
                    }
                    // Something unexpected then...
                    throw new IllegalStateException("Unexpected: " + line);

                case IN_DEFINITION:
                    // Indented ? Something to be output.
                    if (line.startsWith(INDENT)) {
                        var text = line.substring(INDENT.length());
                        if (text.indexOf(MARKER_CHARACTER) < 0) {
                            template.addVerbatimLine(text);
                        } else {
                            template.addLineWithReplacements(text.split(MARKER_CHARACTER));
                        }
                        continue;
                    }
                    // A call to something ?
                    matcher = CALL.matcher(line);
                    if (matcher.matches()) {
                        var indent = matcher.group(1);
                        var target = matcher.group(2);
                        template.addCall(indent, target);
                        state = State.IN_DEFINITION;
                        continue;
                    }
                    // End of the current definition ?
                    matcher = END.matcher(line);
                    if (matcher.matches()) {
                        template.end();
                        state = State.SCANNING;
                        continue;
                    }
                    // Something unexpected then...
                    throw new IllegalStateException("Unexpected: " + line);

                default:
                    // Bad developer. Bad! ;-)
                    throw new IllegalStateException("Unexpected parse state: " + state);
            }
        }
    }

}
