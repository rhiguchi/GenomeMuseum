package jp.scid.gui

import java.util.Comparator
import javax.swing.{JTextField, ListSelectionModel}
import javax.swing.event.{ListSelectionListener, ListSelectionEvent}

import collection.mutable.Buffer

import ca.odell.glazedlists.{swing => glswing, matchers, EventList,
  GlazedLists, FilterList, SortedList, BasicEventList}
import glswing.{EventSelectionModel, SearchEngineTextFieldMatcherEditor}
import matchers.{Matcher, MatcherEditor}

import event.DataListSelectionChanged

class DataListModel[A] extends DataModel with swing.Publisher {
  import DataListModel._
  
  /** ソースリスト */
  protected val tableSource = new BasicEventList[A]
  /** フィルタリング */
  protected val filteredSource = new FilterList(tableSource)
  /** ソーティング */
  protected val sortedSource = new SortedList(filteredSource, null)
  
  /** リスト選択モデル */
  protected val eventSelectionModel = new EventSelectionModel[A](viewSource)
  
  /** 変換やフィルタリングを行った後のソース */
  def viewSource: EventList[A] = sortedSource
  
  /**
   * 書き込みロックをして {@code source} を編集するためのメソッド
   */
  def sourceWithWriteLock[B](function: (Buffer[A]) => B): B =
    withWriteLockedBuffer(tableSource, function)
  
  /**
   * 読み込みロックをしてから {@code source} を読み込むためのメソッド
   */
  def sourceWithReadLock[B](function: Buffer[A] => B): B =
    withReadLockedBuffer(tableSource, function)
  
  /** インクリメンタルサーチ解除関数 */
  private var removeSeachFieldKeyListener = () => {}
  
  /** 設定されているデータソース取得 */
  def source: Buffer[A] = {
    import scala.collection.JavaConverters._
    tableSource.asScala
  }
  
  /** データソースの設定 */
  def source_=(newSource: Seq[A]) {
    import scala.collection.JavaConverters._
    
    tableSource.getReadWriteLock().writeLock().lock()
    try {
      GlazedLists.replaceAll(tableSource, newSource.asJava, true)
    }
    finally {
      tableSource.getReadWriteLock().writeLock().unlock()
    }
  }
  
  /** 項目数の取得 */
  def sourceSize = sourceWithReadLock { source => source.size }
  
  /** 行選択モデルの取得 */
  def selectionModel: ListSelectionModel = eventSelectionModel
  
  /** 選択されている項目を取得 */
  def selectedItems = eventSelectionModel.getTogglingSelected
  
  /** 書き込みロックをして {@code selectedItems} を編集する */
  def selectedItemsWithWriteLock[B](function: (Buffer[A]) => B): B = 
    withWriteLockedBuffer(selectedItems, function)
  
  /** 読み込みロックをして {@code selectedItems} を編集する */
  def selectedItemsWithReadLock[B](function: (Buffer[A]) => B): B = 
    withReadLockedBuffer(selectedItems, function)
  
  def deselectedItems = eventSelectionModel.getTogglingDeselected
  
  /** 書き込みロックをして {@code deselectedItems} を編集する */
  def deselectedItemsWithWriteLock[B](function: (Buffer[A]) => B): B = 
    withWriteLockedBuffer(deselectedItems, function)
  
  /** 読み込みロックをして {@code deselectedItems} を編集する */
  def deselectedItemsWithReadLock[B](function: (Buffer[A]) => B): B = 
    withReadLockedBuffer(deselectedItems, function)
  
  /** ソート用の Comparator を設定 */
  def sortWith(c: Comparator[_ >: A]) {
    sortedSource setComparator c
  }
  
  /** ソート用の比較関数を設定 */
  def sortWith[B >: A](c: (B, B) => Int) {
    sortWith(new FunctionComparator(c))
  }
  
  /** フィルタリング用 Matcher 設定 */
  def filterWith(matcher: Matcher[_ >: A]) {
    filteredSource setMatcher matcher
  }
  
  /** フィルタリング用 MatcherEditor 設定 */
  def filterWith(matcher: MatcherEditor[_ >: A]) {
    // 現在のインクリメンタルサーチを解除
    removeSeachFieldKeyListener()
    
    filteredSource setMatcherEditor matcher
  }
  
  /** テキストフィールドを使用してのフィルタリング */
  def filterUsing(field: JTextField, filterator: (Buffer[String], _ >: A) => Unit) {
    import java.awt.event.{KeyEvent, KeyAdapter}
    
    val textFilterator = new ScalaTextFilterator(filterator)
    val matcherEditor = new SearchEngineTextFieldMatcherEditor(field, textFilterator)
    
    // JTextFields の MatcherEditor を使用する。
    filterWith(matcherEditor)
    
    // インクリメンタルサーチの設定
    val keyHandler = new KeyAdapter {
      override def keyReleased(e: KeyEvent) {
        matcherEditor refilter field.getText
      }
    }
    field addKeyListener keyHandler
    
    // インクリメンタルサーチ解除関数を更新
    removeSeachFieldKeyListener = () => {
      field removeKeyListener keyHandler
    }
  }
  
  // イベント結合
  bindListSelectionEventPublisher(this)
}

object DataListModel {
  import ca.odell.glazedlists.TextFilterator
  
  /** TextFilterator の Scala 用委譲クラス */
  private class ScalaTextFilterator[A](filterator: (Buffer[String], _ >: A) => Unit)
      extends TextFilterator[A] {
    import collection.JavaConverters._
    private val baseBuffer = Buffer.empty[String]
    
    def getFilterStrings(baseList: java.util.List[String], element: A) {
      baseBuffer.clear()
      filterator(baseBuffer, element)
      baseBuffer.map(baseList.add)
    }
  }
  
  /** Scala 関数から Comparator に変換するクラス */
  private class FunctionComparator[A](comparator: (A, A) => Int) extends Comparator[A] {
    def compare(o1: A, o2: A): Int = {
      comparator(o1, o2)
    }
  }
  
  /**
   * EventList の書き込みロックをして Buffer として処理を行う
   */
  private def withWriteLockedBuffer[A, B](el: EventList[A], function: (Buffer[A]) => B): B = {
    import scala.collection.JavaConverters._
    
    el.getReadWriteLock().writeLock().lock()
    try {
      function(el.asScala)
    }
    finally {
      el.getReadWriteLock().writeLock().unlock()
    }
  }
  
  /**
   * EventList の読み込みロックをして Buffer として処理を行う
   */
  private def withReadLockedBuffer[A, B](el: EventList[A], function: (Buffer[A]) => B): B = {
    import scala.collection.JavaConverters._
    
    el.getReadWriteLock().readLock().lock()
    try {
      function(el.asScala)
    }
    finally {
      el.getReadWriteLock().readLock().unlock()
    }
  }
  
  /**
   * Swing イベントと scala.swing.Publisher を接続する
   */
  private def bindListSelectionEventPublisher[A](source: DataListModel[A]) {
    val handler = new ListSelectionEventHandler(source)
    source.eventSelectionModel addListSelectionListener handler
  }
  
  /**
   * DataListSelectionChanged イベント変換クラス
   */
  private class ListSelectionEventHandler[A](source: DataListModel[A]) extends ListSelectionListener {
    def valueChanged(e: ListSelectionEvent) {
      val selections = source.selectedItemsWithReadLock(_.toList)
      val evt = DataListSelectionChanged(source,
        e.getValueIsAdjusting, selections)
      
      source publish evt
    }
  }
}
