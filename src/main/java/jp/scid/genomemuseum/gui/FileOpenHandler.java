package jp.scid.genomemuseum.gui;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JMenuItem;

import jp.scid.gui.control.ActionManager;

public class FileOpenHandler {
    private FileDialog fileDialog;
    private final GeneticSequenceListController controller;
    private final Action openAction;
    
    public FileOpenHandler(GeneticSequenceListController controller, FileDialog fileDialog) {
        if (controller == null)
            throw new IllegalArgumentException("controller must not be null");
        this.controller = controller;
        this.fileDialog = fileDialog;
        
        openAction = new ActionManager(this).getAction("open");
    }
    
    public FileOpenHandler(GeneticSequenceListController controller) {
        this(controller, null);
    }
    
    public void open() {
        FileDialog dialog = getFileDialog();

        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return;
        }

        File file = new File(dialog.getDirectory(), dialog.getFile());
        try {
            controller.importFile(file);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void setFileDialog(FileDialog fileDialog) {
        this.fileDialog = fileDialog;
    }

    private FileDialog getFileDialog() {
        if (fileDialog == null) {
            fileDialog = new FileDialog((Frame) null);
        }
        return fileDialog;
    }
    
    public void bindOpenMenu(JMenuItem openMenu) {
        openMenu.setAction(openAction);
    }
}