package jp.scid.genomemuseum.model

/**
 * 検索結果表現クラス
 */
case class SearchResult(
  val identifier: String,
  var accession: String = "",
  var definition: String = "",
  var length: Int = 0,
  var status: SearchResult.Status.Value = SearchResult.Status.Pending
)

object SearchResult {
  object Status extends Enumeration {
    type Status = Value
    val Pending = Value(0)
    val Searching = Value(1)
    val Failed = Value(2)
    val Succeed = Value(3)
  }
}
