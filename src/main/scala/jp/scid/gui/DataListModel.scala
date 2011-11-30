package jp.scid.gui

import java.util.Comparator
import javax.swing.ListSelectionModel
import javax.swing.event.{ListSelectionListener, ListSelectionEvent}

import ca.odell.glazedlists.{swing => glswing, matchers, EventList,
  GlazedLists, FilterList, SortedList, BasicEventList}
import glswing.EventSelectionModel
import matchers.{Matcher, MatcherEditor, Matchers}

import event.DataListSelectionChanged

class DataListModel[A] extends DataModel with swing.Publisher {
  import DataListModel._
  
  /** ソースリスト */
  private[gui] val tableSource = new BasicEventList[A]
  /** フィルタリング */
  private[gui] val filteredSource = new FilterList(tableSource)
  /** ソーティング */
  private[gui] val sortedSource = new SortedList(filteredSource, null)
  
  /** リスト選択モデル */
  private[gui] val eventSelectionModel = new EventSelectionModel[A](viewEventList)
  
  /** 変換やフィルタリングを行った後の EventList */
  protected[gui] def viewEventList: EventList[A] = sortedSource
  
  /** 選択されている項目の EventList */
  private[gui] def selectedItems = eventSelectionModel.getTogglingSelected
  
  /** 変換やフィルタリングを行った後のソース */
  def viewSource = {
    import scala.collection.JavaConverters._
    withReadLock(viewEventList) { list => list.asScala.toIndexedSeq }
  }
  
  /** 順番目の表示項目を取得 */
  def viewItem(index: Int) = {
    withReadLock(viewEventList) { list => list.get(index) }
  }
  
  /** 行選択モデルの取得 */
  def selectionModel: ListSelectionModel = eventSelectionModel
  
  /** 項目数の取得 */
  def sourceSize = sourceListWithReadLock { source => source.size }
  
  /** 設定されているデータソース取得 */
  def source: IndexedSeq[A] = {
    import scala.collection.JavaConverters._
    sourceListWithReadLock { _.asScala.toIndexedSeq }
  }
  
  /** データソースの設定 */
  def source_=(newSource: Seq[A]) {
    import scala.collection.JavaConverters._
    val javaSource = newSource.asJava
    withWriteLock(tableSource) { tableSource =>
      if (javaSource.size >= 10000) {
        // パフォーマンスのため
        tableSource.clear()
        tableSource.addAll(javaSource)
      }
      else {
        GlazedLists.replaceAll(tableSource, javaSource, true)
      }
    }
  }
  
  /**
   * データの更新を通知
   */
  def itemUpdated(index: Int) {
    sourceListWithWriteLock { list =>
      if (0 <= index && index < list.size)
        list.set(index, list.get(index))
    }
  }
  
  /**
   * データの更新を通知
   * @todo testing
   */
  def updated(item: A) = {
    sourceListWithWriteLock { list =>
      list.indexOf(item) match {
        case -1 => false
        case index =>
          list.set(index, list.get(index))
          true
      }
    }
  }
  
  /**
   * 選択中の項目を取得。
   */
  def selections: List[A] = {
    import scala.collection.JavaConverters._
    withReadLock(selectedItems) { list => list.asScala.toList }
  }
  
  /**
   * 要素を選択状態にする。
   */
  def selections_=(items: Seq[A]) {
    import scala.collection.JavaConverters._
    val jItems = items.asJava
    withWriteLock(selectedItems) { list =>
      list.clear()
      list addAll jItems
    }
  }
  
  /**
   * 要素を選択する。
   */
  def select(item: A, items: A*) {
    selections = item :: items.toList
  }
  
  /** ソート用の Comparator を設定 */
  def sortWith(c: Comparator[_ >: A]) {
    logger.debug("比較 {}", c)
    withWriteLock(sortedSource) { _ =>
      sortedSource setComparator c
    }
  }
  
  /** ソート解除 */
  def clearSorting() {
    withWriteLock(sortedSource) { _ =>
      sortedSource setComparator null
    }
  }
  
  /** フィルタリング用 Matcher 設定 */
  def filterWith(matcher: Matcher[_ >: A]) {
    logger.debug("Matcher {}", matcher)
    filteredSource setMatcher matcher
  }
  
  /** フィルタリング用 MatcherEditor 設定 */
  def filterWith(matcher: MatcherEditor[_ >: A]) {
    logger.debug("MatcherEditor {}", matcher)
    filteredSource setMatcherEditor matcher
  }
  
  /** フィルタリング解除 */
  def clearFiltering() {
    filteredSource setMatcher null
  }
  
  /**
   * tableSource の読み込みロックをしながら処理行う
   */
  protected[gui] def sourceListWithReadLock[B](function: (java.util.List[A]) => B): B = {
    withReadLock[A, B](tableSource)(function)
  }
  
  /**
   * tableSource の書き込みロックをしながら処理行う
   */
  protected[gui] def sourceListWithWriteLock[B](function: (java.util.List[A]) => B): B = {
    withWriteLock[A, B](tableSource)(function)
  }
  
  /** Swing イベントと scala.swing.Publisher を接続 */
  private val eventAdapter = new ListSelectionEventHandler(this)
  eventSelectionModel.addListSelectionListener(eventAdapter)
}

object DataListModel {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[DataListModel[_]])
  
  /** EventList の処理を待機する */
  def waitEventListProcessing() {
    import java.awt.EventQueue
    if (!EventQueue.isDispatchThread) {
      EventQueue.invokeAndWait(new Runnable {
        def run {}
      })
    }
  }
  
  /**
   * EventList の書き込みロックをして処理を行う
   */
  protected[gui] def withWriteLock[A, B](el: EventList[A])(function: (EventList[A]) => B): B = {
    el.getReadWriteLock().writeLock().lock()
    try {
      function(el)
    }
    finally {
      el.getReadWriteLock().writeLock().unlock()
    }
  }
  
  /**
   * EventList の読み込みロックをして処理を行う
   */
  protected[gui] def withReadLock[A, B](el: EventList[A])(function: (EventList[A]) => B): B = {
    el.getReadWriteLock().readLock().lock()
    try {
      function(el)
    }
    finally {
      el.getReadWriteLock().readLock().unlock()
    }
  }
  
  /**
   * DataListSelectionChanged イベント変換クラス
   */
  private class ListSelectionEventHandler[A](source: DataListModel[A]) extends ListSelectionListener {
    def valueChanged(e: ListSelectionEvent) {
      val evt = DataListSelectionChanged(source,
        e.getValueIsAdjusting, source.selections)
      
      source publish evt
    }
  }
}
