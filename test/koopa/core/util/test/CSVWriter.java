package koopa.core.util.test;

import java.io.Writer;
import java.io.IOException;

public class CSVWriter implements AutoCloseable {

    Writer out;

    public CSVWriter(Writer out) {
        this.out = out;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public void writeNext(String[] line) throws IOException {
        var n = line.length - 1;
        var i = 0;
        while (i < n) {
            append(line[i++],',');
        }
        append(line[i],'\n');
    }

    void append(String var, char delim) throws IOException {
        out.append('"');
        for (var i = 0; i < var.length(); i++) {
            var c = var.charAt(i);
            switch (c) {
                case '\b': out.append("\\b");  break;
                case '\n': out.append("\\n");  break;
                case '\t': out.append("\\t");  break;
                case '\r': out.append("\\r");  break;
                case '\f': out.append("\\f");  break;
                case '\"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                default:   out.append(c);      break;
            }
        }
        out.append('"');
        out.append(delim);
    }

    public void flush() throws IOException {
        out.flush();
    }

}
