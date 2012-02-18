package jp.scid.genomemuseum;

import java.io.UnsupportedEncodingException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdesktop.application.Application;

public class GenomeMuseum {
    public static void main(String[] args) {
        Logger logger = Logger.getLogger(GenomeMuseum.class.getPackage().getName());
        if (logger.getHandlers().length == 0) {
            ConsoleHandler h = new ConsoleHandler();
            h.setFormatter(new SimpleFormatter());
            h.setLevel(Level.ALL);
            try {
                h.setEncoding("UTF-8");
            }
            catch (SecurityException e) {
                throw new IllegalStateException(e);
            }
            catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            
            logger.addHandler(h);
            logger.setLevel(Level.FINEST);
            logger.setUseParentHandlers(false);
            
            Logger guilogger = Logger.getLogger("jp.scid.gui");
            guilogger.addHandler(h);
            guilogger.setLevel(Level.INFO);
            guilogger.fine("GUI logger is prepared.");
        }
        
        logger.info("Welcome to GenomeMuseum.");
        
        Application.launch(GenomeMuseumGUI.class, args);
    }
}
