package jp.scid.genomemuseum.model

/**
 * ExhibitRoom のツリーのモデル
 */
class MuseumStructure extends TreeSource[ExhibitRoom] {
  val localStore = ExhibitRoom("Local")
  val libraries = ExhibitRoom("Libraries", localStore)
  
  val userLists = ExhibitRoom("User Lists")
  
  /** ルートオブジェクト */
  val root = ExhibitRoom("Museum", libraries, userLists)
  /** 子要素 */
  def childrenFor(parent: ExhibitRoom) = parent.children
  /** 末端要素であるか */
  def isLeaf(node: ExhibitRoom) = false
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

/**
 * 変更可能ツリー階層の定義
 */
trait EditableTreeSource[A] extends TreeSource[A] {
  /** 値の更新 */
  def update(path: IndexedSeq[A], newValue: AnyRef): A
}
