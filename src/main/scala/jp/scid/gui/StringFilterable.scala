package jp.scid.gui

import ca.odell.glazedlists.TextFilterator
import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor

/**
 * 文字列でフィルタリングをする機能を持たせるモジュールトレイト。
 */
trait StringFilterable[A] {
  this: DataListModel[A] =>
  
  private var currentFilterText = ""
  
  private val matcherEditor = new SearchEngineTextMatcherEditor(new TextFilterator[A]{
    def getFilterStrings(baseList: java.util.List[String], element: A) {
      StringFilterable.this.getFilterString(baseList, element)
    }
  })
  
  /** 現在の抽出条件文字列を取得する */
  def filterText = currentFilterText
  
  /** 抽出条件文字列を設定する */
  def filterText_=(text: String) {
    currentFilterText = text
    matcherEditor refilter text
  }
  
  /**
   * このトレイトの持つ MatcherEditor を FilterList へ設定する。
   */
  protected[gui] def filteratorChanged() {
    filterWith(matcherEditor)
  }
  
  /**
   * フィルタリングに用いられる文字列を要素から取得する
   */
  protected def getFilterString(baseList: java.util.List[String], element: A)
  
  // MatcherEditor を設定
  filteratorChanged()
}
