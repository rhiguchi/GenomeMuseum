package jp.scid.gui.tree

import org.specs2._
import mock._

object TreeSourceSpec extends Mockito {
  /** null を返さないモックを作成 */
  def makeMock[A](source: TreeSource[A], root: A) = {
    source.root returns root
    source.childrenFor(any) returns Nil
    source
  }
}