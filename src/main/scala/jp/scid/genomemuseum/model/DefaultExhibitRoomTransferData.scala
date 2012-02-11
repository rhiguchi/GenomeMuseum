package jp.scid.genomemuseum.model

import java.awt.datatransfer.{DataFlavor, Transferable}
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
case class DefaultExhibitRoomTransferData(
    museumExhibits: List[MuseumExhibit],
    sourceRoom: UserExhibitRoom) extends Transferable {
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  
  def getTransferDataFlavors(): Array[DataFlavor] =
    Array(exhibitRoomDataFlavor, javaFileListFlavor)
  
  def getTransferData(flavor: DataFlavor) = flavor match {
    case `exhibitRoomDataFlavor` => this
    case `javaFileListFlavor` =>
      import collection.JavaConverters._
      val files = museumExhibits flatMap (_.sourceFile)
      files.asJava
    case _ => null
  }
  
  def isDataFlavorSupported(flavor: DataFlavor) = flavor match {
    case `exhibitRoomDataFlavor` => true
    case `javaFileListFlavor` => true
    case _ => false
  }
}
