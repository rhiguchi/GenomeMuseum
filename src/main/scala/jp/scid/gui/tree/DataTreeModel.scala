package jp.scid.gui.tree

import javax.swing.JTree
import javax.swing.tree.{TreeModel, TreeSelectionModel, DefaultTreeSelectionModel,
  TreePath}

import jp.scid.gui.{DataModel}
import jp.scid.gui.plaf.SourceListTreeUI

class DataTreeModel[A <: AnyRef: ClassManifest](source: TreeSource[A])
    extends DataModel with swing.Publisher {
  import DataTreeModel._
  
  /** JTree 用モデル */
  protected val sourceTreeModel = new SourceTreeModel[A](source)
  /** JTree 選択モデルを取得 */
  private val treeSelectionModel = new SourceListSelectionModel(isSelectablePath _)
  
  /** パス項目数が 2 以下のノードは選択できないようにするか */
  var sourceListSelectionMode: Boolean = false
  
  /** ツリーソースを取得 */
  def treeSource = source
  
  /** ツリーモデルを取得 */
  def treeModel: TreeModel = sourceTreeModel
  
  /** ツリー選択モデルを取得 */
  def selectionModel: TreeSelectionModel = treeSelectionModel
  
  /**
   * パスを選択状態にする
   */
  def selectPath(path: Path[A]) {
    selectionModel setSelectionPath convertPathToTreePath(path)
  }
  
  /**
   * パスを選択状態にする
   */
  def selectPaths(paths: Seq[Path[A]]) {
    selectionModel.setSelectionPaths(paths map convertPathToTreePath toArray)
  }
  
  /**
   * どのパスも選択されていない状態にする
   */
  def deselect() {
    selectionModel.clearSelection()
  }
  
  /**
   * 選択パスを取得
   */
  def selectedPaths(): IndexedSeq[Path[A]] = {
    selectionModel.getSelectionPaths.map(convertTreePathToPath[A]).toIndexedSeq
  }
  
  /**
   * パスが選択されているか
   */
  def isPathSelected(path: Path[A]) = {
    selectionModel.isPathSelected(convertPathToTreePath(path))
  }
  
  /**
   * JTree にこのモデルを適用する。
   */
  def installTo(tree: JTree) {
    // UI
    tree setUI new SourceListTreeUI
    tree setRootVisible false
//    tree setFocusable false
    tree setInvokesStopCellEditing false
    tree setToggleClickCount 0
    tree setEditable true
    
    // モデル SourceListTreeUI が selectionModel を設定するため後から設定
    tree setModel treeModel
    tree setSelectionModel selectionModel
  }
  
  /**
   * 選択可能パスであるか
   */
  private def isSelectablePath(path: TreePath): Boolean = {
     !sourceListSelectionMode || path.getPathCount > 2
  }
  
  // イベント結合
  bindSelectionEvent(this)
}

object DataTreeModel {
  import javax.swing.event.{TreeSelectionListener, TreeSelectionEvent}
  import jp.scid.gui.event.DataTreePathsSelectionChanged
  
  val Path = IndexedSeq
  type Path[+A] = IndexedSeq[A]
  
  def convertTreePathToPath[A](path: TreePath) =
    Path(path.getPath: _*) map (_.asInstanceOf[A])
  
  def convertPathToTreePath[A](path: Path[A]) =
    new TreePath(path.map(_.asInstanceOf[Object]).toArray)
  
  /** TreeSelectionModel のイベントを swing.Publisher から発行するように結合 */
  private def bindSelectionEvent[A <: AnyRef](model: DataTreeModel[A]) {
    val handler = new TreeSelectionEventHandler(model)
    model.selectionModel addTreeSelectionListener handler
  }
  
  /**
   * TreeSelectionModel のイベントを scala.event.Event に変換するハンドラ 
   */
  private class TreeSelectionEventHandler[A <: AnyRef](publisher: DataTreeModel[A])
      extends TreeSelectionListener {
    def valueChanged(tsEvent: TreeSelectionEvent) {
      // イベントオブジェクトの生成
      val (newPaths, oldPaths) = tsEvent.getPaths.toList partition tsEvent.isAddedPath
      val newPathSeq = newPaths map convertTreePathToPath
      val oldPathSeq = oldPaths map convertTreePathToPath
      
      val dtEvent = DataTreePathsSelectionChanged(publisher, newPathSeq, oldPathSeq)
      
      // イベント発行
      publisher publish dtEvent
    }
  }
  
  /**
   * 選択モデルの実装。ソースリストの大項目は選択できないようにしている。
   */
  private class SourceListSelectionModel[A <: AnyRef](isSelectable: TreePath => Boolean)
      extends DefaultTreeSelectionModel {
    override def setSelectionPath(path: TreePath) {
      if (isSelectable(path))
        super.setSelectionPath(path)
    }
    
    override def setSelectionPaths(paths: Array[TreePath]) {
      val selectPaths = paths.filter(isSelectable)
      if (selectPaths.nonEmpty)
        super.setSelectionPaths(selectPaths)
    }
    
    override def addSelectionPath(path: TreePath) {
      if (isSelectable(path))
        super.addSelectionPath(path)
    }
    
    override def addSelectionPaths(paths: Array[TreePath]) {
      val selectPaths = paths.filter(isSelectable)
      if (selectPaths.nonEmpty)
        super.addSelectionPaths(selectPaths)
    }
  }
  
}
