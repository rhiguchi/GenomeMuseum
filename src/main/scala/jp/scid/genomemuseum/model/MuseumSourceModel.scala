package jp.scid.genomemuseum.model

import javax.swing.tree.{TreeModel, TreeSelectionModel, DefaultTreeSelectionModel,
  DefaultMutableTreeNode, DefaultTreeModel, TreePath}
import javax.swing.event.TreeModelListener
import MuseumScheme.ExhibitRoomService
import jp.scid.gui.tree.SourceTreeModel

class MuseumSourceModel {
  /** ソースリストのツリー構造 */
  val treeSource = new MuseumStructure()
  /** JTree モデルを取得 */
  val treeModel = new SourceTreeModel[ExhibitRoom](treeSource)
  /** JTree 選択モデルを取得 */
  val treeSelectionModel = new DefaultTreeSelectionModel
  
  def addNewListBox(boxName: String) {
    val newBox = ExhibitListBox(boxName)
    userBoxesSource save newBox
    updateSource()
  }
  
  def userBoxesSource = treeSource.userBoxesSource
  
  def userBoxesSource_=(newSource: ExhibitRoomService) {
    treeSource.userBoxesSource = newSource
    updateSource()
  }
  
  protected def updateSource() {
    treeModel.reset()
  }
}
