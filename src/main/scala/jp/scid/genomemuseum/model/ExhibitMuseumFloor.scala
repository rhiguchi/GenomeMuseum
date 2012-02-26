package jp.scid.genomemuseum.model

/**
 * 展示室をもつ博物館階層の構造定義。
 */
trait ExhibitMuseumFloor extends MuseumFloor {
  /**
   * この部屋の子要素となれるか
   */
  def canAddRoom(room: ExhibitRoomModel): Boolean
  
  /**
   * この部屋の子となる部屋を追加する。
   */
  def addRoom(room: ExhibitRoomModel)
  
  /**
   * 子部屋のリストを返す
   */
  def childRoomList: java.util.List[ExhibitRoomModel]
}
