package jp.scid.genomemuseum.gui

import org.specs2._
import mock._
import jp.scid.genomemuseum.model.{MuseumStructure, ExhibitListBox,
  TreeDataService}
import ExhibitListBox.BoxType._

class MuseumSourceModelSpec extends Specification {
  def is = "MuseumSourceModel" ^
    "addListBox" ^
      "ListBox のオブジェクト生成" ! AddListBoxSpec().s1 ^
      "TreeDataService#add(ListBox) 呼び出し" ! AddListBoxSpec().s2 ^
      "treeSource に反映" ! AddListBoxSpec().s3 ^
      "treeModel の更新" ! AddListBoxSpec().s4 ^
    bt ^ "addListBox (子要素としての追加)" ^
      "ListBox のオブジェクト生成" ! AddListBoxToParentSpec().s1 ^
      "TreeDataService#add(ListBox, Some) 呼び出し" ! AddListBoxToParentSpec().s2 ^
      "treeModel に反映" ! AddListBoxToParentSpec().s3 ^
      "ListBox は親にできない" ! AddListBoxToParentSpec().s4 ^
      "SmartBox は親にできない" ! AddListBoxToParentSpec().s5 ^
      "treeModel の更新" ! AddListBoxToParentSpec().s6 ^
    bt ^ "addSmartBox" ^
      "SmartBox のオブジェクト生成" ! AddSmartBoxSpec().s1 ^
      "TreeDataService#add(SmartBox) 呼び出し" ! AddSmartBoxSpec().s2 ^
      "treeSource に反映" ! AddSmartBoxSpec().s3 ^
      "treeModel の更新" ! AddSmartBoxSpec().s4 ^
    bt ^ "addSmartBox (子要素としての追加)" ^
      "ListBox のオブジェクト生成" ! AddSmartBoxToParentSpec().s1 ^
      "TreeDataService#add(ListBox, Some) 呼び出し" ! AddSmartBoxToParentSpec().s2 ^
      "treeModel に反映" ! AddSmartBoxToParentSpec().s3 ^
      "ListBox は親にできない" ! AddSmartBoxToParentSpec().s4 ^
      "SmartBox は親にできない" ! AddSmartBoxToParentSpec().s5 ^
      "treeModel の更新" ! AddSmartBoxToParentSpec().s6 ^
    bt ^ "addBoxFolder" ^
      "BoxFolder のオブジェクト生成" ! AddBoxFolderSpec().s1 ^
      "TreeDataService#add(BoxFolder) 呼び出し" ! AddBoxFolderSpec().s2 ^
      "treeSource に反映" ! AddBoxFolderSpec().s3 ^
      "treeModel の更新" ! AddBoxFolderSpec().s4 ^
    bt ^ "addBoxFolder (子要素としての追加)" ^
      "ListBox のオブジェクト生成" ! AddBoxFolderToParentSpec().s1 ^
      "TreeDataService#add(ListBox, Some) 呼び出し" ! AddBoxFolderToParentSpec().s2 ^
      "treeSource に反映" ! AddBoxFolderToParentSpec().s3 ^
      "ListBox は親にできない" ! AddBoxFolderToParentSpec().s4 ^
      "SmartBox は親にできない" ! AddBoxFolderToParentSpec().s5 ^
      "treeModel の更新" ! AddBoxFolderToParentSpec().s6 ^
    bt ^ "removeElementFromParent" ^
      "TreeDataService#remove 呼び出し" ! RemoveBoxSpec().s1 ^
      "treeSource に反映" ! RemoveBoxSpec().s2 ^
      "treeModel の更新" ! RemoveBoxSpec().s3 ^
      "削除した要素は treeModel からアクセスできない" ! RemoveBoxSpec().s4
  
  trait Base extends Mockito {
    import jp.scid.gui.tree.SourceTreeModel
    
    val treeSource = new MuseumStructure
    
    val model = new MuseumSourceModel(treeSource)
    
    model.userBoxesSource = spy(TreeDataService[ExhibitListBox]())
    
    protected def boxesSource = treeSource.userBoxesSource
    
    protected def userBoxesNode = treeSource.userBoxes
    
    protected def treeModel = model.treeModel.asInstanceOf[SourceTreeModel[ExhibitListBox]]
  }
  
  trait AddBoxSpec extends Base {
    treeModel.getChildCount(treeModel.getRoot)
    assert(treeModel.getChildCount(userBoxesNode) == 0)
    
    val newBox = createBox
    
    protected def createBox: ExhibitListBox
    
    def s2 = there was one(boxesSource).add(newBox, None)
    
    def s3 = treeSource.childrenFor(userBoxesNode) must contain(newBox) and
      have size(1)
    
    def s4 = treeModel.getChildCount(userBoxesNode).must_==(1) and
      treeModel.getChild(userBoxesNode, 0).must_==(newBox)
  }
  
  case class AddListBoxSpec() extends AddBoxSpec {
    def createBox = model.addListBox("List Box")
    
    def s1 = newBox.name must_== "List Box" and
      (newBox.boxType must_== ListBox)
  }
  
  case class AddSmartBoxSpec() extends AddBoxSpec {
    def createBox = model.addSmartBox("Smart Box")
    
    def s1 = newBox.name must_== "Smart Box" and
      (newBox.boxType must_== SmartBox)
  }
  
  case class AddBoxFolderSpec() extends AddBoxSpec {
    def createBox = model.addBoxFolder("Box Folder")
    
    def s1 = newBox.name must_== "Box Folder" and
      (newBox.boxType must_== BoxFolder)
  }
  
  trait AddChildBoxSpec extends Base {
    val listBox = model.addListBox("List Box")
    val smartBox = model.addSmartBox("Smart Box")
    val boxFolder = model.addBoxFolder("Box Folder")
    
    // boxFolder ノードには子が無いことの確認
    treeModel.getChildCount(treeModel.getRoot)
    assert(treeModel.getChildCount(userBoxesNode) == 3)
    assert(treeModel.getChildCount(boxFolder) == 0)
    
    val newBox = createBox
    
    protected def createBox: ExhibitListBox
    
    def childrenForBoxFolder = treeSource.childrenFor(boxFolder)
    def childrenForListBox = treeSource.childrenFor(listBox)
    def childrenForSmartBox = treeSource.childrenFor(smartBox)
    
    def s2 = there was one(boxesSource).add(newBox, Some(boxFolder))
    
    def s3 =
      treeSource.childrenFor(boxFolder) must contain(newBox) and have size(1)
    
    def s4_2 = childrenForListBox must be empty
    
    def s5_2 = childrenForSmartBox must be empty
    
    def s6 = treeModel.getChildCount(boxFolder).must_==(1) and
      treeModel.getChild(boxFolder, 0).must_==(newBox)
  }
  
  case class AddListBoxToParentSpec() extends AddChildBoxSpec {
    def createBox = model.addListBox("List Box", boxFolder)
    
    def s1 = newBox.name must_== "List Box" and
      (newBox.boxType must_== ListBox)
    
    def s4 = model.addListBox("List Box", listBox) must
      throwA[IllegalArgumentException] and s4_2
    
    def s5 = model.addListBox("List Box", smartBox) must
      throwA[IllegalArgumentException] and s5_2
  }
  
  case class AddSmartBoxToParentSpec() extends AddChildBoxSpec {
    def createBox = model.addSmartBox("Smart Box", boxFolder)
    
    def s1 = newBox.name must_== "Smart Box" and
      (newBox.boxType must_== SmartBox)
    
    def s4 = model.addSmartBox("Smart Box", listBox) must
      throwA[IllegalArgumentException] and s4_2
    
    def s5 = model.addSmartBox("Smart Box", smartBox) must
      throwA[IllegalArgumentException] and s5_2
  }
  
  case class AddBoxFolderToParentSpec() extends AddChildBoxSpec {
    def createBox = model.addBoxFolder("Box Folder", boxFolder)
    
    def s1 = newBox.name must_== "Box Folder" and
      (newBox.boxType must_== BoxFolder)
    
    def s4 = model.addBoxFolder("Box Folder", listBox) must
      throwA[IllegalArgumentException] and s4_2
    
    def s5 = model.addBoxFolder("Box Folder", smartBox) must
      throwA[IllegalArgumentException] and s5_2
  }
    
  case class RemoveBoxSpec extends Base {
    val listBox = model.addListBox("List Box")
    val smartBox = model.addSmartBox("Smart Box")
    val boxFolder = model.addBoxFolder("Box Folder")
    val boxChild1 = model.addListBox("Child1", boxFolder)
    val boxChild2 = model.addListBox("Child2", boxFolder)
    val boxChild3 = model.addSmartBox("Child3", boxFolder)
    val boxFolder2 = model.addBoxFolder("Box Folder2")
    val box2Child1 = model.addListBox("Child1", boxFolder2)
    val box2Child2 = model.addListBox("Child2", boxFolder2)
    
    // boxFolder ノードに子が存在していることの確認
    treeModel.getChildCount(treeModel.getRoot)
    assert(treeModel.getChildCount(userBoxesNode) == 4)
    assert(treeModel.getChildCount(boxFolder) == 3)
    assert(treeModel.getChildCount(boxFolder2) == 2)
    
    model.removeElementFromParent(listBox)
    model.removeElementFromParent(boxChild2)
    model.removeElementFromParent(boxFolder2)
    
    def childrenForBoxFolder = treeSource.childrenFor(boxFolder)
    def childrenForListBox = treeSource.childrenFor(listBox)
    def childrenForSmartBox = treeSource.childrenFor(smartBox)
    
    def s1 = (there was one(boxesSource).remove(listBox)) and
      (there was one(boxesSource).remove(boxChild2)) and
      (there was one(boxesSource).remove(boxFolder2))
    
    def s2 = treeSource.childrenFor(userBoxesNode) must
      contain(smartBox, boxFolder) and have size(2) and
      (treeSource.childrenFor(boxFolder) must
      contain(boxChild1, boxChild3) and have size(2))
    
    def s3 = treeModel.getChildCount(userBoxesNode) must_== 2 and
      treeModel.getChildCount(boxFolder).must_==(2)
    
    def s4 = treeModel.isLeaf(listBox) must throwA[NoSuchElementException] and
      (treeModel.isLeaf(boxChild2) must throwA[NoSuchElementException]) and
      (treeModel.isLeaf(boxFolder2) must throwA[NoSuchElementException]) and
      (treeModel.getChildCount(boxFolder2) must throwA[NoSuchElementException])
  }
}

