package jp.scid.genomemuseum.model

import ca.odell.glazedlists.EventList

/**
 * 自由展示スペース
 */
trait ExhibitMuseumSpace extends ExhibitRoomModel {
  /**
   * 名前を設定する
   */
  def name_=(newName: String) = roomModel.name = newName
}
