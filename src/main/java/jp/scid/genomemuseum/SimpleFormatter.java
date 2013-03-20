package jp.scid.genomemuseum;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SimpleFormatter extends Formatter {
    private final static String lineSep = System.getProperty("line.separator");
    
    private final Date date = new Date();
    
    public SimpleFormatter() {
    }
    
    @Override
    public String format(LogRecord record) {
        String dateTime = formatDateTime(record.getMillis());
        String msg = formatMessage(record);
        
        StringBuilder sb = new StringBuilder();
        sb.append(dateTime).append(" ");
        sb.append(record.getSourceClassName()).append(" ");
        sb.append("[").append(record.getLevel()).append("] ");
        sb.append(msg).append(lineSep);
        
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ignore) {
                // ignore
            }
        }
        
        return sb.toString();
    }

    protected String formatDateTime(long time) {
        date.setTime(time);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(date);
    }
}
