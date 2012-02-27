package jp.scid.genomemuseum.model

/**
 * 展示室をもつ博物館階層の構造定義。
 */
trait ExhibitMuseumFloor extends ExhibitMuseumSpace with MuseumFloor {
  /**
   * この部屋の子要素となれるか
   */
  def canAddRoom(room: ExhibitMuseumSpace): Boolean
  
  /**
   * この部屋の子となる部屋を追加する。
   */
  def addRoom(room: ExhibitMuseumSpace)
  
  /**
   * 子部屋のリストを返す
   */
  override def childRoomList: java.util.List[ExhibitMuseumSpace]
}
