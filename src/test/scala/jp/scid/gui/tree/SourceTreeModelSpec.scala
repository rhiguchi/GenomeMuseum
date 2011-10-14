package jp.scid.gui.tree

import org.specs2._
import mock._
import javax.swing.tree.TreePath
import javax.swing.event.{TreeModelListener, TreeModelEvent}

class SourceTreeModelSpec extends Specification with Mockito {
  def is = "SourceTreeModel" ^
    "getRoot" ^
      "ルートオブジェクト取得" ! test.s1 ^
      "TreeSource#root のコールは一回" ! test.s2 ^
    bt ^ "isLeaf" ^
      "取得" ! test.s3 ^
      "TreeSource#isLeaf のコールは一回" ! test.s6 ^
      "未探索時は例外を送出" ! test.s4 ^
      "型が違う場合は例外を送出" ! test.s5 ^
    bt ^ "getChildCount" ^
      "取得" ! test.s7 ^
      "TreeSource#childrenFor のコールは一回" ! test.s8 ^
      "isLeaf が true の項目は 0 を返す" ! test.s9 ^
      "isLeaf が true の項目は TreeSource#childrenFor がコールされない" ! test.s12 ^
      "未探索時は例外を送出" ! test.s10 ^
      "型が違う場合は例外を送出" ! test.s11 ^
    bt ^ "getChild" ^
      "取得" ! test.s13 ^
      "getChildCount せずに取得" ! test.s14 ^
      "TreeSource#childrenFor のコールは一回" ! test.s15 ^
      "範囲を超えたら例外" ! test.s16 ^
      "未探索時は例外を送出" ! test.s17 ^
      "型が違う場合は例外を送出" ! test.s18 ^
    bt ^ "getIndexOfChild" ^
      "取得" ! test.s19 ^
      "存在しない要素は -1 を返す" ! test.s20 ^
      "未探索時は例外を送出" ! test.s21 ^
      "型が違う場合は例外を送出" ! test.s22 ^
    bt ^ "isChildrenExposed" ^
      "取得" ! test.s23 ^
      "開くと true を返す" ! test.s24 ^
      "leaf は開いても false" ! test.s25 ^
      "未探索時は例外を送出" ! test.s26 ^
      "型が違う場合は例外を送出" ! test.s27 ^
    bt ^ "valueForPathChanged" ^
      "TreeSource#update のコール" ! vptest.s1 ^
      "treeNodesChanged イベント発行" ! vptest.s2 ^
      "source が TreeSource の時はリスナーを呼ばない" ! vptest.s3 ^
      "treeNodesChanged のイベントオブジェクト生成" ! vptest.s4 ^
      "未探索時は例外を送出" ! vptest.s5 ^
      "型が違う場合は例外を送出" ! vptest.s6 ^
    bt ^ "reset" ^
      "reset 後に子ノード取得で TreeSource から childrenFor で再読み込み" ! resettest.s1 ^
      "reset は指定したノードより上の再最読み込みは行わない" ! resettest.s2 ^
      "開いていないノードでの呼び出しでは何も起こらない" ! resettest.s3 ^
      "treeStructureChanged 発行" ! resettest.s4 ^
      "treeStructureChanged のイベントオブジェクト生成" ! resettest.s5 ^
    bt ^ "someChildrenWereInserted" ^
      "someChildrenWereInserted 後に TreeSource から childrenFor で再読み込み" ! nwitest.s1 ^
      "開いていないノードでの呼び出しでは TreeSource の childrenFor は呼ばれない" ! nwitest.s2 ^
      "treeNodesInserted 発行" ! nwitest.s3 ^
      "treeNodesInserted のイベントオブジェクト生成" ! nwitest.s4 ^
    bt ^ "someChildrenWereRemoved" ^
      "someChildrenWereRemoved 後に TreeSource から childrenFor で再読み込み" ! nwrtest.s1 ^
      "開いていないノードでの呼び出しでは TreeSource の childrenFor は呼ばれない" ! nwrtest.s2 ^
      "nodesWereRemoved 発行" ! nwrtest.s3 ^
      "nodesWereRemoved のイベントオブジェクト生成" ! nwrtest.s4 
    
  def test = new ModelTest1()
  def vptest = new ValueForPathTest()
  def resettest = new ResetEventTest()
  def nwitest = new NWIEventTest()
  def nwrtest = new NWREventTest()
  
  trait ModelWithEvent extends ModelTrait {
    // リスナ
    val listener = mock[TreeModelListener]
    model addTreeModelListener listener
  }
  
  trait ModelTrait {
    val source = mock[EditableTreeSource[Symbol]]
    makeSourceMock(source)
    
    val model = new SourceTreeModel(source)
    
    model.getRoot
    model.getChildCount('root)
    model.isLeaf('itemB)
    model.isLeaf('itemB)
    
    def makeSourceMock(source: TreeSource[Symbol]) {
      source.root returns 'root
      source.isLeaf('root) returns false
      source.childrenFor('root) returns List('itemA, 'itemB, 'itemC)
      source.isLeaf('itemA) returns true
      source.childrenFor('itemA) returns List('itemA_A, 'itemA_B)
      source.isLeaf('itemB) returns false
      source.childrenFor('itemB) returns List('itemB_B)
      source.isLeaf('itemB_B) returns true
      source.isLeaf('itemC) returns false
      source.childrenFor('itemC) returns List()
    }
  }
  
  class ModelTest1 extends ModelTrait {
    
    def s1 = model.getRoot must_== 'root
    
    def s2 = {
      model.getRoot
      there was one(source).root
    }
    
    def s3_1 = model.isLeaf('root) must beFalse
    def s3_2 = model.isLeaf('itemA) must beTrue
    def s3_3 = model.isLeaf('itemB) must beFalse
    def s3 = s3_1 and s3_2 and s3_3
    
    def s4 = model.isLeaf('itemB_B) must throwA[NoSuchElementException]
    
    def s5 = model.isLeaf("other type") must throwA[IllegalArgumentException]
    
    def s6 = there was one(source).isLeaf('itemB)
    
    def s7_1 = model.getChildCount('root) must_== 3
    def s7_2 = model.getChildCount('itemB) must_== 1
    def s7_3 = model.getChildCount('itemC) must_== 0
    def s7 = s7_1 and s7_2 and s7_3
    
    def s8 = {
      model.getChildCount('itemB)
      model.getChildCount('itemB)
      there was one(source).childrenFor('itemB)
    }
    
    def s9 = model.getChildCount('itemA) must_== 0
    
    def s10 = model.getChildCount('itemB_B) must throwA[NoSuchElementException]
    
    def s11 = model.getChildCount("other type") must throwA[IllegalArgumentException]
    
    def s12 = s9 and {
      there was no(source).childrenFor('itemA)
    }
    
    def s13_1 = model.getChild('root, 1) must_== 'itemB
    def s13_2 = model.getChild('root, 2) must_== 'itemC
    def s13_3 = {
      model.getChildCount('itemB)
      model.getChild('itemB, 0) must_== 'itemB_B
    }
    def s13 = s13_1 and s13_2 and s13_3
    
    def s14 = model.getChild('itemB, 0) must_== 'itemB_B
    
    def s15 = {
      model.getChild('root, 0)
      model.getChild('root, 0)
      model.getChild('itemB, 0)
      model.getChild('itemB, 0)
      there was one(source).childrenFor('itemB) and
        (there was one(source).childrenFor('root))
    }
    
    def s16_1 = model.getChild('root, -1) must throwA[IndexOutOfBoundsException]
    def s16_2 = model.getChild('root, 3) must throwA[IndexOutOfBoundsException]
    def s16 = s16_1 and s16_2
    
    def s17 = model.getChild('itemB_B, 0) must throwA[NoSuchElementException]
    
    def s18 = model.getChild(123, 0) must throwA[IllegalArgumentException]
    
    def s19_1 = model.getIndexOfChild('root, 'itemB) must_== 1
    def s19_2 = model.getIndexOfChild('root, 'itemC) must_== 2
    def s19_3 = {
      model.getChildCount('itemB)
      model.getIndexOfChild('itemB, 'itemB_B) must_== 0
    }
    def s19 = s19_1 and s19_2 and s19_3
    
    def s20_1 = model.getIndexOfChild('itemB, 'root) must_== -1
    def s20_2 = model.getIndexOfChild('itemB, 'root2) must_== -1
    def s20 = s20_1 and s20_2
    
    def s21_1 = model.getIndexOfChild('itemB_B, 'itemB_B_A) must throwA[NoSuchElementException]
    def s21_2 = model.getIndexOfChild('itemX, 'root) must throwA[NoSuchElementException]
    def s21 = s21_1 and s21_2
    
    def s22_1 = model.getIndexOfChild('root, 123) must throwA[IllegalArgumentException]
    def s22_2 = model.getIndexOfChild(123, 'root) must throwA[IllegalArgumentException]
    def s22 = s22_1 and s22_2
    
    def s23 = model.isChildrenExposed('root) must beTrue and
      (model.isChildrenExposed('itemB) must beFalse)
    
    def s24 = {
      model.getChildCount('itemB)
      model.isChildrenExposed('itemB) must beTrue
    }
    
    def s25 = {
      model.getChildCount('itemA)
      model.isChildrenExposed('itemA) must beFalse
    }
    
    def s26 = model.isChildrenExposed('itemB_B) must throwA[NoSuchElementException]
    
    def s27 = model.isChildrenExposed(123) must throwA[IllegalArgumentException]
  }
  
  class ValueForPathTest extends ModelWithEvent {
    // イベントオブジェクトの保持
    var event: TreeModelEvent = null
    listener.treeNodesChanged(any) answers { e =>
      event = e.asInstanceOf[TreeModelEvent]
      e
    }
    
    // 値の編集
    val pathA = new TreePath(Array[Object]('root, 'itemC))
    model.getChildCount(model.getRoot)
    model.valueForPathChanged(pathA, "new value")
    
    def s1 = there was one(source).update(IndexedSeq('root, 'itemC), "new value")
    
    def s2 = there was one(listener).treeNodesChanged(any)
    
    def s3 = {
      val source = mock[TreeSource[Symbol]]
      makeSourceMock(source)
      val model = new SourceTreeModel(source)
      val listener = mock[TreeModelListener]
      model addTreeModelListener listener
      model.getChildCount(model.getRoot)
      model.valueForPathChanged(pathA, "new value")
      there was no(listener).treeNodesChanged(any)
    }
    
    def s4_1 = event.getSource must_== model
    def s4_2 = event.getPath must_== Array('root)
    def s4_3 = event.getChildIndices must_== Array(2)
    def s4_4 = event.getChildren must_== Array('itemC)
    def s4 = s4_1 and s4_2 and s4_3 and s4_4
    
    def s5 = model.valueForPathChanged(new TreePath(Array[Object]('root, 'itemB, 'itemB_B)), "new value") must
      throwA[NoSuchElementException]
    
    def s6 = model.valueForPathChanged(new TreePath(Array[Object]('root, "123")), "new value") must
      throwA[IllegalArgumentException]
  }
  
  class ResetEventTest extends ModelWithEvent {
    // イベントオブジェクトの保持
    var event: TreeModelEvent = null
    listener.treeStructureChanged(any) answers { e =>
      event = e.asInstanceOf[TreeModelEvent]
      e
    }
    
    def s1 = {
      model.reset('root)
      model.getChildCount('root)
      there were two(source).childrenFor('root)
    }
    
    def s2 = {
      model.getChildCount('itemB)
      model.reset('itemB)
      there were two(source).childrenFor('itemB) and
        (there were one(source).childrenFor('root))
    }
    
    def s3 = {
      model.reset('itemB)
      there was no(source).childrenFor('itemB_B)
    }
    
    def s4 = {
      model.reset('root)
      there was one(listener).treeStructureChanged(any)
    }
    
    def s5 = {
      val item = model.getChild(model.getRoot, 1)
      model.reset(item)
      event.getSource must_== model and
        (event.getPath must_== Array('root, item))
    }
  }

  class NWIEventTest extends ModelWithEvent {
    // イベントオブジェクトの保持
    var event: TreeModelEvent = null
    listener.treeNodesInserted(any) answers { e =>
      event = e.asInstanceOf[TreeModelEvent]
      e
    }
    
    val itemB = model.getChild(model.getRoot, 1)
    model.getChildCount(itemB)
    // 子ノード変化
    source.childrenFor(itemB) returns List('itemB_A, 'itemB_B, 'itemB_C, 'itemB_D)
    // 変化通知
    model.someChildrenWereInserted(itemB)
    
    def s1 = there were one(source).childrenFor('root) and
        (there were two(source).childrenFor(itemB))
    
    def s2 = {
      model.someChildrenWereInserted('itemA_A)
      there were no(source).childrenFor('itemA_A)
    }
    
    def s3 = there was one(listener).treeNodesInserted(any)
    
    def s4_1 = event.getSource must_== model
    def s4_2 = event.getChildren.length must_== 3
    def s4_3 = event.getChildren.toArray must_== Array('itemB_A, 'itemB_C, 'itemB_D)
    def s4_4 = event.getChildIndices.length must_== 3
    def s4_5 = event.getChildIndices must_== Array(0, 2, 3)
    def s4 = s4_1 and s4_2 and s4_3 and s4_4 and s4_5
  }

  class NWREventTest extends ModelWithEvent {
    // イベントオブジェクトの保持
    var event: TreeModelEvent = null
    listener.treeNodesRemoved(any) answers { e =>
      event = e.asInstanceOf[TreeModelEvent]
      e
    }
    
    override def makeSourceMock(source: TreeSource[Symbol]) {
      source.root returns 'root
      source.isLeaf('root) returns false
      source.childrenFor('root) returns List('itemA, 'itemB, 'itemC)
      source.isLeaf('itemA) returns false
      source.childrenFor('itemA) returns List('itemA_A, 'itemA_B)
      source.isLeaf('itemB) returns false
      source.childrenFor('itemB) returns List('itemB_A, 'itemB_B, 'itemB_C, 'itemB_D, 'itemB_E)
    }
    
    model.getChildCount(model.getRoot)
    model.getChildCount('itemB)
    // 子ノード変化
    source.childrenFor('itemB) returns List('itemB_B, 'itemB_D)
    // 変化通知
    model.someChildrenWereRemoved('itemB)
    
    def s1 = there were one(source).childrenFor('root) and
        (there were two(source).childrenFor('itemB))
    
    def s2 = {
      model.someChildrenWereRemoved('itemA_A)
      there were no(source).childrenFor('itemA_A)
    }
    
    def s3 = there was one(listener).treeNodesRemoved(any)
    
    def s4_1 = event.getSource must_== model
    def s4_2 = event.getChildren.length must_== 3
    def s4_3 = event.getChildren.toArray must_== Array('itemB_A, 'itemB_C, 'itemB_E)
    def s4_4 = event.getChildIndices.length must_== 3
    def s4_5 = event.getChildIndices must_== Array(0, 2, 4)
    def s4 = s4_1 and s4_2 and s4_3 and s4_4 and s4_5
  }
}
