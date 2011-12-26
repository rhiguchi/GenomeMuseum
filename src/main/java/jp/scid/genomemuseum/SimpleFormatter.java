package jp.scid.genomemuseum;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SimpleFormatter extends Formatter {
    private final static String lineSep = System.getProperty("line.separator");
    
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        String msg = formatMessage(record);
        
        sb.append("[").append(record.getLevel()).append("] ");
        sb.append(msg).append(lineSep);
        
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }
        
        return sb.toString();
    }
}
