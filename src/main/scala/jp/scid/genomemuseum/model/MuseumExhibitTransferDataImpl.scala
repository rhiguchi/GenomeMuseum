package jp.scid.genomemuseum.model

import java.awt.datatransfer.DataFlavor
import java.io.File

import DataFlavor.{javaFileListFlavor, stringFlavor}

private class MuseumExhibitTransferDataImpl(exhibits: Seq[MuseumExhibit],
    val sourceRoom: Option[UserExhibitRoom]) extends MuseumExhibitTransferData {
  import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
  
  def this(exhibits: Seq[MuseumExhibit]) {
    this(exhibits, None)
  }
  
  /** ファイル格納管理オブジェクト */
  var fileStorage: Option[MuseumExhibitStorage] = None
  /** 文字列変換 */
  var stringConverter = (e: MuseumExhibit) => e.toString
  // 改行文字
  private lazy val lineSep = System.getProperty("line.separator")
  
  def museumExhibits = exhibits.toList
  
  def getTransferDataFlavors(): Array[DataFlavor] = {
    val f = Array(exhibitDataFlavor, stringFlavor)
    fileStorage match {
      case Some(_) => f :+ javaFileListFlavor
      case _ => f
    }
  }
  
  def getTransferData(flavor: DataFlavor) = flavor match {
    case `exhibitDataFlavor` => this
    case `javaFileListFlavor` =>
      import collection.JavaConverters._
      val files = exhibits flatMap (fileStorage.get.getSource) map (_.toURI) filter
        (_.getScheme == "file") map (uri => new File(uri))
      files.asJava
    case `stringFlavor` =>
      exhibits map stringConverter mkString lineSep + lineSep
    case _ => null
  }
  
  def isDataFlavorSupported(flavor: DataFlavor) = {
    exhibitDataFlavor.equals(flavor) || stringFlavor.equals(flavor) ||
      javaFileListFlavor.equals(flavor) && fileStorage.nonEmpty
  }
}
