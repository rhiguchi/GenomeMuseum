package jp.scid.genomemuseum.model

import java.awt.datatransfer.DataFlavor
import java.io.File

import DataFlavor.{javaFileListFlavor, stringFlavor}

/**
 * MuseumExhibit 転送データ
 * 
 * 展示物オブジェクトと、展示物にファイルが設定されている時はファイルも転送される。
 * 文字列も転送可能であり、展示物の {@code toString()} から作成される。
 * 転送文字列の形式を変えたい時は、{@link #stringConverter} プロパティを変更する。
 * 
 * @param exhibits 転送する展示物。
 * @param sourceRoom 展示物が存在していた部屋。部屋からの転出ではない時は {@code None} 。
 */
class DefaultMuseumExhibitTransferData(exhibits: Seq[MuseumExhibit],
    val sourceRoom: Option[UserExhibitRoom]) extends MuseumExhibitTransferData {
  import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
  
  /**
   * 転出元の部屋を設定せずに展示物の転送オブジェクトを生成する。
   * 
   * @param exhibits 転送する展示物。
   */
  def this(exhibits: Seq[MuseumExhibit]) {
    this(exhibits, None)
  }
  
  /** 文字列変換 */
  var stringConverter = (e: MuseumExhibit) => e.toString
  // 改行文字
  private lazy val lineSep = System.getProperty("line.separator")
  
  def museumExhibits = exhibits.toList
  
  def getTransferDataFlavors(): Array[DataFlavor] =
    Array(exhibitDataFlavor, javaFileListFlavor, stringFlavor)
  
  def getTransferData(flavor: DataFlavor) = flavor match {
    case `exhibitDataFlavor` => this
    case `javaFileListFlavor` =>
      import collection.JavaConverters._
      val files = exhibits flatMap (_.sourceFile)
      files.asJava
    case `stringFlavor` =>
      exhibits map stringConverter mkString lineSep + lineSep
    case _ => null
  }
  
  def isDataFlavorSupported(flavor: DataFlavor) = flavor match {
    case `exhibitDataFlavor` => true
    case `stringFlavor` => true
    case `javaFileListFlavor` => true
    case _ => false
  }
}
