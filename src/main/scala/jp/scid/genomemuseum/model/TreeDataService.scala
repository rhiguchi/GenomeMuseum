package jp.scid.genomemuseum.model

/**
 * ツリー構造のデータを扱うためのトレイト
 */
trait TreeDataService[A] {
  /**
   * 子要素を返す。
   * @param parent 親要素。{@code None} で、ルート要素（どの親にも属さない要素）を返す。
   * @return 子要素。
   */
  def getChildren(parent: Option[A]): Iterable[A]
  
  /**
   * 親要素を取得する
   * @param element 子要素
   * @return 親要素。属する親が無いの時は {@code None} 。
   */
  def getParent(element: A): Option[A]
  
  /**
   * 要素の更新を通知する。
   * @param element 削除する要素。
   * @return 削除された要素数。
   */
  def save(element: A)
}
