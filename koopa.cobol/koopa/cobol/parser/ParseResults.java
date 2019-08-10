package koopa.cobol.parser;

import java.nio.file.Path;

import koopa.core.parsers.Parse;

public class ParseResults {

    private final Path file;
    private Parse parse = null;

    private boolean validInput = false;

    private int numberOfLines = -1;
    private int numberOfLinesWithCode = -1;
    private int numberOfLinesWithComments = -1;
    private long time = -1;

    public ParseResults(Path file) {
        this.file = file;
        this.validInput = false;
    }

    public void setValidInput(boolean validity) {
        this.validInput = validity;
    }

    public Path getFile() {
        return this.file;
    }

    public boolean isValidInput() {
        return this.validInput;
    }

    public ParseResults copy() {
        var copy = new ParseResults(file);
        copy.validInput = validInput;
        copy.parse = parse;
        copy.numberOfLines = numberOfLines;
        copy.numberOfLinesWithCode = numberOfLinesWithCode;
        copy.numberOfLinesWithComments = numberOfLinesWithComments;
        copy.time = time;
        return copy;
    }

    public void setNumberOfLines(int numberOfLines) {
        this.numberOfLines = numberOfLines;
    }

    public int getNumberOfLines() {
        return numberOfLines;
    }

    public void setNumberOfLinesWithCode(int numberOfLinesWithCode) {
        this.numberOfLinesWithCode = numberOfLinesWithCode;
    }

    public int getNumberOfLinesWithCode() {
        return numberOfLinesWithCode;
    }

    public void setNumberOfLinesWithComments(int numberOfLinesWithComments) {
        this.numberOfLinesWithComments = numberOfLinesWithComments;
    }

    public int getNumberOfLinesWithComments() {
        return numberOfLinesWithComments;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setParse(Parse parse) {
        this.parse = parse;
    }

    public Parse getParse() {
        return parse;
    }

}
