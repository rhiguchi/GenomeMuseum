package jp.scid.genomemuseum.model

import org.specs2.mock._

object ListDataServiceSpec extends Mockito {
  /**
   * モックオブジェクトを NullPointerException が起きないように構成する。
   */
  def makeMock(mock: ListDataService[_]) = {
    mock.allElements returns Nil
    mock.indexOf(any) returns -1
    mock
  }
}