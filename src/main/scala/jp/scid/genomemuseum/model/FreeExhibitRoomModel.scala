package jp.scid.genomemuseum.model

/**
 * 追加や削除ができる部屋の構造定義
 */
trait FreeExhibitRoomModel extends ExhibitRoomModel {
  /**
   * 展示物を部屋に追加する。
   */
  def add(element: MuseumExhibit): Boolean
  
  /**
   * 指定の要素番目の展示物を指定の物に置き換える。
   * 
   * @return 置換された展示物
   */
  def set(index: Int, element: MuseumExhibit)
  
  /**
   * 展示物を部屋から除去する。
   * 
   * @return 除去された展示物
   */
  def remove(index: Int): MuseumExhibit
}