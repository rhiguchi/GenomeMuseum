package jp.scid.genomemuseum.model

import jp.scid.gui.model.ValueModel

/**
 * 部屋データの構造定義
 */
trait ExhibitRoomModel extends ValueModel[java.util.List[_ <: MuseumExhibit]]
  with PropertyChangeObservable {

  /** イベント発行 */
  def setValue(newExhibitList: java.util.List[_ <: MuseumExhibit]) {
    firePropertyChange("value", null, newExhibitList)
  }
  
  /** このデータの部屋 */
  def sourceRoom: Option[ExhibitRoom]
  
  /** 展示物リスト */
  def exhibitList: List[MuseumExhibit] = {
    import collection.JavaConverters._
    getValue.asScala.toList
  }
}
