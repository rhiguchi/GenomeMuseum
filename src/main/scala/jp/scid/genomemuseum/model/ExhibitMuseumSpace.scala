package jp.scid.genomemuseum.model

import ca.odell.glazedlists.EventList

/**
 * 自由展示スペース
 */
trait ExhibitMuseumSpace extends MuseumSpace {
//  /** 展示物リスト */
//  def exhibitList: EventList[MuseumExhibit]

  def roomModel: Option[UserExhibitRoom]
}
