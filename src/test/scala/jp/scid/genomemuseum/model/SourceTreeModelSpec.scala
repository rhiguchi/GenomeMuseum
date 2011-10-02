package jp.scid.genomemuseum.model

import org.specs2.mutable._
import org.specs2.mock._
import javax.swing.tree.TreePath
import javax.swing.event.{TreeModelListener}

class SourceTreeModelSpec extends Specification with Mockito {
  def createTreeSourceMock[T <: TreeSource[String]: ClassManifest]() = {
    val treeSource = mock[T]
    treeSource.root returns "root"
    treeSource.isLeaf("root") returns false
    treeSource.childrenFor("root") returns List("A", "B", "C")
    treeSource.isLeaf("A") returns false
    treeSource.childrenFor("A") returns List("A-A", "A-B")
    treeSource.isLeaf("B") returns true
    treeSource.childrenFor("B") returns List("B-A", "B-B", "B-C")
    treeSource.isLeaf("C") returns false
    treeSource.childrenFor("C") returns List()
    treeSource
  }
  
  "SourceTreeModel" should {
    val treeSource = createTreeSourceMock[TreeSource[String]]()
    
    "ルートオブジェクト 取得" in {
      val model = new SourceTreeModel(treeSource)
      model.getRoot must_== "root"
    }
    
    "isLeaf" in {
      "取得" in {
        val model = new SourceTreeModel(treeSource)
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
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.isLeaf("A") must throwA[NoSuchElementException]
      }
      
      "型が違う場合は例外を送出" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.isLeaf(123) must throwA[IllegalArgumentException]
      }
    }
    
    "getChildCount" in {
      "取得" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getChildCount(rootItem) must_== 3
        model.getChildCount("A") must_== 2
        model.getChildCount("C") must_== 0
      }
      
      "未探索時は例外を送出" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getChildCount("A") must throwA[NoSuchElementException]
      }
      
      "型が違う場合は例外を送出" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getChildCount(123) must throwA[IllegalArgumentException]
      }
      
      "isLeaf が true の場合、小項目数は必ず 0" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getChildCount(rootItem) must_== 3
        val itemB = model.getChild(rootItem, 1)
        model.getChildCount(itemB) must_== 0
      }
    }
    
    "getChild" in {
      "取得" in {
        val model = new SourceTreeModel(treeSource)
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
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getChild(rootItem, 0) must_== "A"
        model.getChild(rootItem, 1) must_== "B"
        model.getChild(rootItem, 2) must_== "C"
      }
      
      "未探索時は例外を送出" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getChild("A", 0) must throwA[NoSuchElementException]
      }
      
      "型が違う場合は例外を送出" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getChild(123, 0) must throwA[IllegalArgumentException]
      }
    }
    
    "getIndexOfChild" in {
      "取得" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getChildCount(rootItem)
        val itemA = model.getChild(rootItem, 0)
        model.getIndexOfChild(rootItem, itemA) must_== 0
        val itemB = model.getChild(rootItem, 1)
        model.getIndexOfChild(rootItem, itemB) must_== 1
        val itemC = model.getChild(rootItem, 2)
        model.getIndexOfChild(rootItem, itemC) must_== 2
        
        val itemAA = model.getChild(itemA, 0)
        model.getIndexOfChild(itemA, itemAA) must_== 0
        val itemAB = model.getChild(itemA, 1)
        model.getIndexOfChild(itemA, itemAB) must_== 1
      }
      
      "getChild せずに取得" in {
        val model = new SourceTreeModel(treeSource)
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
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getIndexOfChild(rootItem, "A") must throwA[NoSuchElementException]
      }
      
      "型が違う場合は例外を送出" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        model.getIndexOfChild(123, "A") must throwA[IllegalArgumentException]
      }
    }
    
    "値の変更" in {
      "source の update 呼び出し" in {
        val treeSource = createTreeSourceMock[EditableTreeSource[String]]()
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        val itemC = model.getChild(rootItem, 2)
        
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        val path = new TreePath(Array[Object](rootItem, itemC))
        model.valueForPathChanged(path, "newVal")
        there was one(treeSource).update(
          IndexedSeq("root", "C"), "newVal")
      }
      
      "リスナーの反応" in {
        val treeSource = createTreeSourceMock[EditableTreeSource[String]]()
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        val itemC = model.getChild(rootItem, 2)
        
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        
        val path = new TreePath(Array[Object](rootItem, itemC))
        model.valueForPathChanged(path, "newVal")
        there was one(listener).treeNodesChanged(any)
      }
      
      "source が TreeSource ではリスナーを呼ばない" in {
        val model = new SourceTreeModel(treeSource)
        val rootItem = model.getRoot
        val itemC = model.getChild(rootItem, 2)
        
        val listener = mock[TreeModelListener]
        model addTreeModelListener listener
        val path = new TreePath(Array[Object](rootItem, itemC))
        model.valueForPathChanged(path, "newVal")
        
        there was no(listener).treeNodesChanged(any)
      }
    }
  }
}
