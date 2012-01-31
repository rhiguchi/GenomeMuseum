package jp.scid.gui

import swing.Publisher

/**
 * GUI 用データモデル
 * 実装クラスは scala.swing.Publisher を実装する必要がある。
 */
trait DataModel {
  this: Publisher =>
}

object DataModel {
  /**
   * モデルバインディングの結合を保持するインターフェイス
   */
  trait Connector {
    /**
     * モデル結合を解除する。
     */
    def release()
  }
  
  object Connector {
    def apply(releaser: => Unit): Connector = new Connector {
      def release() = releaser
    }
  }
}
