package jp.scid.bio.ws

import java.net.URLEncoder
import java.io.{InputStream, InputStreamReader, BufferedReader}

import io.Source
import actors.{Futures, Future}

import org.apache.http
import http.{client, impl, HttpResponse, HttpEntity}
import client.HttpClient
import client.methods.HttpGet
import impl.client.DefaultHttpClient

import WebServiceAgent.{Query, Identifier, EntryValues}

/**
 * TogoWS を利用した Web サービスアクセス
 */
protected class TogoWebServiceAgent extends WebServiceAgent {
  /** Web アクセスクライアント */
  def client = new DefaultHttpClient
  
  private val urlBase = "http://togows.dbcls.jp/"
  private val searchingURL = urlBase + "search/nuccore/"
  private val entryURL = urlBase + "entry/nuccore/"
  
  /** カウント取得 URL の構築 */
  private def countUrl(query: String) = {
    val urlQuery = URLEncoder.encode(query, "utf-8")
    searchingURL + urlQuery + "/count"
  }
  
  /** 検索 URL の構築 */
  private def searchUrl(query: String, offset: Int, limit: Int) = {
    require(offset >= 0, "offset must be greater than 0")
    require(limit > 0, "limit must be greater than 0")
    
    val urlQuery = URLEncoder.encode(query, "utf-8")
    searchingURL + urlQuery + "/" + (offset + 1) + "," + limit
  }
  
  /** エントリ取得 URL の構築 */
  private def entryUrl(identifiers: Seq[Identifier], key: String) = {
    // ブランクではエラーになるため、文字を置き換える
    def getValidIdVal(e: Identifier) = e.value match {
      case "" => "_"
      case value => value
    }
    val idsValue = identifiers.map(getValidIdVal).mkString(",")
    entryURL + idsValue + "/" + key
  }
  
  /** エントリ取得 URL の取得 */
  private def entryUrl(identifier: Identifier) = {
    // ブランクではエラーになるため、文字を置き換える
    def getValidIdVal(e: Identifier) = e.value match {
      case "" => "_"
      case value => value
    }
    entryURL + identifier.value
  }
  
  /**
   * 要素数を取得する。通信中の処理をブロックする。
   */
  def getCount(query: String) = {
    val count = query.trim.nonEmpty match {
      case true => getContent(countUrl(query.trim)).getLines.next.toInt
      case false => 0
    }
    Query(query, count)
  }
  
  /**
   * 識別子を取得する。通信中の処理をブロックする。
   */
  def searchIdentifiers(query: Query): IndexedSeq[Identifier] = {
    import util.control.Exception.catching
    if (query.count <= 0) IndexedSeq.empty
    else {
      val url = searchUrl(query.text, 0, query.count)
      val lop = catching(classOf[IllegalArgumentException]) opt {
        getContent(url).getLines
      }
      lop.getOrElse(Nil).toIndexedSeq.map
        { identifier: String => Identifier(identifier)}
    }
  }
  
  /**
   * エントリの値を取得する。通信中の処理をブロックする。
   */
  def getFieldValues(identifiers: Seq[Identifier]): IndexedSeq[EntryValues] = {
    if (identifiers.isEmpty) IndexedSeq.empty
    else getFieldValuesFromWeb(identifiers)
  }
  
  /**
   * バイオデータを取得できる URL
   */
  def getSource(identifier: Identifier): java.net.URL =
    new java.net.URL(entryUrl(identifier))
  
  /**
   * ウェブからエントリの値を取得する。通信中の処理をブロックする。
   */
  private def getFieldValuesFromWeb(identifiers: Seq[Identifier]) = {
    val accessionUrl = entryUrl(identifiers, "accession")
    val lengthUrl = entryUrl(identifiers, "length")
    val definitionUrl = entryUrl(identifiers, "definition")
    
    val accessions = getContent(accessionUrl).getLines.toIndexedSeq
    val lengths = getContent(lengthUrl).getLines.toIndexedSeq
    val definitions = getContent(definitionUrl).getLines.toIndexedSeq
    
    if (identifiers.size != accessions.size || identifiers.size != lengths.size ||
         identifiers.size != definitions.size) {
      throw new IllegalStateException("content size was invalid")
    }
    
    val valuesList = Range(0, identifiers.size) map { index =>
      val identifier = identifiers(index)
      val accession = accessions(index)
      val length = lengths(index) match {
        case "" => 0
        case length => length.toInt
      }
      val definition = definitions(index)
      EntryValues(identifier, accession, length, definition)
    }
    
    valuesList.toIndexedSeq
  }

  /**
   * URL のコンテンツを Source として取得する
   * @throws IllegalArgumentException この URL が 404 を返す時
   * @throws IllegalStateException この URL が 200 もしくは 404 ではない返答コードを返す時
   */
  @throws(classOf[IllegalStateException])
  private[ws] def getContent(url: String): io.Source = {
    val method = new HttpGet(url)
    val response: HttpResponse = client.execute(method)
    
    lazy val content = Source.fromInputStream(response.getEntity.getContent)
    
    response.getStatusLine.getStatusCode match {
      case 200 => content
      case 404 =>
        throw new IllegalArgumentException(
          "404 Not found. Maybe URL '%s' was invalid.".format(url))
      case code =>
        throw new IllegalStateException(
          "The response %d is recieved from '%s'. The reason: %s"
              .format(code, url, content.mkString))
    }
  }
}
