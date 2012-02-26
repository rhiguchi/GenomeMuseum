package jp.scid.genomemuseum.model

/**
 * 複数の部屋を保持する博物館階層の構造定義。
 */
trait MuseumFloor extends MuseumSpace {
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
