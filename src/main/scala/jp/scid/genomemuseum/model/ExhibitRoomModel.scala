package jp.scid.genomemuseum.model

import jp.scid.gui.model.ValueModel

/**
 * 部屋データの構造定義
 */
trait ExhibitRoomModel extends ValueModel[java.util.List[MuseumExhibit]]
  with PropertyChangeObservable {

  /** 展示物リストを取得 */
  def exhibitList = {
    import collection.JavaConverters._
    getValue.asScala.toIndexedSeq
  }
  
  @deprecated("", "not use")
  def setValue(ehixibiList: java.util.List[MuseumExhibit]) {
    // do nothing
  }
  
  /** このデータの部屋 */
  def sourceRoom: ExhibitRoom
}