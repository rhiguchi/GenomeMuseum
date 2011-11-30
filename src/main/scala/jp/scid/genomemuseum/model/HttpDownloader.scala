package jp.scid.genomemuseum.model

import java.net.URL
import java.io.{File, FileInputStream, FileOutputStream, BufferedOutputStream, BufferedInputStream}
import java.util.NoSuchElementException
import java.util.concurrent.Callable

import scala.io.Source
import scala.actors.OutputChannel

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet

object HttpDownloader {
  sealed abstract class Progress
  case class Start(source: HttpDownloader, size: Long) extends Progress
  case class InProgress(source: HttpDownloader, size: Long) extends Progress
  case class Done(source: HttpDownloader) extends Progress
}

class HttpDownloader(url: String) extends Callable[File] {
  import HttpDownloader._
  
  @volatile private var canceled = false
  @volatile var outputChannel: Option[OutputChannel[Progress]] = None
  
  def call() = {
    val client = new DefaultHttpClient
    val method = new HttpGet(url)
    val response = client.execute(method)
    
    // 返答コード 200 ならストリームを取得し、それ以外は例外とする
    val inst = response.getStatusLine().getStatusCode() match {
      case 200 => response.getEntity.getContent
      case 404 => throw new NoSuchElementException(url)
      case code =>
        throw new IllegalStateException(
          "The response %d is recieved from '%s'. The reason: %s"
              .format(code, url, Source.fromInputStream(response.getEntity.getContent).mkString))
    }
    
    // データサイズ通知
    val length = response.getEntity().getContentLength()
    outputChannel.map(_.!(Start(this, length)))
    
    // 一時ファイルに保管
    val dest = File.createTempFile("download-temp", "dat")
    using (new FileOutputStream(dest)) { dest =>
      val outst = new BufferedOutputStream(dest)
      
      using(inst) { source => 
        val inst = new BufferedInputStream(source)
        val buf = new Array[Byte](8196)
        var transfered = 0L
        
        Iterator.continually(inst.read(buf)).takeWhile(read => read != -1) foreach { read =>
          // ファイルへ書き込み
          outst.write(buf, 0, read)
          outst.flush();
          
          // 転送済み量の通知
          transfered += read
          outputChannel.map(_.!(InProgress(this, transfered)))
        }
      }
      
      outputChannel.map(_.!(Done(this)))
      
      if (canceled)
        method.abort()
    }
    
    dest
  }
  
  def cancel = canceled = true
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}
