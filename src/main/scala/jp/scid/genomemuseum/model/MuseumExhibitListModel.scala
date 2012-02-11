package jp.scid.genomemuseum.model

import jp.scid.gui.model.ValueModel

/**
 * 展示物を取得できる構造定義
 */
trait MuseumExhibitListModel extends ValueModel[java.util.List[MuseumExhibit]]
    with RoomContentExhibits with PropertyChangeObservable {
  
  /**
   * 指定した部屋の要素を取得する。
   */
  def getRoom: Option[UserExhibitRoom]
  
  def exhibitList: List[MuseumExhibit] = {
    import collection.JavaConverters._
    getValue.asScala.toList
  }
  
  def userExhibitRoom: Option[UserExhibitRoom] = getRoom
}

/**
 * 親子関係を構築できる部屋の構造適宜
 */
trait GroupRoomContentsModel extends MuseumExhibitListModel {
  def canAddChild(room: UserExhibitRoom): Boolean
  
  def addChild(room: UserExhibitRoom)
}
