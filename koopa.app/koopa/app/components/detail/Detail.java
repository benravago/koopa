package koopa.app.components.detail;

import fx.mvc.Controller;

import koopa.cobol.parser.ParseResults;

@Controller("koopa.app.components.detail.DetailController")
public class Detail {

    public final String id;
    public final String dir;
    public final String file;
    public final String source;
    public final ParseResults results;

    public Detail(String id, String dir, String file, String source, ParseResults results) {
        this.id = id;
        this.dir = dir;
        this.file = file;
        this.source = source;
        this.results = results;
    }
}
