package jp.scid.genomemuseum.view;


import org.jdesktop.application.Application;
import org.jdesktop.application.FrameView;

class GUICheckApp extends Application {
    private static Class<? extends GenomeMuseumView> viewClass;
    
    private GenomeMuseumView contentView;
    private FrameView view = new FrameView(this);

    public static void launch(String[] args, Class<? extends GenomeMuseumView> viewClass) {
        GUICheckApp.viewClass = viewClass;
        Application.launch(GUICheckApp.class, args);
    }
    
    GUICheckApp() {
        getContext().getResourceManager().setResourceFolder("");
    }
    
    @Override
    protected void initialize(String[] args) {
        try {
            contentView = viewClass.newInstance();
        }
        catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    protected void startup() {
        view.setComponent(contentView.getContentPane());
    }
    
    @Override
    protected void ready() {
        view.getFrame().setLocationRelativeTo(null);
        view.getFrame().pack();
        view.getFrame().setVisible(true);
    }
}
