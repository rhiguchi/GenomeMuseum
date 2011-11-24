package jp.scid.bio.ws

import actors.Future

/**
 * ウェブサービスの情報を取得するインターフェイス
 */
trait WebServiceAgent {
  import WebServiceAgent.{Query, Identifier, EntryValues}
  
  /**
   * 検索クエリの該当数を取得。
   * @param query 検索文字列。空白文字列で And 検索
   * @return 検索クエリ
   * @throws IOException 検索中に障害が発生した時
   */
  def getCount(query: String): Query
  
  /**
   * 検索クエリで該当したデータの識別子を取得。
   * @param query 検索クエリ
   * @return 識別子
   * @throws IOException 検索中に障害が発生した時
   */
  def searchIdentifiers(query: Query): IndexedSeq[Identifier]
  
  /**
   * 識別子の属性値を取得する。
   * 結果は identifiers の要素数と同一の長さになる。
   * @return 識別子から取得できるデータの属性値
   * @throws IOException 検索中に障害が発生した時
   */
  def getFieldValues(identifiers: Seq[Identifier]): Iterator[EntryValues]
  
  /**
   * 識別子のデータにアクセスするための URL を取得する。
   * identifier が有効かどうかに関わらずに URL が構成されるため
   * この URL は必ずしもアクセス可能なデータを表すとは限らない。
   */
  def getSource(identifier: Identifier): java.net.URL
}

object WebServiceAgent {
  /**
   * 検索クエリとその該当要素数を保持するクラス
   */
  case class Query(text: String = "", count: Int = 0)
  
  /**
   * 単一のバイオデータと結びつけられた識別子を持つクラス
   */
  case class Identifier(value: String = "")
  
  /**
   * バイオデータの属性値を持つクラス
   * @param identifier この情報にアクセスする識別子
   * @param accession 割り当てられたアクセスコード
   * @param length 配列情報の長さ
   * @param definition 情報の定義文字列
   */
  case class EntryValues (
    identifier: Identifier,
    accession: String = "",
    length: Int = 0,
    definition: String = ""
  )
  
  def apply(): WebServiceAgent = new TogoWebServiceAgent
}
