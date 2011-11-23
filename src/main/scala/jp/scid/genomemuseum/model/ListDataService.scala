package jp.scid.genomemuseum.model

/**
 * リストデータの提供を行うインターフェイス。
 */
trait ListDataService[A] {
  type ElementClass <: A
  /**
   * このサービスが持つ全ての要素を取得する。
   * @return 全ての要素の {@code List} 。
   */
  def allElements: List[ElementClass]
  
  /**
   * 要素の順序番号を取得する。
   * @param このサービスに含まれる要素。
   * @return 要素の順序番号。このサービスに含まれていない時は {@code -1} 。
   */
  def indexOf(element: ElementClass): Int
  
  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  def remove(element: ElementClass): Boolean
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素がまだサービスに永続化されていない時は、永続化される。
   * 要素がこのサービスに存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def save(element: ElementClass)
}
