package jp.scid.genomemuseum.model

import java.io.File
import java.net.URI

/**
 * ファイルを URI として管理するライブラリのインターフェイス
 * 
 * ファイルは URI に変換され管理されるが、ファイルのパスがライブラリ内である時、
 * ライブラリ内の相対パスとして URI を作成し、管理を行う。
 */
trait UriFileStorage {
  /**
   * ライブラリの URI からファイルに変換する。
   * 
   * @throws IllegalArgumentException {@code libraryUri} にスキーマが
   *         このライブラリが管理できるファイルを表していないとき。
   */
  def getFile(libraryUri: URI): File
  
  /**
   * ファイルを格納先 URI に変換する。
   * 
   * 返される URI は、スキーマが {@code file} となり、
   * この格納場所の権限が付与され、パスは相対化された格納先となる。
   */
  def getUri(file: File): URI
}
