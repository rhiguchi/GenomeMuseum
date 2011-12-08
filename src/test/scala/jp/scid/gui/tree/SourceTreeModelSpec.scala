package jp.scid.gui.tree

import javax.swing.tree.TreePath
import javax.swing.event.{TreeModelListener, TreeModelEvent}

import collection.mutable.ListBuffer

import org.specs2._
import mock._


class SourceTreeModelSpec extends Specification with Mockito {
  private type Factory = TreeSource[Symbol] => SourceTreeModel[Symbol]
  
  def is = "SourceTreeModel" ^
    "ルートオブジェクト取得" ^ canGetRoot(treeModelOf) ^
    "葉要素判定" ^ canGetLeaf(treeModelOf) ^
    "子要素数取得" ^ canGetChildCount(treeModelOf) ^
    "子要素取得" ^ canGetChild(treeModelOf) ^
    "子要素番号取得" ^ canGetIndexOfChild(treeModelOf) ^
    "要素変更通知" ^ valueForPathChangedSpec(treeModelOf) ^
    "リセット" ^ canReset(treeModelOf) ^
    "ノード削除通知" ^ nodeRemovedSpec(treeModelOf) ^
    "ノード変更通知" ^ nodeChangedSpec(treeModelOf) ^
    "子要素削除通知" ^ someChildrenWereRemovedSpec(treeModelOf) ^
    "子要素挿入通知" ^ someChildrenWereInsertedSpec(treeModelOf) ^
    end
  
  def canGetRoot(f: Factory) =
    "ソースから取得" ! getRoot(f).returnsRoot ^
    "TreeSource#root のコールは一回" ! getRoot(f).callsOnlyOnce ^
    bt
  
  def canGetLeaf(f: Factory) =
    "ソースから取得" ! isLeaf(f).returnsFromSource ^
    "TreeSource#isLeaf のコールは一回" ! isLeaf(f).callsOnlyOnce ^
    "未探索時は例外を送出" ! isLeaf(f).throwsNSEE ^
    "型が違う場合は例外を送出" ! isLeaf(f).throwsIAE ^
    bt
  
  def canGetChildCount(f: Factory) =
    "ソースから取得" ! getChildCount(f).returnsFromSource ^
    "TreeSource#childrenFor のコールは一回" ! getChildCount(f).callsOnlyOnce ^
    "isLeaf が true の項目は 0 を返す" ! getChildCount(f).returnsZeroByLeaf ^
    "isLeaf が true の項目は TreeSource#childrenFor がコールされない" ! getChildCount(f).notCallByLeaf ^
    "未探索時は例外を送出" ! getChildCount(f).throwsNSEE ^
    "型が違う場合は例外を送出" ! getChildCount(f).throwsIAE ^
    bt
  
  def canGetChild(f: Factory) =
    "ソースから取得" ! getChild(f).returnsFromSource ^
    "TreeSource#childrenFor のコールは一回" ! getChild(f).callsOnlyOnce ^
    "範囲を超えたら例外" ! getChild(f).throwsIOBE ^
    "未探索時は例外を送出" ! getChild(f).throwsNSEE ^
    "型が違う場合は例外を送出" ! getChild(f).throwsIAE ^
    bt
  
  def canGetIndexOfChild(f: Factory) =
    "取得" ! getIndexOfChild(f).returnsValue ^
    "存在しない要素は -1 を返す" ! getIndexOfChild(f).retursMinusByNoElement ^
    "未探索時は例外を送出" ! getIndexOfChild(f).throwsNSEE ^
    "型が違う場合は例外を送出" ! getIndexOfChild(f).throwsIAE ^
    bt
  
  def valueForPathChangedSpec(f: Factory) =
    "EditableTreeSource#update がコールされる" ! valueForPathChanged(f).callsMethod ^
    "treeNodesChanged イベント発行"  ! valueForPathChanged(f).publishEvent ^
    "treeNodesChanged イベントソースオブジェクト"  ! valueForPathChanged(f).eventObjectSource ^
    "treeNodesChanged イベントパス"  ! valueForPathChanged(f).eventObjectPath ^
    "型が違う場合は例外を送出" ! valueForPathChanged(f).throwsIAE ^
    bt
  
  def canReset(f: Factory) =
    "ソースから再読み込みが行われる" ! reset(f).reloadFromSource ^
    "開いていないノードを指定しても何も起こらない" ! reset(f).notOpen ^
    "指定無しはルートが再読み込みされる" ! reset(f).noArg ^
    "treeStructureChanged 発行" ! reset(f).publishEvent ^
    "treeStructureChanged のイベントソースオブジェクト" ! reset(f).eventSource ^
    "treeStructureChanged のイベントパス" ! reset(f).eventPath ^
    bt
  
  def nodeRemovedSpec(f: Factory) =
    "treeNodesRemoved イベント発行" ! nodeRemoved(f).publishEvent ^
    "treeNodesRemoved イベントソースオブジェクト" ! nodeRemoved(f).eventSource ^
    "treeNodesRemoved イベントパス" ! nodeRemoved(f).eventPath ^
    "treeNodesRemoved イベント子要素番号" ! nodeRemoved(f).eventIndices ^
    "treeNodesRemoved イベント子要素" ! nodeRemoved(f).eventChildren ^
    "開いていない要素ではイベントを発行しない" ! nodeRemoved(f).notPublish ^
    "親ノードから除去されている" ! nodeRemoved(f).removedFromParent ^
    bt
  
  def nodeChangedSpec(f: Factory) =
    "treeNodesChanged イベント発行" ! nodeChanged(f).publishEvent ^
    "treeNodesChanged イベントソースオブジェクト" ! nodeChanged(f).eventSource ^
    "treeNodesChanged イベントパス" ! nodeChanged(f).eventPath ^
    "開いていない要素ではイベントを発行しない" ! nodeRemoved(f).notPublish ^
    bt
  
  def someChildrenWereRemovedSpec(f: Factory) =
    "treeNodesRemoved イベント発行" ! someChildrenWereRemoved(f).publishEvent ^
    "treeNodesRemoved イベントソースオブジェクト" ! someChildrenWereRemoved(f).eventSource ^
    "treeNodesRemoved イベントパス" ! someChildrenWereRemoved(f).eventPath ^
    "treeNodesRemoved イベント子要素番号" ! someChildrenWereRemoved(f).eventIndices ^
    "treeNodesRemoved イベント子要素" ! someChildrenWereRemoved(f).eventChildren ^
    "開いていない要素ではイベントを発行しない" ! someChildrenWereRemoved(f).notPublish ^
    "親ノードから除去されている" ! someChildrenWereRemoved(f).removedFromParent ^
    bt
  
  def someChildrenWereInsertedSpec(f: Factory) =
    "treeNodesInserted イベント発行" ! someChildrenWereInserted(f).publishEvent ^
    "treeNodesInserted イベントソースオブジェクト" ! someChildrenWereInserted(f).eventSource ^
    "treeNodesInserted イベントパス" ! someChildrenWereInserted(f).eventPath ^
    "treeNodesInserted イベント子要素番号" ! someChildrenWereInserted(f).eventIndices ^
    "treeNodesInserted イベント子要素" ! someChildrenWereInserted(f).eventChildren ^
    "開いていない要素ではイベントを発行しない" ! someChildrenWereInserted(f).notPublish ^
    "要素にアクセスできる" ! someChildrenWereInserted(f).getFromParent ^
    bt
  
  def treeModelOf(source: TreeSource[Symbol]) = {
    new SourceTreeModel(source)
  }
  
  def sourceMockOf(root: Symbol) = {
    val source = mock[TreeSource[Symbol]]
    TreeSourceSpec.makeMock(source, root)
  }
  
  def getRoot(f: Factory) = new {
    def modelWith(root: Symbol) = f(sourceMockOf(root))
    
    def returnsRoot = {
      val roots = List('root, 'a, 'b)
      roots map modelWith map (_.getRoot) must_== roots
    }
    
    def callsOnlyOnce = {
      val source = sourceMockOf('root)
      val model = f(source)
      1 to 9 foreach(_ => model.getRoot)
      there was one(source).root
    }
  }
  
  class TestBase(f: Factory) {
    val source = sourceMockOf('root)
    
    lazy val model = f(source)
    
    def makeMockLeaf(pairs: (Symbol, Boolean)*) {
      pairs foreach { case (v, b) => source.isLeaf(v) returns b }
    }
    
    def makeMockChildren(pairs: (Symbol, List[Symbol])*) {
      pairs foreach { case (v, b) => source.childrenFor(v) returns b }
    }
  }
  
  def isLeaf(f: Factory) = new TestBase(f) {
    makeMockChildren('root -> List('a, 'b, 'c))
    makeMockLeaf('a -> true, 'b -> false, 'c -> false)
    
    model.getRoot
    
    def returnsFromSource = {
      model.getChildCount('root)
      List('a, 'b, 'c) map model.isLeaf must_== List(true, false, false)
    }
    
    def callsOnlyOnce = {
      model.getChildCount('root)
      1 to 9 foreach(_ => model.isLeaf('a))
      there was one(source).isLeaf('a)
    }
    
    def throwsNSEE = model.isLeaf('a) must throwA[NoSuchElementException]
    
    def throwsIAE = model.isLeaf("other type") must throwA[IllegalArgumentException]
  }
  
  def getChildCount(f: Factory) = new TestBase(f) {
    makeMockChildren('root -> List('a, 'b, 'c))
    makeMockLeaf('a -> true, 'b -> false, 'c -> false)
    makeMockChildren('a -> List('a_a) , 'b -> List('b_a, 'b_b), 'c -> Nil)
    
    model.getRoot
    
    def returnsFromSource =
      List('root, 'b, 'c) map model.getChildCount must_== List(3, 2, 0)
    
    def callsOnlyOnce = {
      1 to 9 foreach(_ => model.getChildCount('root))
      there was one(source).childrenFor('root)
    }
    
    def returnsZeroByLeaf =
      List('root, 'a) map model.getChildCount drop 1 must_== List(0)
    
    def notCallByLeaf = {
      List('root, 'a) map model.getChildCount
      there was no(source).childrenFor('a)
    }
    
    def throwsNSEE = model.getChildCount('a) must throwA[NoSuchElementException]
    
    def throwsIAE = model.getChildCount("other type") must throwA[IllegalArgumentException]
  }
  
  def getChild(f: Factory) = new TestBase(f) {
    makeMockChildren('root -> List('a, 'b, 'c))
    makeMockLeaf('a -> true, 'b -> false, 'c -> false)
    makeMockChildren('a -> List('a_a) , 'b -> List('b_a, 'b_b), 'c -> Nil)
    
    model.getRoot
    
    def returnsFromSource =
      List('root -> 3, 'b -> 2, 'c -> 0) flatMap { case (s, c) =>
        (0 until c).map(i => model.getChild(s, i)) } must_==
          List('a, 'b, 'c, 'b_a, 'b_b)
    
    def callsOnlyOnce = {
      1 to 9 foreach(_ => model.getChild('root, 0))
      there was one(source).childrenFor('root)
    }
    
    def throwsIOBE = {
      model.getChildCount('root)
      model.getChild('a, 0) must throwA[IndexOutOfBoundsException]
    }
    
    def throwsNSEE = model.getChild('b, 0) must throwA[NoSuchElementException]
    
    def throwsIAE = model.getChild("other type", 0) must throwA[IllegalArgumentException]
  }
  
  def getIndexOfChild(f: Factory) = new TestBase(f) {
    makeMockChildren('root -> List('a, 'b))
    makeMockLeaf('a -> true, 'b -> false)
    makeMockChildren('a -> List('a_a) , 'b -> List('b_a, 'b_b))
    
    model.getRoot
    
    def returnsValue = {
      List('root, 'b) foreach model.getChildCount
      List('a, 'b).map(c => model.getIndexOfChild('root, c)) :::
        List('b_a, 'b_b).map(c => model.getIndexOfChild('b, c)) must_==
          List(0, 1, 0, 1)
    }
    
    def retursMinusByNoElement = {
      model.getIndexOfChild('root, 'c) must_== -1
    }
    
    def throwsNSEE = model.getIndexOfChild('b, 'b) must throwA[NoSuchElementException]
    
    def throwsIAE = model.getIndexOfChild("other type", 'b) must throwA[IllegalArgumentException]
  }
  
  private class TreeModelEventCatcher extends TreeModelListener {
    val events = ListBuffer.empty[TreeModelEvent]
    
    def treeNodesChanged(e: TreeModelEvent) { events += e }
    def treeNodesInserted(e: TreeModelEvent) { events += e }
    def treeNodesRemoved(e: TreeModelEvent) { events += e }
    def treeStructureChanged(e: TreeModelEvent) { events += e }
  }
  
  def valueForPathChanged(f: Factory) = new TestBase(f) {
    override val source = mock[EditableTreeSource[Symbol]]
    TreeSourceSpec.makeMock(source, 'root)
    makeMockChildren('root -> List('a, 'b))
    
    private val eventCatcher = new TreeModelEventCatcher
    model.addTreeModelListener(eventCatcher)
    
    model.getRoot
    model.getChildCount('root)
    val path = new TreePath(Array[Object]('root, 'b))
    model.valueForPathChanged(path, 'newValue)
    
    def callsMethod =  there was one(source).update(Vector('root, 'b), 'newValue)
    
    def publishEvent = eventCatcher.events must haveSize(1)
    
    def eventObjectSource = eventCatcher.events.headOption.map(_.getSource) must beSome(model)
    
    def eventObjectPath = eventCatcher.events.headOption.map(_.getTreePath) must
      beSome(path.getParentPath)
    
    def throwsIAE = model.valueForPathChanged(
      new TreePath(Array[Object]('root, "other type")), 'newValue) must
      throwA[IllegalArgumentException]
  }
  
  def reset(f: Factory) = new TestBase(f) {
    makeMockChildren('root -> List('a, 'b))
    makeMockLeaf('a -> true, 'b -> false)
    makeMockChildren('a -> List('a_a) , 'b -> List('b_a, 'b_b))
    
    model.getRoot
    model.getChildCount('root)
    model.getChildCount('b)
    
    // ソースが変更
    makeMockLeaf('a -> true, 'b -> false)
    makeMockChildren('root -> List('a), 'a -> List('a_a) , 'b -> List('b_a, 'b_b, 'b_c))
    
    // イベント捕獲
    private val eventCatcher = new TreeModelEventCatcher
    model.addTreeModelListener(eventCatcher)
    
    val path = new TreePath(Array[Object]('root, 'b))
    
    def reloadFromSource = {
      model.reset('b)
      List('root, 'b) map model.getChildCount must_== List(2, 3)
    }
    
    def notOpen = {
      model.reset('b_c)
      List('root, 'b) map model.getChildCount must_== List(2, 2)
    }
    
    def noArg = {
      model.reset()
      List('root, 'a) map model.getChildCount must_== List(1, 0)
    }
    
    def publishEvent = {
      model.reset('b)
      model.reset()
      eventCatcher.events must haveSize(2)
    }
    
    def eventSource = {
      model.reset('b)
      model.reset()
      eventCatcher.events.map(_.getSource) must_== List(model, model)
    }
    
    def eventPath = {
      model.reset('b)
      model.reset()
      eventCatcher.events.map(_.getTreePath) must_== List(path, path.getParentPath)
    }
  }
  
  def treePathOf(items: Symbol*) = new TreePath(items.toArray[Object])
  
  def nodeRemoved(f: Factory) = new TestBase(f) {
    makeMockChildren('root -> List('a, 'b))
    makeMockLeaf('a -> true, 'b -> false)
    makeMockChildren('a -> List('a_a) , 'b -> List('b_a, 'b_b))
    
    model.getRoot
    model.getChildCount('root)
    model.getChildCount('b)
    
    // イベント捕獲
    private val eventCatcher = new TreeModelEventCatcher
    model.addTreeModelListener(eventCatcher)
    
    def publishEvent = {
      List('b_a, 'a) foreach model.nodeRemoved
      eventCatcher.events must haveSize(2)
    }
    
    def eventSource = {
      List('b_a, 'a) foreach model.nodeRemoved
      eventCatcher.events.map(_.getSource) must_== List(model, model)
    }
    
    def eventPath = {
      List('b_a, 'a) foreach model.nodeRemoved
      eventCatcher.events.map(_.getTreePath) must_==
        List(treePathOf('root, 'b), treePathOf('root))
    }
    
    def eventIndices = {
      List('b_b, 'a) foreach model.nodeRemoved
      eventCatcher.events.flatMap(_.getChildIndices) must_== List(1, 0)
    }
    
    def eventChildren = {
      List('b_b, 'a) foreach model.nodeRemoved
      eventCatcher.events.flatMap(_.getChildren) must_== List('b_b, 'a)
    }
    
    def notPublish = {
      List('a_a) foreach model.nodeRemoved
      eventCatcher.events must beEmpty
    }
    
    def removedFromParent = {
      List('b_b) foreach model.nodeRemoved
      model.getIndexOfChild('b, 'b_b) must_== -1
    }
  }
  
  def nodeChanged(f: Factory) = new TestBase(f) {
    makeMockChildren('root -> List('a, 'b))
    makeMockLeaf('a -> true, 'b -> false)
    makeMockChildren('a -> List('a_a) , 'b -> List('b_a, 'b_b))
    
    model.getRoot
    model.getChildCount('root)
    model.getChildCount('b)
    
    // イベント捕獲
    private val eventCatcher = new TreeModelEventCatcher
    model.addTreeModelListener(eventCatcher)
    
    def publishEvent = {
      List('b_a, 'a) foreach model.nodeChanged
      eventCatcher.events must haveSize(2)
    }
    
    def eventSource = {
      List('b_a, 'a) foreach model.nodeChanged
      eventCatcher.events.map(_.getSource) must_== List(model, model)
    }
    
    def eventPath = {
      List('b_a, 'a) foreach model.nodeChanged
      eventCatcher.events.map(_.getTreePath) must_==
        List(treePathOf('root, 'b), treePathOf('root))
    }
    
    def notPublish = {
      List('a_a) foreach model.nodeChanged
      eventCatcher.events must beEmpty
    }
  }
  
  def someChildrenWereRemoved(f: Factory) = new TestBase(f) {
    makeMockChildren('root -> List('a, 'b))
    makeMockLeaf('a -> false, 'b -> false)
    makeMockChildren('a -> List('a_a, 'a_b, 'a_c), 'b -> List('b_a, 'b_b))
    
    List(model.getRoot, 'a, 'b) foreach model.getChildCount
    // ソース変化
    makeMockChildren('root -> List('a), 'a -> List('a_a, 'a_c),
      'b -> List())
    
    // イベント捕獲
    private val eventCatcher = new TreeModelEventCatcher
    model.addTreeModelListener(eventCatcher)
    
    List('b, 'a, 'root) foreach model.someChildrenWereRemoved
    
    def publishEvent = eventCatcher.events must haveSize(3)
    
    def eventSource =
      eventCatcher.events.map(_.getSource) must_== List(model, model, model)
    
    def eventPath = eventCatcher.events.map(_.getTreePath) must_==
      List(treePathOf('root, 'b), treePathOf('root, 'a), treePathOf('root))
      
    def eventIndices =
      eventCatcher.events.flatMap(_.getChildIndices) must_== List(0, 1, 1, 1)
    
    def eventChildren =
      eventCatcher.events.flatMap(_.getChildren) must_==  List('b_a, 'b_b, 'a_b, 'b)
      
    def notPublish = {
      List('c) foreach model.nodeChanged
      eventCatcher.events must haveSize(3)
    }
    
    def removedFromParent = model.getChildCount('root) must_== 1
  }
  
  
  def someChildrenWereInserted(f: Factory) = new TestBase(f) {
    makeMockChildren('root -> List('a))
    makeMockLeaf('a -> false, 'b -> false)
    makeMockChildren('a -> List('a_a, 'a_c), 'b -> List('b_a, 'b_b))
    
    List(model.getRoot, 'a) foreach model.getChildCount
    // ソース変化
    makeMockChildren('root -> List('a, 'b), 'a -> List('a_a, 'a_b, 'a_c))
    
    // イベント捕獲
    private val eventCatcher = new TreeModelEventCatcher
    model.addTreeModelListener(eventCatcher)
    
    List('root, 'a, 'b) foreach model.someChildrenWereInserted
    
    def publishEvent = eventCatcher.events must haveSize(2)
    
    def eventSource =
      eventCatcher.events.map(_.getSource) must_== List(model, model)
    
    def eventPath = eventCatcher.events.map(_.getTreePath) must_==
      List(treePathOf('root), treePathOf('root, 'a))
      
    def eventIndices =
      eventCatcher.events.flatMap(_.getChildIndices) must_== List(1, 1)
    
    def eventChildren =
      eventCatcher.events.flatMap(_.getChildren) must_==  List('b, 'a_b)
      
    def notPublish = {
      List('c) foreach model.nodeChanged
      eventCatcher.events must haveSize(2)
    }
    
    def getFromParent =
      List('root, 'a, 'b) map model.getChildCount must_== List(2, 3, 2)
  }
}
