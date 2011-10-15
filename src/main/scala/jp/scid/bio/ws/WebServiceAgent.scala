package jp.scid.bio.ws

trait WebServiceAgent {
  
}

object WebServiceAgent {
  
}

import org.apache.http
import http.{client, impl, HttpResponse, HttpEntity}
import client.HttpClient
import client.methods.HttpGet
import impl.client.DefaultHttpClient
import io.Source

/**
 * TogoWS を利用した Web サービスアクセス
 */
class TogoWebServiceAgent extends WebServiceAgent {
  
  def findEntry(entryId: String): Option[String] = {
    val client = new DefaultHttpClient
    val addr = "http://togows.dbcls.jp/entry/nucleotide/" + entryId
    val method = new HttpGet(addr)
    val response: HttpResponse = client.execute(method)
    
    val entity = response.getStatusLine.getStatusCode match {
      case 200 => Option(response.getEntity())
      case _ => None
    }
    
    entity map { entity =>
      val source = Source.fromInputStream(entity.getContent)
      val content = source.getLines.mkString("\n")
      source.close
      content
    }
  }
}