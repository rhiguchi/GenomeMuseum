package jp.scid.genomemuseum.model

import MuseumScheme.ExhibitRoomService
import jp.scid.gui.tree.TreeSource

/**
 * ExhibitRoom のツリーのモデル
 */
class MuseumStructure extends TreeSource[ExhibitRoom] {
  val localStore = ExhibitRoom("Local")
  val libraries = ExhibitRoom("Libraries", localStore)
  
  private var myUserBoxesSource = ExhibitRoomService.empty
  
  val userLists = new ExhibitRoom {
    def name = "User Lists"
    def children = Nil
  }
  
  /** ルートオブジェクト */
  val root = ExhibitRoom("Museum", libraries, userLists)
  
  /** 子要素 */
  def childrenFor(parent: ExhibitRoom) = parent match {
    case `userLists` =>
      myUserBoxesSource.rootItems
    case parent: ExhibitListBox =>
      myUserBoxesSource.childrenFor(parent)
    case parent => parent.children
  }
  
  /** 末端要素であるか */
  def isLeaf(node: ExhibitRoom) = false
  
  def userBoxesSource = myUserBoxesSource
  
  def userBoxesSource_=(newSource: ExhibitRoomService) {
    myUserBoxesSource = newSource
  }
}
