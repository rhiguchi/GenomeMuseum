package jp.scid.genomemuseum.model

import javax.swing.tree.{TreeModel, TreeSelectionModel, DefaultTreeSelectionModel,
  DefaultMutableTreeNode, DefaultTreeModel, TreePath}
import javax.swing.event.{TreeModelListener}
  
class MuseumSourceModel {
  /** ソースリストのツリー構造 */
  val treSource = new MuseumStructure()
  /** JTree モデルを取得 */
  val treeModel = new SourceTreeModel[ExhibitRoom](treSource)
  /** JTree 選択モデルを取得 */
  val treeSelectionModel = new DefaultTreeSelectionModel
}

/**
 * ソースツリーモデル実装
 */
class SourceTreeModel[A <: AnyRef: ClassManifest](source: TreeSource[A]) extends TreeModel {
  import collection.mutable.WeakHashMap
  import SourceTreeModel._
  
  private val treeNodes = WeakHashMap.empty[A, SourceTreeNode[A]]
  
  protected val itemClass = implicitly[ClassManifest[A]]
  /** 
   * 項目の TreeNode を取得。
   * 親が設定されないので、設定の必要の無い root か 設定処理のある reloadChildren 以外で呼出してはならない
   */
  private def treeNodeFor(nodeObj: A) = treeNodes getOrElseUpdate
      (nodeObj, new SourceTreeNode(nodeObj, source.isLeaf(nodeObj)))
  
  /** ルートノード */
  private lazy val rootSource = treeNodeFor(source.root)
  
  /** ツリー構造管理の委譲先 */
  // TODO イベントのディスパッチを書き直し
  private lazy val treeDelegate = new DefaultTreeModel(rootSource, true)
  
  /** ルート項目を取得 */
  def getRoot: A = rootSource.nodeObject
  
  /** 項目が子項目を持つことができない要素か */
  def isLeaf(node: Any): Boolean = withSource(node){ treeDelegate isLeaf }
    
  
  /** 子項目数 */
  def getChildCount(parent: Any) = withSource(parent){ parent =>
    ensureChildrenLoad(parent)
    treeDelegate getChildCount parent
  }
  
  /** 子項目 */
  def getChild(parent: Any, index: Int): A = withSource(parent){ parent =>
    ensureChildrenLoad(parent)
    val child = treeDelegate.getChild(parent, index).asInstanceOf[DefaultMutableTreeNode]
    if (itemClass.erasure isInstance child.getUserObject)
      child.getUserObject.asInstanceOf[A]
    else
      throw new IllegalStateException("Not a source item")
  }
  
  /** 子項目の順序番号 */
  def getIndexOfChild(parent: Any, child: Any) = withSource(parent) { parent =>
    withSource(child) { child =>
      treeDelegate.getIndexOfChild(parent, child)
    }
  }
  
  /** 値の変更 */
  def valueForPathChanged(path: TreePath, newValue: AnyRef) {
    // val pathSeq: IndexedSeq[A]
    // TODO
  }
  
  // リスナー
  def addTreeModelListener(l: TreeModelListener) =
    treeDelegate addTreeModelListener l
  def removeTreeModelListener(l: TreeModelListener) =
    treeDelegate removeTreeModelListener l
    
  /** 子要素の読み出しが行われているようにする */
  private def ensureChildrenLoad(node: SourceTreeNode[A]) {
    if (!node.childPrepared) reloadChildren(node)
  }
  
  /** 子要素の再読み込み */
  private def reloadChildren(node: SourceTreeNode[A]) =
    node.updateChildren(
      source childrenFor node.nodeObject map treeNodeFor)
  
  /** 項目を持つ TreeNode に対する処理 */
  private def withSource[B](item: Any)(taskWith: SourceTreeNode[A] => B) = 
    if (itemClass.erasure isInstance item)
      taskWith(treeNodes(item.asInstanceOf[A]))
    else
      throw new IllegalArgumentException("%s must be a %s"
        .format(item.getClass, itemClass.erasure.getName))
}

object SourceTreeModel {
  private class SourceTreeNode[A](
      val nodeObject: A, override val isLeaf: Boolean) 
      extends DefaultMutableTreeNode(nodeObject, !isLeaf) {
    private var myChildren: Option[IndexedSeq[SourceTreeNode[A]]] = None
    
    def childPrepared = myChildren.isDefined
    
    def updateChildren(newChildren: Seq[SourceTreeNode[A]]) {
      myChildren = Some(newChildren.toIndexedSeq)
      removeAllChildren()
      newChildren foreach add
    }
  }
}
