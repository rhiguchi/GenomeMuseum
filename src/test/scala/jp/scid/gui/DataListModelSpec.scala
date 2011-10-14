package jp.scid.gui

import org.specs2._
import mock._

import java.util.Comparator
import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.matchers.{Matcher, MatcherEditor}

class DataListModelSpec extends Specification with Mockito {
  import DataListModelSpec.TestElement
  import scala.collection.JavaConverters._
  
  def is = "DataListModel" ^
    "source" ^
      "設定と取得" ! test.s1 ^
      "viewSource 取得" ! test.s2 ^
    bt ^ "sortWith" ^
      "Comparator オブジェクト" ! sortTest.s1 ^
      "比較関数" ! sortTest.s2 ^
    bt ^ "filterWith" ^
      "Matcher オブジェクト" ! filterTest.s1 ^
      "MatcherEditor" ! filterTest.s2 ^
    bt ^ "filterUsing" ^
      "文字列設定" ! fieldTest.s1 ^
      "インクリメンタルサーチ" ! fieldTest.s2 ^
    bt ^ "selectedItems" ^
      "設定と解除" ! selectionTest.s1 ^
      "非選択項目数" ! selectionTest.s2 ^
    bt ^ "selectionModel" ^
      "valueChanged コール" ! selEventTest.s1 ^
      "valueChanged イベントオブジェクト" ! selEventTest.s2 ^
    bt ^ "DataListSelectionChanged イベント" ^
      "reaction イベントオブジェクト" ! listSelEventTest.s1 ^
    bt ^ "ソート後の選択項目" ^ pending ^
//      "選択項目" ^ pending ^
//      "選択項目位置" ^ pending ^
    bt ^ "フィルタリング後の選択項目" ^ pending
//      "選択項目" ^ pending ^
//      "選択項目位置" ^ pending
      
  
  trait ModelPrep  {
    val model = new DataListModel[TestElement]
    
    val elements = List(
      TestElement("user4", 20),
      TestElement("user2", 40),
      TestElement("user1", 30)
    )
    
    model.source = elements
    model.sourceWithWriteLock { source =>
      source += TestElement("user3", 10)
    }
  }
    
  def test = new ModelPrep {
    def s1 = model.source must contain(TestElement("user4", 20), 
      TestElement("user2", 40), TestElement("user1", 30), TestElement("user3", 10)) and
        have size(4)
    def s2 = model.viewSource.asScala must contain(TestElement("user4", 20), 
      TestElement("user2", 40), TestElement("user1", 30), TestElement("user3", 10)) and
        have size(4)
  }
    
  def sortTest = new ModelPrep {
    val nameComp = new Comparator[TestElement] {
      def compare(o1: TestElement, o2: TestElement): Int = {
        o1.name.compareTo(o2.name)
      }
    }
    
    val ageComp = (a: TestElement, b: TestElement) => {
      a.age - b.age
    }
    
    def s1_1 = model.viewSource.get(0).name must_== "user1"
    def s1_2 = model.viewSource.get(1).name must_== "user2"
    def s1_3 = model.viewSource.get(2).name must_== "user3"
    def s1_4 = model.viewSource.get(3).name must_== "user4"
    def s1 = {
      model sortWith nameComp
      s1_1 and s1_2 and s1_3 and s1_4
    }
    
    def s2_1 = model.viewSource.get(0).age must_== 10
    def s2_2 = model.viewSource.get(1).age must_== 20
    def s2_3 = model.viewSource.get(2).age must_== 30
    def s2_4 = model.viewSource.get(3).age must_== 40
    def s2 = {
      model sortWith ageComp
      s2_1 and s2_2 and s2_3 and s2_4
    }
  }
  
  def filterTest = new ModelPrep {
    val youngMatcher = new Matcher[TestElement] {
      def matches(item: TestElement): Boolean = {
        item.age <= 20
      }
    }
    
    val user3Matcher = GlazedLists fixedMatcherEditor new Matcher[TestElement] {
      def matches(item: TestElement): Boolean = {
        item.name.equals("user3")
      }
    }
    
    def s1 = {
      model filterWith youngMatcher
      model.viewSource.asScala must contain(TestElement("user3", 10),
        TestElement("user4", 20)) and have size(2)
    }
      
    def s2 = {
      model filterWith user3Matcher
      model.viewSource.asScala must contain(TestElement("user3", 10)) and have size(1)
    }
  }
  
  def fieldTest = new ModelPrep {
    import scala.collection.mutable.Buffer
    import java.awt.event.KeyEvent
    
    val field = new javax.swing.JTextField {
      def pushKey0 = {
        processKeyEvent(new KeyEvent(this, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_0, '0'))
      }
    }
    val filterator = (buf: Buffer[String], e: TestElement) => {
      buf += e.name
      buf += e.age.toString
    }: Unit
    
    field.setText("3")
    model.filterUsing(field, filterator)
    
    def s1 = model.viewSource.asScala must contain(TestElement("user3", 10),
      TestElement("user1", 30)) and have size(2)
    
    def s2 = {
      // キータイプシミュレート
      field.setText("30")
      field.pushKey0
      model.viewSource.asScala must contain(TestElement("user1", 30)) and have size(1)
    }
  }
  
  def selectionTest = new ModelPrep {
    import scala.collection.JavaConverters._
    
    assert(model.selectedItems.size == 0)
    
    val selItem1 = TestElement("user3", 10)
    val selItem2 = TestElement("user1", 30)
    model.selectedItemsWithWriteLock { _ => model.selectedItems add selItem1 }
    model.selectedItemsWithWriteLock { _ += selItem2 }
    model.deselectedItemsWithWriteLock { _ += selItem2 }
    
    def s1 = model.selectedItems.asScala must contain (selItem1) and have size(1)
    
    def s2 = model.deselectedItemsWithReadLock(_.size) must_== 3
  }
  
  def selEventTest = new ModelPrep {
    import javax.swing.event.{ListSelectionListener, ListSelectionEvent}
    import scala.collection.JavaConverters._
    
    val listener = mock[ListSelectionListener]
    model.selectionModel addListSelectionListener listener
    // イベントオブジェクト保持
    var event: ListSelectionEvent = null
    listener.valueChanged(any) answers { e =>
      event = e.asInstanceOf[ListSelectionEvent]
      e
    }
    
    val selItem = TestElement("user2", 40)
    model.selectedItems add selItem
    
    def s1 = there was one(listener).valueChanged(any)
    
    def s2_1 = event.getSource must_== model.selectionModel
    def s2_2 = event.getFirstIndex must_== 1
    def s2_3 = event.getLastIndex must_== 1
    def s2_4 = event.getValueIsAdjusting must beFalse
    def s2 = s2_1 and s2_2 and s2_3 and s2_4
  }
  
  def listSelEventTest = new ModelPrep {
    import jp.scid.gui.event.DataListSelectionChanged
    
    // リアクション設定とオブジェクト保持
    var event: DataListSelectionChanged[TestElement] = null
    model.reactions += {
      case e: DataListSelectionChanged[_] =>
        event = e.asInstanceOf[DataListSelectionChanged[TestElement]]
    }
    
    val selItem = TestElement("user2", 40)
    model.selectedItems add selItem
        
    def s1_1 = event.source must_== model
    def s1_2 = event.isAdjusting must beFalse
    def s1_3 = event.selections must contain(selItem) and have size(1)
    def s1 = s1_1 and s1_2 and s1_3
  }
}

object DataListModelSpec {
  case class TestElement(
    name: String,
    age: Int
  )
}