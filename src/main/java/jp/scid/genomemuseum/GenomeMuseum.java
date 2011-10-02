package jp.scid.genomemuseum;

import org.jdesktop.application.Application;

public class GenomeMuseum {
    public static void main(String[] args) {
        System.out.println("Welcome to GenomeMuseum.");
        
        Application.launch(GenomeMuseumGUI.class, args);
    }
}
