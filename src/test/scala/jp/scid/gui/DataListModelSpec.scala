package jp.scid.gui

import org.specs2._
import mock._

import java.util.Comparator
import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.matchers.{Matcher, MatcherEditor, CompositeMatcherEditor}
import event.DataListSelectionChanged

class DataListModelSpec extends Specification with Mockito {
  import DataListModelSpec.TestElement
  import scala.collection.JavaConverters._
  
  def is = "DataListModel" ^
    "初期状態" ^
      "source の取得" ! initial.s1 ^
      "selections が空である" ! initial.s2 ^
    bt ^ "sortWith" ^
      "並び替え" ! sortWith.s1 ^
    bt ^ "filterWith" ^
      "Matcher オブジェクト" ! filterWith.s1 ^
      "MatcherEditor オブジェクト" ! filterWith.s2 ^
    bt ^ "selections" ^
      "selectionModel からの設定" ! selections.s1 ^
      "selections プロパティの指定" ! selections.s2 ^
      "select メソッドからの指定" ! selections.s3 ^
    bt ^ "変換後の選択項目" ^
      "並び替え後" ! transformed.s1 ^
      "フィルタリング後" ! transformed.s2 ^
    bt ^ "reactions" ^
      "イベント発行" ! reactions.s1 ^
      "isAdjusting 値" ! reactions.s2 ^
      "selections 値" ! reactions.s3 
      
  
  class TestBase {
    val e1 = TestElement("element1", 30, "apple")
    val e2 = TestElement("element2", 50, "bread")
    val e3 = TestElement("element3", 20, "app")
    val model = new DataListModel[TestElement]
    val sampleSource = List(e1, e2, e3)
    model.source = sampleSource
    
    def viewSource = model.viewSource
    
    def selectionModel = model.selectionModel
    
    def selections = model.selections
  }
  
  val initial = new TestBase {
    def s1 = viewSource must contain(e1, e2, e3).only.inOrder
    
    def s2 = selections must beEmpty
  }
  
  trait TestComparators { this: TestBase =>
    val ageComparator = new Comparator[TestElement] {
      def compare(o1: TestElement, o2: TestElement) = {
        o1.age - o2.age
      }
    }
    
    def sortModelByAgeComparator {
      model sortWith ageComparator
    }
  }
  
  def sortWith = new TestBase with TestComparators {
    def viewSourceAfterSorting = {
      sortModelByAgeComparator
      viewSource
    }
    
    def s1 = viewSourceAfterSorting must contain(e3, e1, e2).only.inOrder
  }
  
  trait TestFilters { this: TestBase =>
    val youngMatcher = new Matcher[TestElement] {
      def matches(item: TestElement): Boolean = {
        item.age <= 20
      }
    }
    
    def setYoungMatcherToModel() {
      model filterWith youngMatcher
    }
  }
  
  def filterWith = new TestBase with TestFilters {
    def youngMatcherSet = {
      setYoungMatcherToModel()
      viewSource
    }
    
    def emptyMatcherEditorSet = {
      val cme = new CompositeMatcherEditor[TestElement]
      model filterWith cme
      viewSource
    }
    
    def matcherEditorSetChange = {
      val cme = new CompositeMatcherEditor[TestElement]
      model filterWith cme
      cme.getMatcherEditors add GlazedLists.fixedMatcherEditor(youngMatcher)
      viewSource
    }
    
    def s1 = youngMatcherSet must contain(e3).only
    
    def s2_1 = emptyMatcherEditorSet must contain(e1, e2, e3).only.inOrder
    def s2_2 = matcherEditorSetChange must contain(e3).only
    def s2 = s2_1 and s2_2
  }
  
  def selections = new TestBase {
    def selectFirstElement = {
      selectionModel.setSelectionInterval(0, 0)
      selections
    }
    
    def byProperty = {
      model.selections = List(e2, e3)
      selections
    }
    
    def bySelect = {
      model select (e3, e1)
      selections
    }
    
    def s1 = selectFirstElement must contain(e1).only
    
    def s2 = byProperty must contain(e2, e3).only
    
    def s3 = bySelect must contain(e3, e1).only
  }
  
  def transformed = new TestBase with TestComparators with TestFilters {
    def selectionsAfterSorting = {
      model.selections = List(e1)
      sortModelByAgeComparator
      selections
    }
    
    def selectionsAfterFiltered1 = {
      model.selections = List(e1)
      setYoungMatcherToModel()
      selections
    }
    
    def s1 = selectionsAfterSorting must contain(e1).only
    
    def s2 = selectionsAfterFiltered1 must contain(e1).only
  }
  
  def reactions = new TestBase {
    def published = {
      var isPublished = false
      model.reactions += {
        case DataListSelectionChanged(_, _, _) =>
          isPublished = true
      }
      selectionModel setSelectionInterval (0, 0)
      isPublished
    }
    
    def adjustingValue = {
      var adjusting = false
      model.reactions += {
        case DataListSelectionChanged(_, isAdjusting, _) =>
          adjusting = isAdjusting
      }
      selectionModel setValueIsAdjusting true
      selectionModel setSelectionInterval (0, 0)
      adjusting
    }
    
    def selectionsValue = {
      var selVal = List.empty[TestElement]
      model.reactions += {
        case DataListSelectionChanged(_, _, selections) =>
          selVal = selections.asInstanceOf[List[TestElement]]
      }
      selectionModel setSelectionInterval (0, 1)
      selVal
    }
    
    def s1 = published must beTrue
    
    def s2 = adjustingValue must beTrue
    
    def s3 = selectionsValue must contain(e1, e2).only.inOrder
  }
}

object DataListModelSpec {
  private[DataListModelSpec] case class TestElement(
    name: String,
    age: Int,
    addr: String
  )
}
