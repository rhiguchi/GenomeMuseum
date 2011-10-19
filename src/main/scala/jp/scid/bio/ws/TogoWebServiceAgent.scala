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

import WebServiceAgent.{Identifier, EntryValues}

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
  
  /**
   * 要素数を取得する。通信中の処理をブロックする。
   */
  def countHeavy(query: String) = {
    if (query.trim.isEmpty) 0
    else getCountFromWeb(query)
  }
  
  /**
   * ウェブから要素数を取得する。通信中の処理をブロックする。
   */
  private def getCountFromWeb(query: String) = {
    val url = countUrl(query)
    val count = using(new BufferedReader(
        new InputStreamReader(contentGet(url)))) { reader =>
      reader.readLine match {
        case null =>
          throw new IllegalStateException("Content is null")
        case str => str.toInt
      }
    }
    
    count
  }
  
  /**
   * 識別子を取得する。通信中の処理をブロックする。
   */
  def searchIdentifiersHeavy(query: String, offset: Int, limit: Int): IndexedSeq[Identifier] = {
    if (query.trim.isEmpty) IndexedSeq.empty
    else getIdentifiersFromWeb(query, offset, limit)
  }
  
  /**
   * ウェブから識別子を取得する。通信中の処理をブロックする。
   */
  private def getIdentifiersFromWeb(query: String, offset: Int, limit: Int) = {
    val url = searchUrl(query, offset, limit)
    val identifiers = using(contentGet(url)) { inst =>
      Source.fromInputStream(inst).getLines.toIndexedSeq map { identifier: String =>
        Identifier(identifier)
      }
    }
    identifiers
  }
  
  /**
   * エントリの値を取得する。通信中の処理をブロックする。
   */
  def getFieldValues(identifiers: Seq[Identifier]): IndexedSeq[EntryValues] = {
    if (identifiers.isEmpty) IndexedSeq.empty
    else getFieldValuesFromWeb(identifiers)
  }
  
  /**
   * ウェブからエントリの値を取得する。通信中の処理をブロックする。
   */
  private def getFieldValuesFromWeb(identifiers: Seq[Identifier]) = {
    val accessionUrl = entryUrl(identifiers, "accession")
    val lengthUrl = entryUrl(identifiers, "length")
    val definitionUrl = entryUrl(identifiers, "definition")
    
    val accessions = using(contentGet(accessionUrl)) { inst =>
      Source.fromInputStream(inst).getLines.toIndexedSeq
    }
    val lengths = using(contentGet(lengthUrl)) { inst =>
      Source.fromInputStream(inst).getLines.toIndexedSeq
    }
    val definitions = using(contentGet(definitionUrl)) { inst =>
      Source.fromInputStream(inst).getLines.toIndexedSeq
    }
    
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
  
  
  // インターフェイスの実装
  def count(query: String) = Futures.future {
    countHeavy(query)
  }

  def searchIdentifiers(query: String, offset: Int, limit: Int) = Futures.future {
    searchIdentifiersHeavy(query, offset, limit)
  }

  def getFieldValuesFor(identifiers: Seq[Identifier]) = Futures.future {
    getFieldValues(identifiers)
  }
  
  /**
   * URL のコンテンツの InputStream を GET メソッドで取得する
   * @throws IllegalArgumentException この URL が 404 を返す時
   */
  @throws(classOf[IllegalStateException])
  private def contentGet(url: String): InputStream = {
    println("contentGet: " + url)
    val method = new HttpGet(url)
    val response: HttpResponse = client.execute(method)
    
    val entity = response.getStatusLine.getStatusCode match {
      case 200 =>
        response.getEntity()
      case 404 =>
        throw new IllegalArgumentException(
          "404 Not found. Maybe URL '%s' was invalid.".format(url))
      case code =>
        val content = using(response.getEntity().getContent()) { inst =>
          val source = Source.fromInputStream(inst)
          source.getLines.mkString("\n")
        }
        throw new IllegalStateException(
          "The response %d is recieved from '%s'. The reason: %s"
              .format(code, url, content))
    }
    
    entity.getContent
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
