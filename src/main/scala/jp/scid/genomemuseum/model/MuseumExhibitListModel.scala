package jp.scid.genomemuseum.model

import jp.scid.gui.model.ValueModel

object MuseumExhibitListModel {
  val empty = new MuseumExhibitListModel {
    def exhibitList = Nil
    def userExhibitRoom = None
  }
}

/**
 * 展示物を取得できる構造定義
 */
trait MuseumExhibitListModel extends ValueModel[java.util.List[MuseumExhibit]]
    with RoomContentExhibits with PropertyChangeObservable {
  
  override def getValue() = {
    import collection.JavaConverters._
    
    exhibitList.asJava
  }
  
  override def setValue(ehixibiList: java.util.List[MuseumExhibit]) {
    // do nothing
  }
  
  /** {@inheritDoc} */
  def exhibitList: List[MuseumExhibit]
  
  /**
   * 指定した部屋の要素を取得する。
   */
  def userExhibitRoom: Option[UserExhibitRoom]
}

/**
 * 親子関係を構築できる部屋の構造適宜
 */
trait GroupRoomContentsModel extends MuseumExhibitListModel {
  /**
   * この部屋の子要素となれるか
   */
  def canAddChild(room: UserExhibitRoom): Boolean
  
  /**
   * この部屋の子となる部屋を追加する。
   */
  def addChild(room: UserExhibitRoom)
}
