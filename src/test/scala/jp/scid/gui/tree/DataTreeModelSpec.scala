package jp.scid.gui.tree

import org.specs2._
import mock._

import javax.swing.tree.TreePath
import DataTreeModel.Path
import jp.scid.gui.event.DataTreePathsSelectionChanged

class DataTreeModelSpec extends Specification with Mockito {
  def is = "DataTreeModel" ^
    "selectPath" ^
      "モデルに反映" ! selectPath.s1 ^
    bt ^ "selectPaths" ^
      "モデルに反映" ! selectPaths.s1 ^
    bt ^ "deselect" ^
      "モデルに反映" ! deselect.s1 ^
    bt ^ "selectionModel" ^
      "valueChanged イベント" ! event.s1 ^
      "valueChanged イベントオブジェクト" ! event.s2 ^
    bt ^ "DataTreePathsSelectionChanged" ^
      "DataTreePathsSelectionChanged イベントオブジェクト" ! publish.s1
      
  trait ModelPrep  {
    val source = mock[TreeSource[Symbol]]
    val model = new DataTreeModel(source)
    
    val path1 = Path('root, 'childA, 'childA_A)
    val path2 = Path('root, 'childB)
    
    val tPath1 = new TreePath(Array[Object]('root, 'childA, 'childA_A))
    val tPath2 = new TreePath(Array[Object]('root, 'childB))
    
    def selectionModel = model.selectionModel
    
    def selectionCount = selectionModel.getSelectionCount
  }
    
  def selectPath = new ModelPrep {
    model selectPath path1
    
    def s1 = selectionModel.getSelectionPath must_== tPath1 and
      (selectionCount must_== 1)
  }
    
  def selectPaths = new ModelPrep {
    model selectPaths List(path1, path2)
    
    def s1 = selectionModel.getSelectionPaths must_== Array(tPath1, tPath2) and
      (selectionCount must_== 2)
  }
    
  def deselect = new ModelPrep {
    model selectPaths List(path1, path2)
    model.deselect()
    
    def s1 = selectionCount must_== 0
  }
  
  def event = new ModelPrep {
    import javax.swing.event.{TreeSelectionListener, TreeSelectionEvent}
    
    val listener = mock[TreeSelectionListener]
    // イベントオブジェクト保持
    var event: TreeSelectionEvent = null
    listener.valueChanged(any) answers { e =>
      event = e.asInstanceOf[TreeSelectionEvent]
      e
    }
    
    selectionModel addTreeSelectionListener listener
    
    model selectPath path1
    model selectPath path2
    
    def s1 = there was two(listener).valueChanged(any)
    
    def s2_1 = event.getSource must_== selectionModel
    def s2_2 = event.getPaths().toList must contain(tPath1, tPath2) and have size(2)
    def s2_3 = event.isAddedPath(tPath2) must beTrue
    def s2_4 = event.isAddedPath(tPath1) must beFalse
    def s2 = s2_1 and s2_2 and s2_3 and s2_4
  }
  
  def publish = new ModelPrep {
    import javax.swing.event.{TreeSelectionListener, TreeSelectionEvent}
    
    // リアクション設定とオブジェクト保持
    var event: DataTreePathsSelectionChanged[Symbol] = null
    model.reactions += {
      case e: DataTreePathsSelectionChanged[_] =>
        event = e.asInstanceOf[DataTreePathsSelectionChanged[Symbol]]
    }
    
    model selectPath path1
    model selectPath path2
    
    def s1_1 = event.source must_== model
    def s1_2 = event.newPaths must contain(path2) and have size(1)
    def s1_3 = event.oldPaths must contain(path1) and have size(1)
    def s1 = s1_1 and s1_2 and s1_3
  }
}
