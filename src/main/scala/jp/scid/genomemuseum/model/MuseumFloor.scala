package jp.scid.genomemuseum.model

/**
 * 複数の部屋を保持する博物館階層の構造定義。
 */
trait MuseumFloor extends MuseumSpace {
  /**
   * 子部屋のリストを返す
   */
  def childRoomList: java.util.List[_ <: MuseumSpace]
}
