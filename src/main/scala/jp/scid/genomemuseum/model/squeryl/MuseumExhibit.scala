package jp.scid.genomemuseum.model.squeryl

import java.util.Date
import java.io.File
import java.net.URI
import org.squeryl.{annotations, KeyedEntity}
import annotations.Transient

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit, UriFileStorage}
import IMuseumExhibit.FileType

/**
 * MuseumExhibit の Squeryl 用実装
 */
case class MuseumExhibit(
  var name: String = "",
  var sequenceLength: Int = 0,
  var accession: String = "",
  var identifier: String = "",
  var namespace: String = "",
  var version: Option[Int] = None,
  var definition: String = "",
  var source: String = "",
  var organism: String = "",
  var date: Option[Date] = None,
  var sequenceUnit: String = "",
  var moleculeType: String = "",
  var dataSourceUri: String = "",
  var filePath: String = "",
  var fileSize: Long = 0,
  var fileType: FileType.Value = FileType.Unknown
) extends IMuseumExhibit with KeyedEntity[Long] {
  def this() = this("", version = Some(0))
  var id: Long = - MuseumExhibit.newId
  
  @Transient
  def filePathAsURI = sourceFile.get.toURI
  def filePathAsURI_=(uri: URI) {
    filePath = uri.toString
  }
  
  
  /**
   * この展示物の元ファイルを取得する。
   */
  @Transient
  def sourceFile: Option[File] = dataSourceUri match {
    case (null | "") => None
    case _ =>
      val uri = URI.create(dataSourceUri)
      uri.getScheme match {
        case "file" =>
          val file = exhibitFileStorage match {
            case Some(storage) => storage.getFile(uri)
            case None => new File(uri)
          }
          Some(file)
        case _ => None
      }
  }
  
  /**
   * この展示物の元ファイルを設定する。
   * 
   * {@code uri#toString()} の値を {@code sourceFile} に適用する。
   * {@code uri} がライブラリ内のファイルであるとき、 {@code uri} は
   * ライブラリ内の相対パスを表す URI に変換され、 {@code sourceFile} に適用される。
   */
  def sourceFile_=(file: Option[File]) = dataSourceUri = file match {
    case Some(file) => exhibitFileStorage match {
      case Some(storage) => storage.getUri(file).toString
      case None => file.toURI.toString
    }
    case None => ""
  }
  
  /**
   * ライブラリとファイルパスの変換クラス
   */
  @transient
  @Transient
  var exhibitFileStorage: Option[UriFileStorage] = MuseumExhibit.defaultStorage
}

object MuseumExhibit {
  import java.util.concurrent.atomic.AtomicLong
  
  /**
   * MuseumExhibit が作成されるときに設定される標準のファイル管理オブジェクト
   */
  private[squeryl] var defaultStorage: Option[UriFileStorage] = None
  
  private val elmentCount = new AtomicLong
  private def newId = elmentCount.incrementAndGet
}
