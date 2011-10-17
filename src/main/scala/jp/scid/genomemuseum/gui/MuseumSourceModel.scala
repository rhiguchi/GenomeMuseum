package jp.scid.genomemuseum.gui

import jp.scid.gui.tree.{DataTreeModel, SourceTreeModel, TreeSource}
import jp.scid.genomemuseum.model.{MuseumStructure, ExhibitRoom, ExhibitListBox,
  TreeDataService}

class MuseumSourceModel(source: MuseumStructure) extends DataTreeModel[ExhibitRoom](source)
    with TreeDataServiceSource[ExhibitRoom, ExhibitListBox] {
  import ExhibitListBox.BoxType._
  import DataTreeModel.Path
  
  /**
   * 現在のユーザーボックスのデータソースを取得
   */
  def userBoxesSource = source.userBoxesSource
  
  /**
   * ユーザーボックスのデータソースを設定する。
   * 同じオブジェクトで、 {@code dataService} も設定される
   */
  def userBoxesSource_=(newSource: TreeDataService[ExhibitListBox]) {
    source.userBoxesSource = newSource
    dataService = newSource
  }
  
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
  
  /** データサービス更新時にイベント送出に使用するルート要素 */
  override def serviceRootElement = source.userBoxes
  
  /**
   * 親が BoxFolder であるか判定して要素を追加する
   */
  private def addFolder(newBox: ExhibitListBox,
      parent: Option[ExhibitListBox] = None): ExhibitListBox = {
    if (parent.isDefined && parent.get.boxType != BoxFolder)
      throw new IllegalArgumentException("The type of parent must be BoxFolder")
    
    addElement(newBox, parent)
    newBox
  }
  
  /** ローカルライブラリノードへのパス */
  def pathForLocalLibrary: Path[ExhibitRoom] =
    Path(source.root, source.libraries, source.localStore)
  
  /** ウェブソースノードへのパス */
  def pathForWebSource: Path[ExhibitRoom] =
    Path(source.root, source.libraries, source.entrez)
  
  /** ユーザーボックスノードへのパス */
  def pathForUserBoxes: Path[ExhibitRoom] =
    Path(source.root, source.userBoxes)
  
  /** ローカルライブラリノードを選択状態にする */
  def selectPathLocalLibrary() {
    selectPath(pathForLocalLibrary)
  }
  
}