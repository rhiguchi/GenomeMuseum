package jp.scid.bio.ws

import actors.Future

/**
 * ウェブサービスの情報を取得するインターフェイス
 */
trait WebServiceAgent {
  import WebServiceAgent.{Identifier, EntryValues}
  
  /**
   * クエリの対象要素数を取得。
   * @param query 検索文字列。空白文字列で And 検索
   * @return 取得時に {@code searchIdentifiers} で返すことのできる最大の要素数
   */
  def count(query: String): Future[Int]
  
  /**
   * 検索結果を取得。
   * 結果の配列の長さは limit 未満となる可能性がある。
   * @param query 検索文字列
   * @param offset 取得開始位置
   * @param limit 取得最大数
   * @return 検索結果。項目数は {@code limit} が最大。
   * @throws IllegalArgumentException query で結果が見つからない時
   */
  def searchIdentifiers(query: String, offset: Int, limit: Int): Future[IndexedSeq[Identifier]]
  
  /**
   * 識別子から値を取得する
   * 結果は identifiers の要素数と同一の長さになる。
   */
  def getFieldValuesFor(identifiers: Seq[Identifier]): Future[IndexedSeq[EntryValues]]
}

object WebServiceAgent {
  /**
   * 単一のバイオデータを取得するための識別子を納めたオブジェクト
   * @param value 識別子の文字列表現
   */
  case class Identifier (
    value: String
  )
  
  object Identifier {
    // 空の Identifier を作成
    def empty = Identifier("")
  }
  
  /**
   * バイオデータに登録された情報を納めたオブジェクト
   * @param identifier この情報にアクセスする識別子
   * @param accession 割り当てられたアクセスコード
   * @param length 配列情報の長さ
   * @param definition 情報の定義文字列
   */
  case class EntryValues (
    identifier: Identifier,
    accession: String,
    length: Int = 0,
    definition: String = ""
  )
  
  object EntryValues {
    // 空の EntryValues を作成
    def empty = EntryValues(Identifier.empty, "")
  }
  
  def apply(): WebServiceAgent = new TogoWebServiceAgent
}
