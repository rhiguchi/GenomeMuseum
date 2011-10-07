package jp.scid.gui.tree

/**
 * 変更可能ツリー階層の定義
 */
trait EditableTreeSource[A] extends TreeSource[A] {
  /** 値の更新 */
  def update(path: IndexedSeq[A], newValue: AnyRef)
}
