package jp.scid.genomemuseum.model

import jp.scid.gui.tree.EditableTreeSource
import ExhibitListBox.BoxType._

/**
 * ExhibitRoom のツリーのモデル
 */
class MuseumStructure extends EditableTreeSource[ExhibitRoom] {
  import MuseumStructure._
  
  private type ExhibitRoomService = TreeDataService[ExhibitListBox]
  
  val localStore = ExhibitRoom("Local")
  val entrez = ExhibitRoom("NCBI Entrez")
  val libraries = ExhibitRoom("Libraries", localStore, entrez)
  
  private var myUserBoxesSource = TreeDataService[ExhibitListBox]()
  
  val userBoxes = new ExhibitRoom {
    def name = "User Lists"
    def children = Nil
    override def toString() = "User Lists"
  }
  
  /** ルートオブジェクト */
  val root = ExhibitRoom("Museum", libraries, userBoxes)
  
  /** 子要素 */
  def childrenFor(parent: ExhibitRoom) = {
    if (isLeaf(parent)) Nil
    else parent match {
      case `userBoxes` =>
        myUserBoxesSource.rootItems.toList
      case parent: ExhibitListBox => Nil
        myUserBoxesSource.getChildren(parent).toList
      case parent => parent.children
    }
  }
  
  /** 末端要素であるか */
  def isLeaf(node: ExhibitRoom) = node match {
    case box: ExhibitListBox => box.boxType match {
      case BoxFolder => false
      case _ => true
    }
    case `localStore` => true
    case `entrez` => true
    case _ => false // TODO 不明な要素には IllegalArgumentException
  }
  
  def update(element: ExhibitListBox, newValue: AnyRef) {
    // TODO update impl
    myUserBoxesSource.save(element)
  }
  
  /** 値の更新 */
  def update(path: IndexedSeq[ExhibitRoom], newValue: AnyRef) = path match {
    case Seq(root, userLists, userBoxPath @ _*) => userBoxPath.lastOption match {
      case Some(element: ExhibitListBox) => 
        update(element, newValue)
      case None =>
        throw new IllegalArgumentException("updating is not allowed")
      }
    case _ =>
      throw new IllegalArgumentException("updating is not allowed")
  }
  
  def userBoxesSource = myUserBoxesSource
  
  def userBoxesSource_=(newSource: ExhibitRoomService) {
    myUserBoxesSource = newSource
  }
}

object MuseumStructure {
  private class Element {
    
  }
  
  private object Element {
    
  }
}
