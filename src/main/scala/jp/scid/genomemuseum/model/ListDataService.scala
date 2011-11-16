package jp.scid.genomemuseum.model

/**
 * リストデータの提供を行うインターフェイス。
 */
trait ListDataService[A] {
  /**
   * このサービスが持つ全ての要素を取得する。
   * @return 全ての要素の {@code List} 。要素数は {@code #count} と等しい。
   */
  def allElements: List[A]
  
  /**
   * 要素の順序番号を取得する。
   * @param このサービスに含まれる要素。
   * @return 要素の順序番号。このサービスに含まれていない時は {@code -1} 。
   */
  def indexOf(element: A): Int
  
  /**
   * このサービスの要素を作成する
   * @param model 新しく要素を作成する際のひな形。
   *        作成される要素はこのひな形の要素を基に作成されるが、
   *        同一の要素となる必要は無い。無指定の時は {@code None} 。
   * @return 追加・作成された要素オブジェクト。
   */
  def create(): A
  
  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  def remove(element: A): Boolean
  
  /**
   * このデータサービスが持つ要素の更新を通知する。
   * 要素がこのサービスに存在しない時は無視される。
   */
  def save(element: A)
}
