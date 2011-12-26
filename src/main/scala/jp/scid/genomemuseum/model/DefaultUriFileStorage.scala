package jp.scid.genomemuseum.model

import java.io.File
import java.net.URI

/**
 * ディレクトリを基本としてローカルディスクでファイル管理をする URI 解決クラス
 * 
 * @param name 格納場所識別名
 * @param baseDir 格納先の基本ディレクトリ
 */
class DefaultUriFileStorage(val name: String, var baseDir: File) extends UriFileStorage {
  /**
   * ファイルを {@code baseDir} を基にした URI へ変換する。
   * 
   * ファイルが {@code baseDir} 以下に存在する時は、権限が付与されたパスをもつ
   * URI が返される。それ以外は、{@code file#toURI} の値を返す。
   */
  def getUri(file: File): URI = {
    file.getPath.startsWith(baseDir.getPath) match {
      case true =>
        val relPath = file.getPath.substring(baseDir.getPath.length)
        new URI("file", name, relPath, null, null)
      case false => file.toURI
    }
  }
  
  /**
   * ファイル URI をファイルオブジェクトへ変換する。
   * 
   * URI が {@code name} の権限が付与され {@code file} スキーマであるならば
   * {@code baseDir} を基にした File が返される。それ以外は、{@code new File(URI)} の値を返す。
   */
  def getFile(uri: URI): File = {
    name == uri.getAuthority && uri.getScheme == "file" match {
      case true => new File(baseDir, uri.getPath)
      case false => new File(uri)
    }
  }
}
