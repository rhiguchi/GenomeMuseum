package jp.scid.genomemuseum.model

import java.io.{File, FileInputStream, FileOutputStream, IOException}
import java.net.URI

class LibraryFileManager(baseDir: File) {
  import LibraryFileManager._
  
  if (!baseDir.exists) baseDir.mkdirs
  
  val uriScheme = "gmlib"
  
  /**
   * ファイルをライブラリにコピーして保存
   * {@code exhibit.filePath} に保管場所が適用される
   */
  @throws(classOf[IOException])
  def store(exhibit: MuseumExhibit, source: File) {
    val dest = getFile(getDefaultStorePathFor(exhibit)) match {
      case file if file.exists =>
        findOtherDest(file)
      case file =>
        file
    }
    
    fileCopy(source, dest)
    
    val libURI = toLibraryURI(dest)
    exhibit.filePathAsURI = libURI
  }
  
  /**
   * ライブラリ相対パスから、実際のファイルパスを得る。
   */
  def getFile(uri: URI): File = uri.getScheme match {
    case "file" => new File(uri)
    case `uriScheme` => new File(baseDir, uri.getPath)
    case _ => throw new IllegalArgumentException(
      "Scheme of the uri must be 'file' or '%s' but '%s'".format(uriScheme, uri))
  }
  
  /**
   * バイオファイルが保存される、標準のファイル名
   */
  def getDefaultStorePathFor(exhibit: MuseumExhibit): URI = {
    import MuseumExhibit.FileType._
    
    val namePrefix = exhibit.name match {
      case "" => exhibit.identifier match {
        case "" => "untitled"
        case identifier => identifier
      }
      case name => name
    }
    
    val suffix = exhibit.fileType match {
      case GenBank => "gbk"
      case FASTA => "fasta"
      case Unknown => "txt"
    }
    
    val fileName = namePrefix + "." + suffix
    
    createLibraryURI(fileName)
  }
  
  /**
   * ライブラリのファイルを削除
   */
  def delete(uri: URI): Boolean = {
    val file = getFile(uri)
    file.delete
  }
  
  /**
   * ファイルパスから ライブラリ URI を作成。
   * @throws IllegalArgumentException {@code file} がライブラリディレクトリで開始されていない時
   */
  private def toLibraryURI(file: File): URI = {
    file.getAbsolutePath.startsWith(baseDir.getAbsolutePath) match {
      case true =>
        val path = file.getAbsolutePath.substring(baseDir.getAbsolutePath.length)
        createLibraryURI(path)
      case false => throw new IllegalArgumentException(
        "File '%s' cannot convert to library URI.".format(file))
    }
  }
  
  /**
   * ライブラリ内パスを表す URI を作成
   */
  private def createLibraryURI(path: String): URI = {
    val pathx = path.dropWhile(File.separatorChar.==)
    new URI(uriScheme, "/" + pathx, null)
  }
}

object LibraryFileManager {
  /**
   * ファイルのコピー
   */
  @throws(classOf[IOException])
  private def fileCopy(src: File, dest: File) {
    import java.nio.channels.FileChannel
    val srcChannel = new FileInputStream(src).getChannel()
    val destChannel = new FileOutputStream(dest).getChannel()
    try {
      srcChannel.transferTo(0, srcChannel.size(), destChannel)
    }
    finally {
      try {
        srcChannel.close()
      }
      finally {
        destChannel.close()
      }
    }
  }
  
  /**
   * 連番付けのファイル名探し
   */
  private def findOtherDest(base: File): File = {
    def splitBasename(name: String) = {
      val index = name.lastIndexOf('.', name.length - 1)
      if (index > 0) name.splitAt(index)
      else (name, "")
    }
    
    def findDestFile(dir: File, basename: String, suffix: String, count: Int = 1): File = {
      val candidate = new File(dir, basename + " " + count + suffix)
      candidate.createNewFile match {
        case true => candidate
        case false => findDestFile(dir, basename, suffix, count + 1)
      }
    }
    
    val dir = base.getParentFile
    val (basename, suffix) = splitBasename(base.getName)
    
    findDestFile(dir, basename, suffix)
  }
}
