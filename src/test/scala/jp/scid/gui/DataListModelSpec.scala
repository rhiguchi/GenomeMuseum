package jp.scid.gui

import org.specs2._
import mock._

import java.util.{Comparator, Collections}
import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.matchers.{Matcher, MatcherEditor, CompositeMatcherEditor}
import event.DataListSelectionChanged

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
    "並び替え" ^ canSorting ^ bt ^
    "抽出" ^ canFilter ^ bt ^
    "要素が選択されている状態" ^ someElementsSelectedSpec(emptyModel) ^ bt ^
    "選択モデル" ^ selectionsSpec ^ bt ^
    "イベント発行" ^ canPublishEvent ^ bt ^
    end
  
  def emptyModel = new DataListModel[Symbol]
  
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
  
  def someElementsSelectedSpec(m: => DataListModel[_]) =
    "選択中項目配列は空ではない" ! todo ^
    "選択モデルから行数を取得" ! todo ^
    "無選択にできる" ! todo
  
  def selectionsSpec =
    "選択要素の設定と取得" ! todo ^
    "選択モデルが更新される" ! todo ^
    "選択モデルに適用" ! todo
  
  def canPublishEvent =
    "選択イベント" ! todo
  
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
      val newSource = 0 until count map factory
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
}
