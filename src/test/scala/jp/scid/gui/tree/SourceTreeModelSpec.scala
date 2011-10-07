package jp.scid.gui.tree

import org.specs2.mutable._
import org.specs2.mock._
import javax.swing.tree.TreePath
import javax.swing.event.{TreeModelListener, TreeModelEvent}

class SourceTreeModelSpec extends Specification with Mockito {
  class TestTreeSource extends TreeSource[String] {
    val rootObj = "root"
    
    def root = rootObj
    
    def childrenFor(parent: String) = parent match {
      case `rootObj` => List("A", "B", "C")
      case "A" => List("A-A", "A-B")
      case "B" => List("B-A", "B-B")
      case _ => Nil
    }
    
    def isLeaf(node: String) = node match {
      case `rootObj` => false
      case "A" | "C" => false
      case _ => true
    }
  }
  
  class TestEditableTreeSource extends TestTreeSource with EditableTreeSource[String] {
    def update(path: IndexedSeq[String], newValue: AnyRef) {}
  }
  
  def createModel() = new SourceTreeModel(new TestTreeSource)
  
  "SourceTreeModel" should {
    "ルートオブジェクト 取得" in {
      "TreeSource の root と同一オブジェクト" in {
        val model = createModel()
        model.getRoot must_== "root"
      }
      
      "TreeSource の root 呼び出しは一回" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        model.getRoot
        model.getRoot
        there was one(spiedSource).root
      }
    }
    
    "isLeaf" in {
      "取得" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.isLeaf(rootItem) must beFalse
        
        val itemA = model.getChild(rootItem, 0)
        model.isLeaf(itemA) must beFalse
        val itemB = model.getChild(rootItem, 1)
        model.isLeaf(itemB) must beTrue
        val itemC = model.getChild(rootItem, 2)
        model.isLeaf(itemC) must beFalse
      }
      
      "未探索時は例外を送出" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.isLeaf("A") must throwA[NoSuchElementException]
      }
      
      "型が違う場合は例外を送出" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.isLeaf(123) must throwA[IllegalArgumentException]
      }
      
      "TreeSource の isLeaf 呼び出しは一回" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val rootItem = model.getRoot
        model.isLeaf(rootItem)
        model.isLeaf(rootItem)
        
        there was one(spiedSource).isLeaf(rootItem)
        
        val itemA = model.getChild(rootItem, 0)
        model.isLeaf(itemA)
        model.isLeaf(itemA)
        
        there was one(spiedSource).isLeaf(itemA)
      }
    }
    
    "getChildCount" in {
      "取得" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getChildCount(rootItem) must_== 3
        model.getChildCount("A") must_== 2
        model.getChildCount("C") must_== 0
      }
      
      "未探索時は例外を送出" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getChildCount("A") must throwA[NoSuchElementException]
      }
      
      "型が違う場合は例外を送出" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getChildCount(123) must throwA[IllegalArgumentException]
      }
      
      "isLeaf が true の場合、小項目数は必ず 0" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getChildCount(rootItem) must_== 3
        val itemB = model.getChild(rootItem, 1)
        model.getChildCount(itemB) must_== 0
      }
      
      "TreeSource の childrenFor 呼び出しは一回" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val rootItem = model.getRoot
        model.getChildCount(rootItem)
        model.getChildCount(rootItem)
        
        there was one(spiedSource).childrenFor(rootItem)
        
        val itemA = model.getChild(rootItem, 0)
        model.getChildCount(itemA)
        model.getChildCount(itemA)
        
        there was one(spiedSource).childrenFor(itemA)
      }
      
      "leaf オブジェクトの getChildCount を読んでも TreeSource の childrenFor は呼ばれない" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val itemB = model.getChild(model.getRoot, 1)
        model.getChildCount(itemB)
        model.getChildCount(itemB)
        
        there was no(spiedSource).childrenFor(itemB)
      }
    }
    
    "getChild" in {
      "取得" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getChildCount(rootItem)
        val itemA = model.getChild(rootItem, 0)
        itemA must_== "A"
        model.getChild(rootItem, 1) must_== "B"
        model.getChild(rootItem, 2) must_== "C"
        model.getChild(itemA, 0) must_== "A-A"
        model.getChild(itemA, 1) must_== "A-B"
      }
      
      "getChildCount せずに取得" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getChild(rootItem, 0) must_== "A"
        model.getChild(rootItem, 1) must_== "B"
        model.getChild(rootItem, 2) must_== "C"
      }
      
      "未探索時は例外を送出" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getChild("A", 0) must throwA[NoSuchElementException]
      }
      
      "型が違う場合は例外を送出" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getChild(123, 0) must throwA[IllegalArgumentException]
      }
    }
    
    "getIndexOfChild" in {
      "取得" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getChildCount(rootItem)
        model.getIndexOfChild(rootItem, "A") must_== 0
        model.getIndexOfChild(rootItem, "B") must_== 1
        model.getIndexOfChild(rootItem, "C") must_== 2
        
        model.getChildCount("A")
        model.getIndexOfChild("A", "A-A") must_== 0
        model.getIndexOfChild("A", "A-B") must_== 1
      }
      
      "未探索時は例外を送出" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getIndexOfChild(rootItem, "A") must throwA[NoSuchElementException]
      }
      
      "型が違う場合は例外を送出" in {
        val model = createModel()
        val rootItem = model.getRoot
        model.getIndexOfChild(123, "A") must throwA[IllegalArgumentException]
      }
    }
    
    "値の変更" in {
      "source の update 呼び出し" in {
        val spiedSource = spy(new TestEditableTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val rootItem = model.getRoot
        val itemC = model.getChild(rootItem, 2)
        
        val path = new TreePath(Array[Object](rootItem, itemC))
        model.valueForPathChanged(path, "newVal")
        there was one(spiedSource).update(
          IndexedSeq("root", "C"), "newVal")
      }
      
      "treeNodesChanged イベント発行" in {
        val model = new SourceTreeModel(new TestEditableTreeSource)
        
        val rootItem = model.getRoot
        val itemC = model.getChild(rootItem, 2)
        
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        val path = new TreePath(Array[Object](rootItem, itemC))
        model.valueForPathChanged(path, "newVal")
        there was one(listener).treeNodesChanged(any)
      }
      
      "source が TreeSource ではリスナーを呼ばない" in {
        val model = createModel()
        
        val rootItem = model.getRoot
        val itemC = model.getChild(rootItem, 2)
        
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        val path = new TreePath(Array[Object](rootItem, itemC))
        model.valueForPathChanged(path, "newVal")
        
        there was no(listener).treeNodesChanged(any)
      }
      
      "treeNodesChanged のイベントオブジェクト生成" in {
        val spiedSource = spy(new TestEditableTreeSource)
        val model = new SourceTreeModel(spiedSource)
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        var event: TreeModelEvent = null
        listener.treeNodesChanged(any) answers { e =>
          event = e.asInstanceOf[TreeModelEvent]
          e
        }
        
        val rootItem = model.getRoot
        val itemC = model.getChild(rootItem, 2)
        val path = new TreePath(Array[Object](rootItem, itemC))
        model.valueForPathChanged(path, "newVal")
        
        event.getSource must_== model
        event.getPath must_== Array(rootItem)
        event.getChildIndices must_== Array(2)
        event.getChildren must_== Array(itemC)
      }
    }
    
    "reset" in {
      "reset 後に子ノード取得で TreeSource から childrenFor で再読み込み" in {
        val spiedSource = spy(new TestEditableTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val rootItem = model.getRoot
        model.getChildCount(rootItem)
        model.reset(rootItem)
        model.getChildCount(rootItem)
        
        there were two(spiedSource).childrenFor(rootItem)
      }
      
      "reset は指定したノードより上の再最読み込みは行わない" in {
        val spiedSource = spy(new TestEditableTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val rootItem = model.getRoot
        model.getChildCount(rootItem)
        model.getChildCount("A")
        
        model.reset("A")
        model.getChildCount("A")
        
        there were two(spiedSource).childrenFor("A")
        there were one(spiedSource).childrenFor(rootItem)
      }
      
      "開いていないノードでの呼び出しでは何も起こらない" in {
        val spiedSource = spy(new TestEditableTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val rootItem = model.getRoot
        
        model.reset("A")
        
        there were no(spiedSource).childrenFor("A")
      }
      
      "treeStructureChanged 発行" in {
        val model = createModel()
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        val rootItem = model.getRoot
        val itemA = model.getChild(rootItem, 0)
        
        model.reset(itemA)
        
        there was one(listener).treeStructureChanged(any)
      }
      
      "treeStructureChanged のイベントオブジェクト生成" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        var event: TreeModelEvent = null
        listener.treeStructureChanged(any) answers { e =>
          event = e.asInstanceOf[TreeModelEvent]
          e
        }
        
        val rootItem = model.getRoot
        val itemA = model.getChild(rootItem, 0)
        
        model.reset(itemA)
        
        event.getSource must_== model
        event.getPath must_== Array(rootItem, itemA)
      }
    }
    
    "someNodesWereInserted" in {
      "someNodesWereInserted 後に TreeSource から childrenFor で再読み込み" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val rootItem = model.getRoot
        val itemA = model.getChild(rootItem, 0)
        model.getChildCount(itemA)
        model.someChildrenWereInserted(itemA)
        
        there were one(spiedSource).childrenFor(rootItem)
        there were two(spiedSource).childrenFor(itemA)
      }
      
      "開いていないノードでの呼び出しでは TreeSource の childrenFor は呼ばれない" in {
        val spiedSource = spy(new TestEditableTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val itemA = model.getChild(model.getRoot, 0)
        model.someChildrenWereInserted(itemA)
        
        there were no(spiedSource).childrenFor(itemA)
      }
      
      "treeNodesInserted 発行" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        val rootItem = model.getRoot
        val itemA = model.getChild(rootItem, 0)
        model.getChildCount(itemA)
        
        // 子ノード変化
        spiedSource.childrenFor(itemA) returns List("A-C", "A-A", "A-B")
        
        model.someChildrenWereInserted(itemA)
        
        there was one(listener).treeNodesInserted(any)
      }
      
      "treeNodesInserted のイベントオブジェクト生成" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        val listener = mock[TreeModelListener]
        var event: TreeModelEvent = null
        listener.treeNodesInserted(any) answers { e =>
          event = e.asInstanceOf[TreeModelEvent]
          e
        }
        model addTreeModelListener listener
        
        val rootItem = model.getRoot
        val itemA = model.getChild(rootItem, 0)
        model.getChildCount(itemA)
        
        spiedSource.childrenFor(itemA) returns
          List("A-C", "A-A", "A-D", "A-E", "A-B", "A-F")
        
        model.someChildrenWereInserted(itemA)
        
        event.getSource must_== model
        event.getChildren.length must_== 4
        event.getChildren must_== Array("A-C", "A-D", "A-E", "A-F")
        event.getChildIndices.length must_== 4
        event.getChildIndices must_== Array(0, 2, 3, 5)
      }
    }
    
    "someNodesWereRemoved" in {
      "someNodesWereRemoved 後に TreeSource から childrenFor で再読み込み" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val rootItem = model.getRoot
        val itemA = model.getChild(rootItem, 0)
        model.getChildCount(itemA)
        model.someChildrenWereRemoved(itemA)
        
        there were one(spiedSource).childrenFor(rootItem)
        there were two(spiedSource).childrenFor(itemA)
      }
      
      "開いていないノードでの呼び出しでは TreeSource の childrenFor は呼ばれない" in {
        val spiedSource = spy(new TestTreeSource)
        val model = new SourceTreeModel(spiedSource)
        
        val itemA = model.getChild(model.getRoot, 0)
        model.someChildrenWereRemoved(itemA)
        
        there were no(spiedSource).childrenFor(itemA)
      }
      
      "nodesWereRemoved 発行" in {
        val spiedSource = spy(new TestTreeSource)
        spiedSource.childrenFor("A") returns List("A-A", "A-B",
          "A-C", "A-D", "A-E", "A-F")
        val model = new SourceTreeModel(spiedSource)
        
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        val rootItem = model.getRoot
        val itemA = model.getChild(rootItem, 0)
        model.getChildCount(itemA)
        
        // 子ノード変化
        spiedSource.childrenFor(itemA) returns List("A-B", "A-E")
        
        model.someChildrenWereRemoved(itemA)
        
        there was one(listener).treeNodesRemoved(any)
      }
      
      "nodesWereRemoved のイベントオブジェクト生成" in {
        val spiedSource = spy(new TestTreeSource)
        spiedSource.childrenFor("A") returns List("A-A", "A-B",
          "A-C", "A-D", "A-E", "A-F")
        val model = new SourceTreeModel(spiedSource)
        
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        var event: TreeModelEvent = null
        listener.treeNodesRemoved(any) answers { e =>
          event = e.asInstanceOf[TreeModelEvent]
          e
        }
        
        val rootItem = model.getRoot
        val itemA = model.getChild(rootItem, 0)
        model.getChildCount(itemA)
        
        // 子ノード変化
        spiedSource.childrenFor(itemA) returns List("A-B", "A-E")
        
        model.someChildrenWereRemoved(itemA)
        
        event.getSource must_== model
        event.getChildren.length must_== 4
        event.getChildren must_== Array("A-A", "A-C", "A-D", "A-F")
        event.getChildIndices.length must_== 4
        event.getChildIndices must_== Array(0, 2, 3, 5)
      }
    }
  }
}
