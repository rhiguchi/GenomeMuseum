package jp.scid.gui.control;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jp.scid.gui.model.SourceTreeModel;
import jp.scid.gui.model.TreeSource;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class TreeController<E, M extends TreeSource<E>> extends AbstractController<M> {
    private final SourceTreeModel<E> treeModel = new SourceTreeModel<E>();
    
    private TreeSelectionModel treeSelectionModel = null;
    
    private final ValueModel<TreePath> pathEditTrigger = ValueModels.newNullableValueModel();
    
    private final EditSelectionAction editAction = new EditSelectionAction("Edit");
    
    private final DeleteAction deleteAction = new DeleteAction("Delete");
    
    private List<E> defaultSelection = null;
    
    private final ValueModel<E> selectionModel = ValueModels.newNullableValueModel();
    
    /**
     * List for selected paths
     */
    private final ValueModel<List<E>> selectionsModel = ValueModels.newListModel();
    
    public TreeController() {
    }
    
    public TreeModel getTreeModel() {
        return treeModel;
    }
    
    public synchronized TreeSelectionModel getTreeSelectionModel() {
        if (treeSelectionModel == null) {
            treeSelectionModel = createTreeSelectionModel();
            TreeSelectionModelHandler selectionHandler = new TreeSelectionModelHandler();
            treeSelectionModel.addTreeSelectionListener(selectionHandler);
            selectionChange();
        }
        
        return treeSelectionModel;
    }
    
    @SuppressWarnings("unchecked")
    public List<List<E>> getSelectedPathList() {
        TreePath[] selections = getTreeSelectionModel().getSelectionPaths();
        
        if (selections == null || selections.length == 0) {
            return Collections.emptyList();
        }
            
        List<List<E>> selectionList = new ArrayList<List<E>>(selections.length);
        
        for (int i = 0; i < selections.length; i++) {
            TreePath path = selections[i];
            List<E> listPath = (List<E>) Arrays.asList(path.getPath());
            selectionList.add(listPath);
        }
        
        return selectionList;
    }
    
    protected TreeSelectionModel createTreeSelectionModel() {
        SelectableSelectionModel selectionModel = new SelectableSelectionModel();
        return selectionModel;
    }
    
    public void bindTree(JTree tree) {
        tree.setModel(getTreeModel());
        tree.setSelectionModel(getTreeSelectionModel());
        
        TreePathEditor treePathEditor = new TreePathEditor(tree);
        treePathEditor.setModel(pathEditTrigger);
        
        tree.getActionMap().put("delete", deleteAction);
        tree.getActionMap().put("edit", editAction);
    }
    
    TreeSource<E> getTreeSource() {
        return treeModel.getTreeSource();
    }
    
    @Override
    public void setModel(M newModel) {
        super.setModel(newModel);
        
        selectionChange();
        
        if (newModel != treeModel.getTreeSource())
            treeModel.setTreeSource(newModel);
    }
    
    @Override
    protected void processPropertyChange(M model, String property) {
    }
    
    // Selecting
    public ValueModel<E> getSelection() {
        return selectionModel;
    }
    
    public ValueModel<List<E>> getSelections() {
        return selectionsModel;
    }
    
    protected boolean isSelectable(List<E> path) {
        return true;
    }
    
    public void selectPath(List<E> path) {
        TreePath treePath = new TreePath(path.toArray());
        getTreeSelectionModel().setSelectionPath(treePath);
    }
    
    public void addSelectionPath(List<E> path) {
        TreePath treePath = new TreePath(path.toArray());
        getTreeSelectionModel().addSelectionPath(treePath);
    }
    
    public void removeSelectionPath(List<E> path) {
        TreePath treePath = new TreePath(path.toArray());
        getTreeSelectionModel().removeSelectionPath(treePath);
    }
    
    public List<E> getDefaultSelection() {
        return defaultSelection;
    }
    
    public void setDefaultSelection(List<E> defaultSelection) {
        this.defaultSelection = defaultSelection;
        if (defaultSelection != null && getTreeSelectionModel().isSelectionEmpty()) {
            selectPath(defaultSelection);
        }
    }
    
    // Editing
    public boolean isEditable(List<E> path) {
        return true;
    }
    
    /**
     * Start editing for selection
     */
    public void edit() {
        if (treeSelectionModel == null || treeSelectionModel.isSelectionEmpty())
            return;
        
        List<E> listPath = getListPath(treeSelectionModel.getSelectionPath());
        if (isEditable(listPath)) {
            editPath(listPath);
        }
    }
    
    /**
     * Starts editing at the path
     * @param path To start
     */
    public void editPath(List<E> path) {
        TreePath treePath = new TreePath(path.toArray());
        pathEditTrigger.setValue(treePath);
    }
    
    // Deleting
    /**
     * Removes the selections from tree model.
     */
    public void delete() {
        List<List<E>> pathList = getSelectedPathList();
        
        for (List<E> path: pathList) {
            if (isDeletable(path)) {
                deletePath(path);
            }
        }
    }
    
    /**
     * Deletes the path from tree model.
     * @param path path to root for removing element
     */
    public void deletePath(List<E> path) {
        treeModel.removePath(path);
    }
    
    /**
     * @param path
     * @return allowed to delete
     */
    public boolean isDeletable(List<E> path) {
        return true;
    }
    
    public Action getDeleteAction() {
        return deleteAction;
    }
    
    // Moving
    public boolean isMovable(TreePath sourcePath, TreePath destPath) {
        if (sourcePath.getPathCount() < 1 || destPath.getPathCount() < 1) {
            return false;
        }
        else if (destPath.equals(sourcePath) || sourcePath.isDescendant(destPath)) {
            return false;
        }
        
        return true;
    }
    
    @SuppressWarnings("unchecked")
    public void movePath(TreePath sourcePath, TreePath destPath) {
        List<Object> listSourcePath = Arrays.asList(sourcePath.getPath());
        List<Object> listDestPath = Arrays.asList(destPath.getPath());
        
        treeModel.movePath((List<E>) listSourcePath, (List<E>) listDestPath);
    }
    
    // Selection change handler
    void selectionChange() {
        List<List<E>> pathList = getSelectedPathList();
        updateDeleteActionEnabled(pathList);
        
        updateEditActionEnabled(pathList);
        
        updateSelectionModel(pathList);
        
        updateSelectionsModel(pathList);
        
        if (pathList.isEmpty() && getDefaultSelection() != null) {
            selectPath(getDefaultSelection());
        }
    }

    void updateSelectionModel(List<List<E>> pathList) {
        E selection = null;
        if (!pathList.isEmpty()) {
            List<E> head = pathList.get(0);
            selection = head.get(head.size() - 1);
        }
        getSelection().setValue(selection);
    }

    void updateSelectionsModel(List<List<E>> pathList) {
        List<E> selections = new ArrayList<E>(pathList.size());
        for (List<E> path: pathList) {
            E element = path.get(path.size() - 1);            
            selections.add(element);
        }
        getSelections().setValue(selections);
    }
    
    void updateDeleteActionEnabled(List<List<E>> pathList) {
        boolean enabled = false;
        for (List<E> path: pathList) {
            if (isDeletable(path)) {
                enabled = true;
                break;
            }
        }
        deleteAction.setEnabled(enabled);
    }

    void updateEditActionEnabled(List<List<E>> pathList) {
        boolean enabled = false;
        for (List<E> path: pathList) {
            if (isEditable(path)) {
                enabled = true;
                break;
            }
        }
        editAction.setEnabled(enabled);
    }
    
    @SuppressWarnings("unchecked")
    List<E> getListPath(TreePath path) {
        return (List<E>) Arrays.asList(path.getPath());
    }
    
    class SelectableSelectionModel extends DefaultTreeSelectionModel {
        @Override
        public void setSelectionPath(TreePath path) {
            if (canSelect(path))
                super.setSelectionPath(path);
        }
        
        @Override
        public void setSelectionPaths(TreePath[] paths) {
            super.setSelectionPaths(filterSelectable(paths));
        }

        @Override
        public void addSelectionPath(TreePath path) {
            if (canSelect(path))
                super.addSelectionPath(path);
        }
        
        @Override
        public void addSelectionPaths(TreePath[] paths) {
            super.addSelectionPaths(filterSelectable(paths));
        }

        TreePath[] filterSelectable(TreePath[] pPaths) {
            if (pPaths == null)
                return null;
            
            List<TreePath> pathList = new ArrayList<TreePath>(pPaths.length);
            for (TreePath path: pPaths) {
                if (canSelect(path))
                    pathList.add(path);
            }
            return pathList.toArray(new TreePath[0]);
        }

        boolean canSelect(TreePath path) {
            List<E> listPath = getListPath(path);
            return TreeController.this.isSelectable(listPath) ;
        }
    }
    
    static class TreePathEditor extends ViewValueConnector<JTree, TreePath> {
        public TreePathEditor(JTree targetView) {
            super(targetView);
        }

        @Override
        protected void updateView(JTree target, TreePath modelValue) {
            if (modelValue == null) {
                target.stopEditing();
            }
            else {
                target.startEditingAtPath(modelValue);
            }
        }
    }
    
    class DeleteAction extends AbstractAction {
        public DeleteAction(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            delete();
        }
    }
    
    class EditSelectionAction extends AbstractAction {
        public EditSelectionAction(String name) {
            super(name);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            edit();
        }
    }
    
    class TreeSelectionModelHandler implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            selectionChange();
        }
    }
}
