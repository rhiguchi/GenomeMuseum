package jp.scid.genomemuseum.model

import jp.scid.gui.model.ValueModel

/**
 * 部屋データの構造定義
 */
trait ExhibitRoomModel extends ValueModel[java.util.List[MuseumExhibit]]
  with MuseumSpace with PropertyChangeObservable {

  /** イベント発行 */
  def setValue(newExhibitList: java.util.List[MuseumExhibit]) {
    firePropertyChange("value", null, newExhibitList)
  }
  
  /** この部屋の部屋データ */
  def roomModel: UserExhibitRoom
  
  /** 名前を取得 */
  def name = roomModel.name
}
