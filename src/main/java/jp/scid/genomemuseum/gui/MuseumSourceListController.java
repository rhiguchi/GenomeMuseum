package jp.scid.genomemuseum.gui;

import static java.lang.String.*;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import jp.scid.genomemuseum.model.CollectionBox;
import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.CollectionBoxService;
import jp.scid.genomemuseum.model.EntityService;
import jp.scid.genomemuseum.model.ExhibitListModel;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumSourceModel;
import jp.scid.genomemuseum.model.MuseumSourceModel.CollectionNode;
import jp.scid.genomemuseum.model.MuseumSourceModel.ExhibitCollectionNode;
import jp.scid.genomemuseum.model.MuseumSourceModel.GroupCollectionNode;
import jp.scid.gui.control.ActionManager;

import org.jooq.UpdatableRecord;
import org.jooq.UpdatableTable;
import org.jooq.impl.Factory;



abstract class JooqRecordTreeController<E extends UpdatableRecord<E>> extends TreeController<E> {

    // persistence
    protected Factory factory = null;
    
    protected final UpdatableTable<E> table;
    
    public JooqRecordTreeController(UpdatableTable<E> table) {
        this.table = table;
    }
    
    @Override
    protected E createElement() {
        if (factory == null) {
            throw new IllegalStateException("need factory");
        }
        
        E record = factory.newRecord(table);
        return record;
    }
}