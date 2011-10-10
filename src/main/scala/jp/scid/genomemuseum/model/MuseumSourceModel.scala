package jp.scid.genomemuseum.model

import javax.swing.tree.{TreeModel, TreeSelectionModel, DefaultTreeSelectionModel,
  DefaultMutableTreeNode, DefaultTreeModel, TreePath}
import javax.swing.event.TreeModelListener
import jp.scid.gui.tree.SourceTreeModel
import ExhibitListBox.BoxType._

class MuseumSourceModel {
  /** ソースリストのツリー構造 */
  val treeSource = new MuseumStructure()
  /** JTree モデルを取得 */
  val treeModel = new SourceTreeModel[ExhibitRoom](treeSource)
  /** JTree 選択モデルを取得 */
  val treeSelectionModel = new DefaultTreeSelectionModel
  
  private def roomService = treeSource.userBoxesSource
  
  /**
   * リストボックスの追加
   */
  def addListBox(boxName: String) =
    addFolder(ExhibitListBox(boxName, ListBox))
  
  /**
   * 親フォルダにリストボックスを追加
   */
  def addListBox(boxName: String, parent: ExhibitListBox) =
    addFolder(ExhibitListBox(boxName, ListBox), Option(parent))
  
  /**
   * スマートボックスの追加
   */
  def addSmartBox(boxName: String) =
    addFolder(ExhibitListBox(boxName, SmartBox))
  
  /**
   * 親フォルダにスマートボックスを追加
   */
  def addSmartBox(boxName: String, parent: ExhibitListBox) =
    addFolder(ExhibitListBox(boxName, SmartBox), Option(parent))
  
  /**
   * ボックスフォルダの追加
   */
  def addBoxFolder(boxName: String) =
    addFolder(ExhibitListBox(boxName, BoxFolder))
  
  /**
   * 親フォルダにボックスフォルダを追加
   */
  def addBoxFolder(boxName: String, parent: ExhibitListBox) =
    addFolder(ExhibitListBox(boxName, BoxFolder), Option(parent))
    
  /**
   * ユーザーボックスを削除
   */
  def removeBoxFromParent(box: ExhibitListBox) {
    val parent = roomService.getParent(box)
    roomService.remove(box)
    treeModel.someChildrenWereRemoved(parent.getOrElse(treeSource.userBoxes))
  }
  
  /**
   * 要素を追加する
   */
  private def addFolder(newBox: ExhibitListBox,
      parent: Option[ExhibitListBox] = None): ExhibitListBox = {
    if (parent.isDefined && parent.get.boxType != BoxFolder)
      throw new IllegalArgumentException("The type of parent must be BoxFolder")
    
    roomService.add(newBox, parent)
    treeModel.someChildrenWereInserted(parent.getOrElse(treeSource.userBoxes))
    newBox
  }
  
  def userBoxesSource: MuseumScheme.ExhibitRoomService = MuseumScheme.ExhibitRoomService.empty
  
  def userBoxesSource_=(newSource: MuseumScheme.ExhibitRoomService) {
//    treeSource.userBoxesSource = newSource
//    updateSource()
  }
  
  protected def updateSource() {
    treeModel.reset()
  }
}
