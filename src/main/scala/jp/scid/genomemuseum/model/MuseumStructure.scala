package jp.scid.genomemuseum.model

/**
 * ExhibitRoom のツリーのモデル
 */
class MuseumStructure extends TreeSource[ExhibitRoom] {
  /** ルートオブジェクト */
  val root = ExhibitRoom("Museum")
  /** 子要素 */
  def childrenFor(parent: ExhibitRoom) = Nil
  /** 末端要素であるか */
  def isLeaf(node: ExhibitRoom) = true
}

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
