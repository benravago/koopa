package koopa.app.cli;

import java.util.ArrayList;
import java.util.List;

import koopa.cobol.sources.SourceFormat;

public class CommandLineOptions {

    private SourceFormat format = SourceFormat.FIXED;
    private boolean preprocess = false;

    private final List<String> copybookPaths = new ArrayList<>();
    private final List<String> other = new ArrayList<>();

    public CommandLineOptions(String[] args) {
        for (var i = 0; i < args.length; i++) {
            var option = args[i];
            if (option.startsWith("--")) {
                if (option.equals("--free-format")) {
                    format = SourceFormat.FREE;
                } else if (option.equals("--variable-format")) {
                    format = SourceFormat.VARIABLE;
                } else if (option.equals("--preprocess")) {
                    preprocess = true;
                } else {
                    throw new IllegalArgumentException("Unknown option: " + option);
                }
            } else if (option.startsWith("-")) {
                if (option.equals("-I")) {
                    i += 1;
                    if (i >= args.length) {
                        throw new IllegalArgumentException("Missing copybook path definition.");
                    }
                    copybookPaths.add(args[i]);
                } else {
                    throw new IllegalArgumentException("Unknown option: " + option);
                }
            } else {
                other.add(option);
            }
        }
        if (getOther().size() != 2) {
            System.err.println(usage());
            System.exit(1);
            return;
        }
    }

    public SourceFormat getFormat() {
        return format;
    }

    public boolean isPreprocess() {
        return preprocess;
    }

    public List<String> getCopybookPaths() {
        return copybookPaths;
    }

    public List<String> getOther() {
        return other;
    }

    public String usage() {
        return "Usage: [--free-format | --variable-format] [--preprocess -I <copybookpath>] <source> <target>";
    }

}
