package jp.scid.genomemuseum.model

import java.io.{File, IOException}
import java.net.URI

private object DefaultMuseumExhibitFileLibrary {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[DefaultMuseumExhibitFileLibrary])
  
  /**
   * 連番付けのファイル名探し
   * @throws IOException 入出力エラー
   */
  @throws(classOf[IOException])
  private def createNewDestFile(base: File): File = {
    def splitBasename(name: String) = {
      val index = name.lastIndexOf('.', name.length - 1)
      if (index > 0) name.splitAt(index)
      else (name, "")
    }
    
    def findDestFile(dir: File, basename: String, suffix: String, count: Int): File = {
      val candidate = new File(dir, basename + " " + count + suffix)
      candidate.createNewFile match {
        case true => candidate
        case false => findDestFile(dir, basename, suffix, count + 1)
      }
    }
    
    val dir = base.getParentFile
    dir.mkdirs
    base.createNewFile match {
      case true => base
      case false =>
        val (basename, suffix) = splitBasename(base.getName)
        findDestFile(dir, basename, suffix, 1)
    }
  }
  
  /** ファイルを複製 */
  @throws(classOf[IOException])
  private def copyFile(src: File, dest: File) {
    import java.io.{FileInputStream, FileOutputStream}
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
}

/**
 * ファイルをローカルディスクのディレクトリ以下に保管するクラス
 */
class DefaultMuseumExhibitFileLibrary(val baseDir: File) extends MuseumExhibitFileLibrary {
  import DefaultMuseumExhibitFileLibrary._
  /** ファイル保管場所管理オブジェクト */
  private val myUriFileStorage = new DefaultUriFileStorage("gmlib", baseDir)
  
  /**
   * このライブラリのファイル名の解決を行うオブジェクト
   */
  def uriFileStorage: UriFileStorage = myUriFileStorage
  
  /**
   * 展示物のファイルを保管する。
   * 
   * ファイル名は {@code #getDefaultStorePath} より作成される。
   * ファイルが既に存在する時は、拡張子前に連番が付与されたファイルに保管される。
   * 
   * @return 保管されたファイル
   * @throws IOException ファイル操作時に読み書き例外が発生したとき
   * @see #getDefaultStorePath(MuseumExhibit)
   */
  @throws(classOf[IOException])
  def store(source: File, exhibit: MuseumExhibit): File = {
    logger.debug("ソース保存 {}", exhibit)
    // 標準の出力先を取得
    val relativePath = getDefaultStorePath(exhibit).toString
    
    // ファイルが存在しない、新しい場所にファイルを作成
    val dest = createNewDestFile(new File(baseDir, relativePath))
    
    // 複製
    copyFile(source, dest)
    
    dest
  }
  
  /**
   * バイオファイルの格納先の相対パスを返す。
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
    new File(fileName)
  }
}