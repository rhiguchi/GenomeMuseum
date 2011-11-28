package jp.scid.genomemuseum.model

import java.io.{File, FileInputStream, FileOutputStream, IOException,
  BufferedInputStream, BufferedOutputStream}
import java.net.{URL, URI}

class LibraryFileManager(val baseDir: File) extends MuseumExhibitStorage {
  import LibraryFileManager._
  
  /** このライブラリ内であることを示す URI スキーマ名 */
  private val libraryScheme = "gmlib"
  /** ライブラリディレクトリの URI 表現 */
  private val libraryURI = baseDir.getAbsoluteFile.toURI.normalize
  
  /**
   * ファイルを展示物に合った格納先パスへ移動する。
   * @return 格納先を表す URL
   * @throws IOException 入出力エラー
   */
  def saveSource(exhibit: MuseumExhibit, data: File) = {
    logger.debug("ソース保存 {}", data)
    // 出力先の探索
    val destCandidate = getDefaultStorePath(exhibit)
    if (!destCandidate.getParentFile.exists) destCandidate.getParentFile.mkdirs
  
    val dest = destCandidate.createNewFile match {
      case true => destCandidate
      case false => findOtherDest(destCandidate)
    }
    // ファイルの移動
    val uri = data.renameTo(dest) match {
      case true => toLibraryURI(dest)
      case false => throw new IOException(
        "cannot rename from '%s' to '%s'.".format(data,dest))
    }
    logger.debug("ソース保存先 {}", dest)
    // 保存先の適用
    exhibit.filePath = uri.toString
    dest.toURI.toURL
  }
  
  /**
   * MuseumExhibit に適用された filePath から URL を取得
   */
  def getSource(exhibit: MuseumExhibit) = {
    logger.trace("ソース取得 {}", exhibit.filePath)
    
    URI.create(exhibit.filePath) match {
      case null => None
      case uri => 
        val resolvedUri = uri.getScheme match {
          case `libraryScheme` =>
            new File(baseDir, uri.getSchemeSpecificPart).toURI
          case _ => uri
        }
        logger.trace("ソース取得解決 URI {}", resolvedUri)
        Some(resolvedUri.toURL)
    }
  }
  
  /**
   * バイオファイルの標準の格納先
   */
  def getDefaultStorePath(exhibit: MuseumExhibit) = {
    import MuseumExhibit.FileType._
    
    val namePrefix = exhibit.name match {
      case "" => exhibit.identifier match {
        case "" => "unidentifiable"
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
    new File(baseDir, fileName)
  }
  
  /**
   * ファイルパスから ライブラリ URI を作成。
   * @throws IllegalArgumentException {@code file} がライブラリディレクトリで開始されていない時
   */
  private def toLibraryURI(file: File): URI = {
    val base = file.getAbsoluteFile.toURI.normalize
    base.toString.startsWith(libraryURI.toString) match {
      case true =>
        val relURI = libraryURI.relativize(base)
        new URI(libraryScheme, relURI.getSchemeSpecificPart, null)
      case false => throw new IllegalArgumentException(
        "File '%s' cannot convert to library URI.".format(file))
    }
  }
}

object LibraryFileManager {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[LibraryFileManager])
  
  /**
   * 連番付けのファイル名探し
   * @throws IOException 入出力エラー
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
