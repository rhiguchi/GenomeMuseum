package jp.scid.genomemuseum.controller

import javax.swing.event.{TreeExpansionEvent,  TreeWillExpandListener}
import javax.swing.tree.ExpandVetoException
import javax.swing.JTree

import jp.scid.gui.tree.DataTreeModel.{Path, convertTreePathToPath}
import jp.scid.genomemuseum.gui.MuseumSourceModel

/**
 * ツリーの展開状態を管理するクラス。
 */
class ExhibitRoomListExpansionController(tree: JTree, model: MuseumSourceModel) {
  
  private val expandHandler = new TreeWillExpandListener {
    @throws(classOf[ExpandVetoException])
    def treeWillExpand(event: TreeExpansionEvent) {}
    
    @throws(classOf[ExpandVetoException])
    def treeWillCollapse(event: TreeExpansionEvent) {
      if (!isCollapseAllowed(convertTreePathToPath(event.getPath))) {
          throw new ExpandVetoException(event)
      }
    }
  }
  
  /**
   * ツリーの展開状態を更新する。
   */
  def update() {
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
  
  /**
   * ツリーの展開状態の管理を修了する。
   */
  def dispose() {
    tree removeTreeWillExpandListener expandHandler
  }
  
  protected def isCollapseAllowed(path: Path[_]) = {
    path != model.pathForLocalLibrary.take(2) && path != model.pathForUserRooms
  }
  
  tree addTreeWillExpandListener expandHandler
  update()
}
