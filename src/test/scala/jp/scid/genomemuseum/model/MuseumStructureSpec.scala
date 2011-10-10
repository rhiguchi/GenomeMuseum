package jp.scid.genomemuseum.model

import org.specs2._
import mock._
import ExhibitListBox.BoxType._
import jp.scid.gui.tree.TreeSource

class MuseumStructureSpec extends Specification {
  def newMuseumStructure = {
    new MuseumStructure
  }
  
  def is = "MuseumStructure" ^
    "初期状態" ^
      "userBoxes の子項目数は 0" ! InitialState.s2 ^
    bt ^ "ExhibitListBox の isLeaf 判定" ^
      "libraries は Leaf ではない" ! InitialState.isLeafOfRoot ^
      "libraries は Leaf ではない" ! InitialState.isLeafOfLibraries ^
      "localStore は true" ! InitialState.isLeafOfLocalStore ^
      "entrez は true" ! InitialState.isLeafOfEntrez ^
      "userBoxes は true" ! InitialState.isLeafOfUserBoxes ^
      "ListBox は true" ! InitialState.isLeafListBox ^
      "SmartBox は true" ! InitialState.isLeafSmartBot ^
      "BoxFolder は false" ! InitialState.isLeafBoxFolder ^
    bt ^ "childrenFor メソッド" ^
      "userBoxes 内" ! ChildrenFor().ts1 ^
      "userBoxesSource#rootItems 呼び出し" ! ChildrenFor().callRootItemsOfSource ^
      "BoxFolder から" ! ChildrenFor().ts2 ^
      "userBoxesSource#getChildren(BoxFolder) 呼び出し" ! ChildrenFor().ts2_2 ^
      "ListBox からは子が返されない" ! ChildrenFor().ts3 ^
      "userBoxesSource#getChildren(ListBox) は呼ばない" ! ChildrenFor().ts3_2 ^
      "SmartBox からは子が返されない" ! ChildrenFor().ts4 ^
      "userBoxesSource#getChildren(SmartBox) は呼ばない" ! ChildrenFor().ts4_2 ^
    bt ^ "update メソッド" ^
      "userBoxesSource#update 呼び出し" ! Update().ts1 ^
      "userBoxesSource#update 呼び出し 2" ! Update().ts2
    
  def insertAndGetChildren(parent: ExhibitListBox) = {
    val structure = newMuseumStructure
    val source = structure.userBoxesSource
    
    source add parent
    source.add(ExhibitListBox("child box"), Some(parent))
    
    structure.childrenFor(parent)
  }
  
  object InitialState {
    val structure = new MuseumStructure
    
    def s2 = structure.childrenFor(structure.userBoxes).size must_== 0
    
    def isLeafOfRoot = structure.isLeaf(structure.root) must beFalse
    def isLeafOfLibraries = structure.isLeaf(structure.libraries) must beFalse
    def isLeafOfLocalStore = structure.isLeaf(structure.localStore) must beTrue
    def isLeafOfEntrez = structure.isLeaf(structure.entrez) must beTrue
    def isLeafOfUserBoxes = structure.isLeaf(structure.userBoxes) must beFalse
    
    def isLeafListBox = structure.isLeaf(ExhibitListBox("ListBox", ListBox)) must beTrue
    def isLeafSmartBot = structure.isLeaf(ExhibitListBox("SmartBox", SmartBox)) must beTrue
    def isLeafBoxFolder = structure.isLeaf(ExhibitListBox("BoxFolder", BoxFolder)) must beFalse
  }
  
  case class ChildrenFor() extends Mockito {
    val structure = newMuseumStructure
    val source = spy(TreeDataService[ExhibitListBox]())
    structure.userBoxesSource = source
    // UserBoxes に子を設定
    val boxFolder = ExhibitListBox("BoxFolder", BoxFolder)
    val listBox = ExhibitListBox("ListBox", ListBox)
    val smartBox = ExhibitListBox("SmartBox", SmartBox)
    source add boxFolder
    source add listBox
    source add smartBox
    
    // BoxFolder に子を設定
    val bfChild1 = ExhibitListBox("child box")
    val bfChild2 = ExhibitListBox("child box")
    source.add(bfChild1, Some(boxFolder))
    source.add(bfChild2, Some(boxFolder))
    
    // ListBox に子を設定
    source.add(ExhibitListBox("child box"), Some(listBox))
    
    // SmartBox に子を設定
    source.add(ExhibitListBox("child box"), Some(smartBox))
    
    def ts1 = structure.childrenFor(structure.userBoxes) must
      contain(boxFolder, listBox, smartBox) and have size(3)
    
    def callRootItemsOfSource = {
      structure.childrenFor(structure.userBoxes)
      there was one(source).rootItems
    }
    
    def ts2 = structure.childrenFor(boxFolder) must
      contain(bfChild1, bfChild2) and have size(2)
    
    def ts2_2 = {
      structure.childrenFor(boxFolder)
      there was one(source).getChildren(boxFolder)
    }
    
    def ts3 = structure.childrenFor(listBox) must be empty
    
    def ts3_2 = {
      structure.childrenFor(listBox)
      there was no(source).getChildren(listBox)
    }
    
    def ts4 = structure.childrenFor(smartBox) must be empty
    
    def ts4_2 = {
      structure.childrenFor(smartBox)
      there was no(source).getChildren(smartBox)
    }
  }
  
  case class Update() extends Mockito {
    val structure = newMuseumStructure
    val source = spy(TreeDataService[ExhibitListBox]())
    structure.userBoxesSource = source
    // UserBoxes に子を設定
    val boxFolder = ExhibitListBox("BoxFolder", BoxFolder)
    source add boxFolder
    
    // BoxFolder に子を設定
    val bfChild1 = ExhibitListBox("child box")
    val bfChild2 = ExhibitListBox("child box")
    source.add(bfChild1, Some(boxFolder))
    source.add(bfChild2, Some(boxFolder))
    
    val boxPath = IndexedSeq(structure.root, structure.userBoxes, boxFolder)
    val boxChildPath = boxPath :+ bfChild2
    
    structure.update(boxPath, "update")
    structure.update(boxChildPath, "update")
    
    def ts1 = there was one(source).save(boxFolder)
    
    def ts2 = there was one(source).save(bfChild2)
  }
}
