package jp.scid.genomemuseum.controller

import javax.swing.event.{TreeExpansionEvent, TreeExpansionListener,
  TreeModelEvent, TreeModelListener, TreeWillExpandListener}
import javax.swing.JTree
import javax.swing.tree.ExpandVetoException

import jp.scid.gui.tree.DataTreeModel
import jp.scid.genomemuseum.gui.MuseumSourceModel
import jp.scid.genomemuseum.model.ExhibitRoom

/**
 * ツリー開閉
 */
class SourceListExpansionHandler(model: MuseumSourceModel) extends TreeWillExpandListener {
  import DataTreeModel._
  
  def openNodeOf(tree: JTree) {
    
    def openRowNodeOf(tree: JTree, row: Int) {
      if (row < tree.getRowCount()) {
        val treePath = tree.getPathForRow(row)
        if (!isCollapseAllowed(convertTreePathToPath(treePath))) {
          tree.expandPath(treePath)
        }
        openRowNodeOf(tree, row + 1)
      }
    }
    
    openRowNodeOf(tree, 0)
  }
  
  @throws(classOf[ExpandVetoException])
  def treeWillExpand(event: TreeExpansionEvent) {}
  
  @throws(classOf[ExpandVetoException])
  def treeWillCollapse(event: TreeExpansionEvent) {
    val pathForUserBoxes = model.pathForUserBoxes
    val pathForLibraries = model.pathForLibraries
    
    if (!isCollapseAllowed(convertTreePathToPath(event.getPath))) {
        throw new ExpandVetoException(event)
    }
  }
  
  protected def isCollapseAllowed(path: Path[ExhibitRoom]) = {
    path != model.pathForUserBoxes && path != model.pathForLibraries
  }
}
