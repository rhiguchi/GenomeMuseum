package jp.scid.genomemuseum.model

/**
 * 複数の部屋を保持できる階層の構造定義。
 */
trait ExhibitFloorModel extends ExhibitRoomModel {
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