package jp.scid.gui

import swing.Publisher

/**
 * GUI 用データモデル
 * 実装クラスは scala.swing.Publisher を実装する必要がある。
 */
trait DataModel {
  this: Publisher =>
}