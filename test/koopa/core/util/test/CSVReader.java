package koopa.core.util.test;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader implements AutoCloseable{

    BufferedReader in;

    public CSVReader(Reader in) {
        this.in = (in instanceof BufferedReader) ? (BufferedReader)in : new BufferedReader(in);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    String line;
    int row=0, col=0, end=0;
    List<String> list = new ArrayList<>();
    StringBuilder buf = new StringBuilder();

    public String[] readNext() throws IOException {
           line = in.readLine();
           if (line != null) {
               try {
                   return split();
            }
               catch (Exception e) {
                   System.out.println(e.toString()+" at "+row+'/'+col);
               }
        }
           return null;
    }

    String[] split() {
        list.clear();
        row += 1;
        col = 0;
        end = line.length();
        while (more()) {
            quote();
            buf.setLength(0);
            char c;
            while ((c = get()) != '"') {
                if (c != '\\') {
                    buf.append(c);
                } else {
                    switch (get()) {
                        case 'b':  buf.append('\b'); break;
                        case 'n':  buf.append('\n'); break;
                        case 't':  buf.append('\t'); break;
                        case 'r':  buf.append('\r'); break;
                        case 'f':  buf.append('\f'); break;
                        case '"':  buf.append('"');  break;
                        case '\\': buf.append('\\'); break;
                        default: throw new RuntimeException("bad escape at "+row+'/'+col);
                    }
                }
            }
            list.add(buf.toString());
        }
        return list.toArray(new String[list.size()]);
    }

    boolean more() {
        if (col < end) {
            if (col < 1 || get() == ',') return true;
            throw new RuntimeException("short line at "+row+'/'+col);
        }
        return false;
    }

    void quote() {
        if (get() != '"') throw new RuntimeException("not a \" at "+row+'/'+col);
    }

    char get() {
        return line.charAt(col++);
    }
}
