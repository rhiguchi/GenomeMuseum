package jp.scid.genomemuseum.model

import jp.scid.gui.model.ValueModel

/**
 * 展示物を取得できる構造定義
 */
trait MuseumExhibitListModel extends ValueModel[java.util.List[MuseumExhibit]]
    with PropertyChangeObservable {
  
  /**
   * 指定した部屋の要素を取得する。
   */
  def getRoom: Option[UserExhibitRoom]
}
