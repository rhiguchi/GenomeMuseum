package jp.scid.genomemuseum;

import java.util.logging.Logger;

import org.jdesktop.application.Application;

public class GenomeMuseum {
    private static Logger logger = Logger.getLogger(GenomeMuseum.class.getPackage().getName());
    
    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "GenomeMuseum");
        
        logger.info("Welcome to GenomeMuseum.");
        logger.fine("Debug logging mode");
        
        Application.launch(jp.scid.genomemuseum.gui.GenomeMuseum.class, args);
    }
}
