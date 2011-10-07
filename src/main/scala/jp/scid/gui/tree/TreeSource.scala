package jp.scid.gui.tree

/**
 * ツリー階層の定義
 */
trait TreeSource[A] {
  /** ルートオブジェクト */
  def root: A
  /** 子要素 */
  def childrenFor(parent: A): List[A]
  /** 末端要素であるか */
  def isLeaf(node: A): Boolean
}
