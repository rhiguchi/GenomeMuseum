package jp.scid.genomemuseum.model

/**
 * 検索結果表現クラス
 */
case class SearchResult(
  val identifier: String,
  var accession: String = "",
  var definition: String = "",
  var length: Int = 0,
  var done: Boolean = false,
  var sourceUrl: Option[java.net.URL] = None
)
