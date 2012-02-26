package jp.scid.genomemuseum.model.squeryl

import jp.scid.genomemuseum.model.{FreeExhibitRoomModel => IFreeExhibitRoomModel,
  MuseumExhibit => IMuseumExhibit}

/**
 * 展示物の入れ替え用アクセサを持つ部屋のミックスイン
 */
trait FreeExhibitRoomModel extends IFreeExhibitRoomModel {
  /**
   * 要素を追加する。
   * 
   * @param element 保存を行う要素。
   */
  def add(element: IMuseumExhibit) = getValue.add(element)
  
  /**
   * インデックスの展示物を置換する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def set(index: Int, element: IMuseumExhibit) = getValue.set(index, element)
  
  /**
   * 指定項目番目の要素を除去する。
   */
  def remove(index: Int) = getValue.remove(index)
}
