package jp.scid.genomemuseum.model

import java.net.URL
import java.io.{File, IOException}

trait MuseumExhibitFileLibrary {
  /**
   * 展示物のソースとなるファイルを保管する。
   * 
   * ファイルは複製され、展示物の内容に基づいた名前が付けられて保存される。
   * @return 保管先
   * @throws IOException ファイル操作時に読み書き例外が発生したとき
   */
  @throws(classOf[IOException])
  def store(source: File, exhibit: MuseumExhibit): File
}
