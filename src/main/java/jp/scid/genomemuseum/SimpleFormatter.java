package jp.scid.genomemuseum;

import java.text.MessageFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SimpleFormatter extends Formatter {
    private final static String lineSep = System.getProperty("line.separator");
    
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(record.getLevel()).append("] ");
        
        String msg = record.getMessage();
        if (msg == null)
            msg = "";
        else if (record.getParameters() != null)
            msg = MessageFormat.format(msg, record.getParameters());
        
        sb.append(msg).append(lineSep);
        
        return sb.toString();
    }

}
