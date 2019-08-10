package koopa.stage.source;

import static koopa.core.data.tags.AreaTag.PROGRAM_TEXT_AREA;

import java.io.Reader;

import koopa.core.sources.BashStyleComments;
import koopa.core.sources.LineSplitter;
import koopa.core.sources.Source;
import koopa.core.sources.TagAll;
import koopa.core.sources.TokenSeparator;

public class StageTokens {

    public static Source getNewSource(String resourceName, Reader reader) {
        // Split lines...
        var lineSplitter = new LineSplitter(resourceName, reader);
        // And mark everything as program text...
        var tagAllAsProgramText = new TagAll(lineSplitter, PROGRAM_TEXT_AREA);
        // Separate the tokens...
        var tokenSeparator = new TokenSeparator(tagAllAsProgramText);
        // And mark comments...
        return new BashStyleComments(tokenSeparator);
    }

}
