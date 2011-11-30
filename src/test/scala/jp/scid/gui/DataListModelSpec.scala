package jp.scid.gui

import org.specs2._
import mock._

import java.util.{Comparator, Collections}
import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.matchers.{Matcher, MatcherEditor, CompositeMatcherEditor}
import event.DataListSelectionChanged

import DataListModel.waitEventListProcessing

private[gui] object DataListModelSpec {
  private[gui] case class TestElement(
    name: String,
    age: Int,
    addr: String
  )
}

class DataListModelSpec extends Specification with Mockito {
  import DataListModelSpec.TestElement
  import scala.collection.JavaConverters._
  
  def is = "DataListModel" ^
    "初期状態のモデル" ^ isEmpty(emptyModel) ^ bt ^
    "元要素の設定" ^ sourceSpec(emptyModel, createElement _) ^ bt ^
    "要素数の取得" ^ sourceSizeSpec(emptyModel, createElement _) ^ bt ^
    "表示要素の取得" ^ canGetViewItem(emptyModel, createElement _) ^ bt ^
    "並び替え" ^ canSorting ^ bt ^
    "抽出" ^ canFilter ^ bt ^
    "項目選択" ^ selectionsSpec(emptyModel, createElement _) ^ bt ^
    "要素が選択されている状態" ^ notEmptySelectionSpec(itemsSelected) ^ bt ^
    "イベント発行" ^ canPublishEvent(emptyModel, createElement _) ^ bt ^
    end
  
  def emptyModel = new DataListModel[Symbol]
  
  def itemsSelected = {
    val model = emptyModel
    val items = 0 until 10 map createElement
    model.source = items
    waitEventListProcessing()
    model.selections = items.drop(2).take(4)
    model
  }
  
  def createElement(param: Int) = Symbol("element" + param)
  
  class TestBase[A](val model: DataListModel[A])
  
  def isEmpty(m: => DataListModel[_]) =
    "ソース配列は空" ! empty(m).s1 ^
    "ソースの要素数が 0" ! empty(m).s2 ^
    "ビュー配列は空" ! empty(m).s3 ^
    "選択中項目配列は空" ! empty(m).s4
  
  def sourceSpec[A](m: => DataListModel[A], factory: Int => A) =
    "適用したソースの取得 [1]" ! source(m, factory).getSource(1) ^
    "適用したソースの取得 [10]" ! source(m, factory).getSource(10) ^
    "適用したソースの取得 [10000]" ! source(m, factory).getSource(10000) ^
    "平行設定 [2]" ! source(m, factory).parallelSetting(2) ^
    "平行設定 [10]" ! source(m, factory).parallelSetting(10) ^
    "平行設定・取得 [10]" ! source(m, factory).parallelSetGet(10)
  
  def sourceSizeSpec[A](m: => DataListModel[A], factory: Int => A) =
    "要素数の取得 [1]" ! sourceSize(m, factory).getSourceSize(1) ^
    "要素数の取得 [10]" ! sourceSize(m, factory).getSourceSize(10) ^
    "要素数の取得 [10000]" ! sourceSize(m, factory).getSourceSize(10000) ^
    "平行取得 [10]" ! sourceSize(m, factory).parallelGet(10)
  
  def canGetViewItem[A](m: => DataListModel[A], factory: Int => A) =
    "最初の要素取得" ! viewItem(m, factory).getFirst ^
    "最後の要素取得" ! viewItem(m, factory).getLast ^
    "負の数で例外" ! viewItem(m, factory).negativeThrowsException ^
    "要素数超えで例外" ! viewItem(m, factory).overIndexThrowsException ^
    "平行取得 [10]" ! viewItem(m, factory).parallelGet(10)
  
  def canSorting =
    "表示側要素が並びかわる" ! sorting.comparator ^
    "ソースは同じ" ! sorting.sourceNotAffected ^
    "None で整列が戻る" ! sorting.none ^
    "平行設定 [100]" ! sorting.parallel(100)
  
  def canFilter =
    "表示側要素が変わる" ! filtering.matcher ^
    "ソースは同じ" ! filtering.sourceNotAffected ^
    "None で抽出が戻る" ! filtering.none ^
    "MatcherEditor" ! filtering.matcherEditor ^
    "MatcherEditor 解除" ! filtering.clearMatcherEditor ^
    "平行設定 [100]" ! sorting.parallel(100)
  
  def notEmptySelectionSpec(m: => DataListModel[_]) =
    "選択中項目配列は空ではない" ! nonEmptySelections(m).notEmpty ^
    "無選択にできる" ! nonEmptySelections(m).makeNoSelection
  
  def selectionsSpec[A](m: => DataListModel[A], factory: Int => A) =
    "選択要素の設定と取得" ! selections(m, factory).setAndGet ^
    "選択モデルが更新される" ! selections(m, factory).selectionModelSelections
  
  def canPublishEvent[A](m: => DataListModel[A], factory: Int => A) =
    "選択イベント" ^ canPublishSelectionEvent(m, factory) ^ bt
  
  def canPublishSelectionEvent[A](m: => DataListModel[A], factory: Int => A) =
    "ソースオブジェクト" ! selectionEvent(m, factory).sourceObj ^
    "調節中値 true" ! selectionEvent(m, factory).adjust(true) ^
    "調節中値 false" ! selectionEvent(m, factory).adjust(false) ^
    "選択項目" ! selectionEvent(m, factory).selectionList
  
  def empty(m: DataListModel[_]) = new Object {
    def s1 = m.source must beEmpty
    def s2 = m.sourceSize must_== 0
    def s3 = m.viewSource must beEmpty
    def s4 = m.selections must beEmpty
  }
  
  def source[A](m: DataListModel[A], factory: Int => A) = new Object {
    def elms(i: Int) = 0 until i map factory
    
    def getSource(count: Int) = {
      val newSource = 0 until count map factory
      m.source = newSource
      m.source must_== newSource
    }
    
    def parallelSetting(count: Int) = {
      (0 until count).map(i => elms(i)).par.foreach(m.source_=)
      success
    }
    
    def parallelSetGet(count: Int) = {
      (0 until count).map(i => elms(i)).par.foreach{ e =>
        m.source = e
        m.source
      }
      success
    }
  }
  
  def sourceSize[A](m: DataListModel[A], factory: Int => A) = new Object {
    def elms(i: Int) = 0 until i map factory
    
    def getSourceSize(count: Int) = {
      val newSource = elms(count)
      m.source = newSource
      m.sourceSize must_== newSource.size
    }
    
    def parallelGet(count: Int) = {
      (0 until count).map(i => elms(i)).par.foreach{ e =>
        m.source = e
        m.sourceSize
      }
      success
    }
  }
  
  def viewItem[A](m: DataListModel[A], factory: Int => A) = new Object {
    def elms(i: Int) = 0 until i map factory
    
    def getFirst = {
      val newSource = elms(10)
      m.source = newSource
      m.viewItem(0) must_== newSource.head
    }
    
    def getLast = {
      val newSource = elms(5)
      m.source = newSource
      m.viewItem(4) must_== newSource.last
    }
    
    def negativeThrowsException = {
      m.source = elms(3)
      m.viewItem(-1) must throwA[IndexOutOfBoundsException]
    }
    
    def overIndexThrowsException = {
      m.source = elms(1)
      m.viewItem(1) must throwA[IndexOutOfBoundsException]
    }
    
    def parallelGet(count: Int) = {
      (1 to count).map(i => elms(i)).par.foreach{ e =>
        m.source = e
        m.viewItem(0)
      }
      success
    }
  }
  
  def sorting = new Object {
    val c = new Comparator[Symbol] {
      def compare(o1: Symbol, o2: Symbol) =  o1.name.compareTo(o2.name)
    }
    val cRev = Collections.reverseOrder(c)
    
    val model = emptyModel
    
    private def setSourceAndSort() {
      model.source = List('c, 'a, 'b)
      model.sortWith(c)
    }
    
    def comparator = {
      setSourceAndSort()
      model.viewSource must contain('a, 'b, 'c).only.inOrder
    }
    
    def sourceNotAffected = {
      setSourceAndSort()
      model.source must contain('c, 'a, 'b).only.inOrder
    }
    
    def none = {
      setSourceAndSort()
      model.clearSorting()
      model.viewSource must contain('c, 'a, 'b).only.inOrder
    }
    
    def parallel(count: Int) = {
      model.source = 0 to 100 map (i => Symbol("elm" + i))
      (0 until count).map(i => i % 3).par.foreach { i =>
        i match {
          case 0 => model.clearSorting()
          case 1 => model.sortWith(c)
          case 2 => model.sortWith(cRev)
        }
        model.viewSource
      }
      success
    }
  }
  
  def filtering = new Object {
    val model = emptyModel
    val aMatcher = new Matcher[Symbol] {
      def matches(item: Symbol) = item.name.contains("a")
    }
    val aMatcherEditor = GlazedLists.fixedMatcherEditor(aMatcher)
    
    private def setSourceAndFilter() {
      model.source = List('cab, 'apple, 'bee)
      model.filterWith(aMatcher)
    }
    
    def matcher = {
      setSourceAndFilter()
      model.viewSource must contain('cab, 'apple).only.inOrder
    }
    
    def sourceNotAffected = {
      setSourceAndFilter()
      model.source must contain('cab, 'apple, 'bee).only.inOrder
    }
    
    def none = {
      setSourceAndFilter()
      model.clearFiltering()
      model.viewSource must contain('cab, 'apple, 'bee).only.inOrder
    }
    
    def matcherEditor = {
      model.source = List('cab, 'apple, 'bee)
      model.filterWith(aMatcherEditor)
      model.viewSource must contain('cab, 'apple).only.inOrder
    }
    
    def clearMatcherEditor = {
      model.source = List('cab, 'apple, 'bee)
      model.filterWith(aMatcherEditor)
      model.clearFiltering()
      model.viewSource must contain('cab, 'apple, 'bee).only.inOrder
    }
    
    def parallel(count: Int) = {
      model.source = 0 to 100 map (i => Symbol("elm" + i))
      (0 until count).map(i => i % 3).par.foreach { i =>
        i match {
          case 0 => model.clearFiltering()
          case 1 => model.filterWith(aMatcherEditor)
          case 2 => model.filterWith(aMatcher)
        }
        model.viewSource
      }
      success
    }
  }
  
  private[gui] class SelectionBase[A](m: DataListModel[A], factory: Int => A) {
    val elements = 0 until 10 map factory
    val selection = elements.drop(3).take(3)
    m.source = elements
    waitEventListProcessing()
  }
  
  def selections[A](m: DataListModel[A], factory: Int => A) = new SelectionBase(m, factory) {
    m.selections = selection
    
    def setAndGet = {
      m.selections must_== selection
    }
    
    def selectionModelSelections = {
      (m.selectionModel.getMinSelectionIndex must_== 3) and
      (m.selectionModel.getMaxSelectionIndex must_== 5)
    }
  }
  
  def nonEmptySelections(m: DataListModel[_]) = new Object {
    def notEmpty = m.selections must not beEmpty
    
    def makeNoSelection = {
      m.selections = Nil
      m.selections must beEmpty
    }
  }
  
  def selectionEvent[A](m: DataListModel[A], factory: Int => A) = new SelectionBase(m, factory) {
    var eventSource: Option[DataListModel[_]] = None
    var eventAdjusting: Option[Boolean] = None
    var eventSelections: Option[List[A]] = None
    
    m.reactions += {
      case DataListSelectionChanged(source, adj, sel) =>
        eventSource = Some(source)
        eventAdjusting = Some(adj)
        eventSelections = Some(sel.asInstanceOf[List[A]])
    }
    
    def sourceObj = {
      m.selections = selection
      eventSource must beSome(m)
    }
    
    def adjust(value: Boolean) = {
      m.selectionModel.setValueIsAdjusting(value)
      m.selections = selection
      eventAdjusting must beSome(value)
    }
    
    def selectionList = {
      m.selections = selection
      eventSelections must beSome(selection)
    }
  }
}
