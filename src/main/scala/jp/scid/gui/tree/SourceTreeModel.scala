package jp.scid.gui.tree

import javax.swing.tree.{TreeModel, TreeSelectionModel, DefaultTreeSelectionModel,
  DefaultMutableTreeNode, DefaultTreeModel, TreePath}
import javax.swing.event.TreeModelListener
import collection.mutable.Buffer

/**
 * ソースツリーモデル実装
 * @param source ツリーソース
 */
class SourceTreeModel[A <: AnyRef: ClassManifest](source: TreeSource[A]) extends TreeModel { self =>
  import collection.mutable.WeakHashMap
  import SourceTreeModel._
  
  private val treeNodes = WeakHashMap.empty[A, SourceTreeNode[A]]
  
  protected val itemClass = implicitly[ClassManifest[A]]
  /** 
   * 項目の TreeNode を取得。
   * 親が設定されないので、設定の必要の無い root か 設定処理のある reloadChildren 以外で呼出してはならない
   * @throws NoSuchElementException {@code nodeObj} がこのモデルでは不明である時
   */
  private def treeNodeFor(nodeObj: A) = treeNodes getOrElseUpdate
      (nodeObj, new SourceTreeNode(nodeObj, source.isLeaf(nodeObj)))
  
  /** ルートノード */
  private lazy val rootSource = treeNodeFor(source.root)
  
  /** ツリー構造管理の委譲先 */
  private lazy val treeDelegate = new SourceTreeModelDelegate(rootSource)
  
  /** ルート項目を取得 */
  def getRoot: A = rootSource.nodeObject
  
  /** 
   * 項目が子項目を持つことができない要素か
   * @param node {@code A} 型で親ノードから既に読み出されたことがあるオブジェクト
   * @retrun 子項目を持つことができる場合 {@code true}
   * @throws IllegalArgumentException {@code node} が {@code A} 型でない時
   * @throws NoSuchElementException {@code node} の親が不明でパスが定まっていない時
   */
  def isLeaf(node: Any): Boolean = withSource(node){ treeDelegate isLeaf }
  
  /**
   * 子要素が source から読み込まれているか。
   * @return ノードの子要素が読み込み済みの時は {@code true} 。Leaf の時は常に {@code false} 。
   * @throws IllegalArgumentException {@code node} が {@code A} 型でない時
   * @throws NoSuchElementException {@code node} の親が不明でパスが定まっていない時
   */
  def isChildrenExposed(node: Any): Boolean = withSource(node) { node => node.isLeaf match {
    case true => false
    case false => node.childPrepared
  }}
  
  /** 
   * 子項目数の取得
   * @param parent {@code A} 型で親ノードから既に読み出されたことがあるオブジェクト
   * @retrun 子項目数
   * @throws IllegalArgumentException {@code parent} が {@code A} 型でない時
   * @throws NoSuchElementException {@code parent} の親が不明でパスが定まっていない時
   */
  def getChildCount(parent: Any) = withSource(parent){ parent =>
    ensureChildrenLoad(parent)
    treeDelegate getChildCount parent
  }
  
  /** 
   * 子項目の取得
   * @param parent {@code A} 型で親ノードから既に読み出されたことがあるオブジェクト
   * @param index 位置
   * @retrun 子項目
   * @throws IllegalArgumentException {@code parent} が {@code A} 型でない時
   * @throws IndexOutOfBoundsException {@code index} が 0 未満か {@link #getChildCount(Any)}
   *         の値以上の時
   */
  def getChild(parent: Any, index: Int): A = withSource(parent){ parent =>
    ensureChildrenLoad(parent)
    val child = treeDelegate.getChild(parent, index).asInstanceOf[DefaultMutableTreeNode]
    if (itemClass.erasure isInstance child.getUserObject)
      child.getUserObject.asInstanceOf[A]
    else
      throw new IllegalStateException("Not a source item")
  }
  
  /** 
   * 子項目の順序番号を取得
   * @param parent {@code A} 型で親ノードから既に読み出されたことがあるオブジェクト
   * @param child {@code A} 型で親ノードから既に読み出されたことがあるオブジェクト
   * @retrun 順序番号
   * @throws IllegalArgumentException {@code parent} もしくは {@code child} が
   *         {@code A} 型でない時
   * @throws NoSuchElementException {@code parent} が不明でパスが定まっていない時
   */
  def getIndexOfChild(parent: Any, child: Any) = withSource(parent) { parent =>
    getTreeNode(child) match {
      case Some(childNode) =>
        treeDelegate.getIndexOfChild(parent, childNode)
      case None => -1
    }
  }
  
  /** 
   * 項目の値の変更。
   * {@code source} が {@code EditableTreeSource} を継承している場合にのみ有効。
   * @param path {@code A} 型のオブジェクトからなる変更項目までのパス
   * @param newValue 変更情報を持つ値
   * @throws IllegalArgumentException {@code path} に {@code A} 型でないオブジェクトが含まれている。
   * @throws NoSuchElementException {@code path} にパスが定まっていない項目が含まれる時。
   */
  def valueForPathChanged(path: TreePath, newValue: AnyRef) {
    val seqPath = convertTreePath(path)
    val lastNode = treeNodes(seqPath.last)
    source match {
      case source: EditableTreeSource[_] =>
        source.update(seqPath, newValue)
        treeDelegate nodeChanged lastNode
      case _ =>
    }
  }
  
  /**
   * ツリー構造を初期化する。
   */
  def reset(): Unit = reset(getRoot)
  
  /**
   * 指定した項目から下のツリーを初期化する
   * @param node このノードから先をソースから読み込む
   */
  def reset(item: A) = getTreeNode(item) map { node =>
    val node = treeNodes(item)
    reloadChildren(node)
    treeDelegate.reload(node)
  }
  
  /**
   * TreeSource へ子要素が挿入されたことを、モデルに通知する
   */
  def someChildrenWereInserted(parent: A) {
    treeNodes.get(parent) filter (_.childPrepared) foreach { node =>
      val oldChildren = node.getChildren.toList
      reloadChildren(node)
      val newChildren = node.getChildren.toList
      val inserted = newChildren diff oldChildren
      val indices = getIndices(Buffer.empty, newChildren, inserted).toIndexedSeq
      treeDelegate.nodesWereInserted(node, indices.toArray)
    }
  }
  
  /**
   * TreeSource から子要素が除去されたことをモデルに通知する
   */
  def someChildrenWereRemoved(parent: A) {
    treeNodes.get(parent) filter (_.childPrepared) foreach { node =>
      val oldChildren = node.getChildren
      reloadChildren(node)
      val newChildren = node.getChildren
      val remove = oldChildren diff newChildren
      val indices = getIndices(Buffer.empty, oldChildren, remove).toIndexedSeq
      removeDescendant(remove.toList)
      treeDelegate.nodesWereRemoved(node, indices.toArray, remove.toArray)
    }
  }
  
  def nodeRemoved(parent: A) {
    // TODO
  }
  
  def nodeChanged(parent: A) {
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
  private def reloadChildren(node: SourceTreeNode[A]) {
    val children =
      if (treeDelegate isLeaf node)
        IndexedSeq.empty[SourceTreeNode[A]]
      else
        source childrenFor node.nodeObject map treeNodeFor
    node.updateChildren(children)
  }
  
  private def removeDescendant(nodes: List[SourceTreeNode[A]]): Unit = nodes match {
    case node :: tail =>
      val children = node.getChildren.toList
      node.resetChildren()
      treeNodes.remove(node.nodeObject)
      node.removeFromParent()
      
      removeDescendant(tail ::: children)
    case Nil =>
  }
  
  /**
   * このモデルの要素クラスで、 TreeNode を参照することができれば、Option 値に格納して返す
   * @throws IllegalArgumentException {@code item} が {@code A} 型 でない時
   */
  private def getTreeNode(item: Any) = itemClass.erasure isInstance item match {
    case true => treeNodes.get(item.asInstanceOf[A])
    case false => throw new IllegalArgumentException("%s must be a %s"
          .format(item.getClass, itemClass.erasure.getName))
  }
  
  /** 
   * {@code A} 型項目を持つ TreeNode に対する処理 
   * @param item {@code A} 型のオブジェクト
   * @param taskWith {@code SourceTreeNode[A]} を受け、 {@code B} の値を返す関数。
   * @throws IllegalArgumentException {@code item} が {@code A} 型 でない時
   * @throws NoSuchElementException {@code item} がこのモデルでは不明である時
   */
  private def withSource[B](item: Any)(taskWith: SourceTreeNode[A] => B) = 
    getTreeNode(item) match {
      case Some(node) => taskWith(node)
      case None =>
        throw new NoSuchElementException("%s treeNode is not prepared yet."
          .format(item.getClass))
    }
  
  /** 
   * {@code SourceTreeNode[A]} 型ノードから項目を取得
   * @param node {@code SourceTreeNode[A]} 型のオブジェクト
   * @return {@code A} 型であるノードオブジェクト
   * @throws IllegalStateException {@code node} が {@code SourceTreeNode[A]} 型 でない時
   */
  private def convertItem(node: AnyRef) = node match {
    case node: SourceTreeNode[_] => node.nodeObject.asInstanceOf[A]
    case _ => throw new IllegalStateException("%s must be a SourceTreeNode".format(node))
  }
  
  /**
   * イベントを実装するための DefaultTreeModel 委譲クラス
   */
  private class SourceTreeModelDelegate[A](rootNode: SourceTreeNode[A])
      extends DefaultTreeModel(rootNode, true) {
    import javax.swing.event.TreeModelEvent
    
    override protected def fireTreeNodesChanged(source: AnyRef, path: Array[AnyRef],
        childIndices: Array[Int], children: Array[AnyRef]) {
      fireEvent(path, childIndices, children){_ treeNodesChanged _}
    }
    
    override protected def fireTreeNodesInserted(source: AnyRef, path: Array[AnyRef],
        childIndices: Array[Int], children: Array[AnyRef]) {
      fireEvent(path, childIndices, children){_ treeNodesInserted _}
    }
    
    override protected def fireTreeNodesRemoved(source: AnyRef, path: Array[AnyRef],
        childIndices: Array[Int], children: Array[AnyRef]) {
      fireEvent(path, childIndices, children){_ treeNodesRemoved _}
    }
    
    override protected def fireTreeStructureChanged(source: AnyRef, path: Array[AnyRef],
        childIndices: Array[Int], children: Array[AnyRef]) {
      fireEvent(path, childIndices, children){_ treeStructureChanged _}
    }
    
    private def fireEvent(path: Array[AnyRef], childIndices: Array[Int], children: Array[AnyRef])
        (fire: (TreeModelListener, TreeModelEvent) => Unit) {
      lazy val itemPath: Array[Object] =
        if (path == null) null else path map convertItem
      lazy val itemChildren: Array[Object] = 
        if (children == null) null else children map convertItem
      lazy val e = new TreeModelEvent(self, itemPath, childIndices, itemChildren)
      
      getTreeModelListeners.foreach(fire(_, e))
    }
  }
}

object SourceTreeModel {
  /**
   * 型を保持しながらノードオブジェクトを格納する TreeNode
   */
  private class SourceTreeNode[A](
      val nodeObject: A, override val isLeaf: Boolean) 
      extends DefaultMutableTreeNode(nodeObject, !isLeaf) {
    private var myChildren: Option[IndexedSeq[SourceTreeNode[A]]] = None
    
    /**
     * 子ノードが設定されているか
     */
    def childPrepared = myChildren.isDefined
    
    /**
     * このノードの子ノードを設定する。
     * 設定後 {@code childPrepared} は {@code true} を返すようになる。
     */
    def updateChildren(newChildren: Seq[SourceTreeNode[A]]) {
      myChildren = Some(newChildren.toIndexedSeq)
      removeAllChildren()
      newChildren foreach add
    }
    
    /**
     * SourceTreeNode 型で子ノードを返す
     */
    def getChildren = myChildren.getOrElse(IndexedSeq.empty)
    
    /**
     * 子ノードを未設定状態に戻す。
     */
    def resetChildren() {
      myChildren = None
    }
  }
  
  /** TreePath から [A] 型の配列を作成 */
  private def convertTreePath[A: ClassManifest](path: TreePath): IndexedSeq[A] = {
    val itemClass = implicitly[ClassManifest[A]]
    path.getPath map { obj => 
      if (itemClass.erasure isInstance obj)
        obj.asInstanceOf[A]
      else
        throw new IllegalArgumentException("The path item '%s' must be a %s"
        .format(obj, itemClass.erasure.getName))
    }
  }
  
  /** 子要素インデックス取得 */
  private def getIndices[A](indice: Buffer[Int], source: Seq[SourceTreeNode[A]],
      diff: Seq[SourceTreeNode[A]], offset: Int = 0): Buffer[Int] = {
    diff match {
      case Seq(head, tail @ _*) =>
        val index = source.indexOf(head, offset)
        assert(index >= 0)
        indice += index
        getIndices(indice, source, tail, index + 1)
      case _ => indice
    }
  }
}
