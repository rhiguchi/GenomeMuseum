package jp.scid.genomemuseum.model

import jp.scid.gui.model.ValueModel
import ca.odell.glazedlists.EventList

/**
 * 部屋データの構造定義
 */
trait ExhibitRoomModel extends ValueModel[java.util.List[MuseumExhibit]]
    with MuseumSpace with PropertyChangeObservable {
  import collection.JavaConverters._

  /** イベント発行 */
  def setValue(newExhibitList: java.util.List[MuseumExhibit]) {
    firePropertyChange("value", null, newExhibitList)
  }
  
  /** この部屋の部屋データ */
  def roomModel: UserExhibitRoom
  
  /** 名前を取得 */
  def name = roomModel.name
  
  /** 展示物リストを返す */
  def exhibitList: List[MuseumExhibit] = getValue match {
    case null => Nil
    case list: EventList[_] => lockWith(list.getReadWriteLock.readLock) {
      list.asScala.toList
    }
    case list => list.asScala.toList
  }
  
  def lockWith[A <% ca.odell.glazedlists.util.concurrent.Lock, B](l: A)(e: => B) = {
    l.lock()
    try e finally l.unlock()
  }
}
