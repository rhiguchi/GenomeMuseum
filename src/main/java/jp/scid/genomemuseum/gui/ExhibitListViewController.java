package jp.scid.genomemuseum.gui;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.KeyStroke;

import jp.scid.genomemuseum.model.ExhibitListModel;
import jp.scid.genomemuseum.model.FreeExhibitBoxModel;
import jp.scid.genomemuseum.model.FreeExhibitBoxModel.Element;
import jp.scid.genomemuseum.model.MuseumExhibitTableFormat;
import jp.scid.genomemuseum.model.GMExhibit;
import jp.scid.genomemuseum.view.ExhibitListView;

import org.jooq.Condition;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;

public class ExhibitListViewController extends ListController<GMExhibit> {
    private String searchText = "";
    
    final TableFormat<GMExhibit> tableFormat;
    
    final BindingSupport bindings = new BindingSupport(this);
    
    protected ExhibitListModel exhibitListModel = null;
    
    private List<Long> boxContent = null;
    
    // Controllers
    protected BioFileLoader bioFileLoader = null;
    
    public ExhibitListViewController() {
        tableFormat = new MuseumExhibitTableFormat();
    }
    
    // bioFileLoader
    public BioFileLoader getBioFileLoader() {
        return bioFileLoader;
    }
    
    public void setBioFileLoader(BioFileLoader bioFileLoader) {
        this.bioFileLoader = bioFileLoader;
    }
    
    // exhibitListModel
    public ExhibitListModel getExhibitListModel() {
        return exhibitListModel;
    }
    
    public void setExhibitListModel(ExhibitListModel exhibitListModel) {
        setSource(null);
        
        this.exhibitListModel = exhibitListModel;
        
        fetch();
    }
    
    public void loadToModel(List<File> fileList) {
        getBioFileLoader().loadFilesRecursive(getExhibitListModel(), fileList);
    }
    
    @Override
    protected List<GMExhibit> retrieve() {
        List<GMExhibit> exhibitList = super.retrieve();
        boxContent = null;
        
        if (exhibitListModel instanceof FreeExhibitBoxModel) {
            long boxId = ((FreeExhibitBoxModel) exhibitListModel).getId();
            boxContent = getFreeBoxContent(boxId);
            
            if (boxContent.size() != exhibitList.size()) {
                Map<Long, GMExhibit> exibitMap = toIdMap(exhibitList);
                List<GMExhibit> realList = new ArrayList<GMExhibit>(boxContent.size());
                
                for (Long id: boxContent) {
                    GMExhibit exhibit = exibitMap.get(id);
                    if (exhibit == null)
                        throw new IllegalStateException("invalid id: " + id);
                    realList.add(exhibit);
                }
                
                exhibitList = realList;
            }
        }
        
        return exhibitList;
    }

    private static Map<Long, GMExhibit> toIdMap(final List<GMExhibit> exhibitList) {
        Map<Long, GMExhibit> exibitMap = new HashMap<Long, GMExhibit>(exhibitList.size());
        for (GMExhibit exhibit: exhibitList) {
            exibitMap.put(exhibit.getId(), exhibit);
        }
        return exibitMap;
    }
    
    List<Long> getFreeBoxContent(long boxId) {
        // TODO
        return null;
    }
    
    protected void requestFetching() {
        
    }
    
    @Override
    public void add(int index, GMExhibit exhibit) {
        if (exhibitListModel instanceof FreeExhibitBoxModel) {
            ((FreeExhibitBoxModel) exhibitListModel).add(index, exhibit);
            
            requestFetching();
        }
        
        super.add(index, exhibit);
    }
    
    @Override
    public void move(int[] indices, int dest) {
        if (exhibitListModel instanceof FreeExhibitBoxModel) {
            super.move(indices, dest);
            
//            FreeExhibitBoxModel model = (FreeExhibitBoxModel) exhibitListModel;
//            Iterator<Element> elementIte = boxElementList.iterator();
//            Iterator<GMExhibit> dataIte = listModel.iterator();
//            
//            while (elementIte.hasNext() && dataIte.hasNext()) {
//                Element element = elementIte.next();
//                GMExhibit exhibit = dataIte.next();
//                
//                if (!element.exhibit().getId().equals(exhibit.getId())) {
//                    model.updateIndexOfElement(element.orderIndex(), exhibit);
//                }
//            }
        }
    }
    
    @Override
    public void removeAt(int index) {
        
        ContentRemoveHandler removeHandler = null;
        
        if (exhibitListModel instanceof FreeExhibitBoxModel) {
            
            removeHandler = new ContentRemoveHandler();
            listModel.addListEventListener(removeHandler);
        }
        
        listModel.remove(index);
    }
    
    static class ContentRemoveHandler<E> implements ListEventListener<E> {
        
        public static <E> ContentRemoveHandler<E> install(EventList<E> list) {
            ContentRemoveHandler<E> removeHandler = new ContentRemoveHandler<E>();
            list.addListEventListener(removeHandler);
            return removeHandler;
        }
        
        @Override
        public void listChanged(ListEvent<E> listChanges) {
            // TODO Auto-generated method stub
            
            listChanges.getSourceList().removeListEventListener(this);
        }
    }
    
    public void bindExhibitListView(ExhibitListView view) {
        bindTable(view.dataTable);
    }
    
    @Override
    public void bindTable(JTable table) {
        super.bindTable(table);
        
        table.setFocusable(true);
        table.getInputMap().put(KeyStroke.getKeyStroke('+'), "add");
        table.getInputMap().put(KeyStroke.getKeyStroke('-'), "delete");
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete");
    }
    
    @Override
    public TableFormat<GMExhibit> getTableFormat() {
        return tableFormat;
    }
}