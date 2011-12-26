package jp.scid.gui.table

import org.specs2._

import java.util.Comparator

import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.gui.AdvancedTableFormat

class StringSortableSpec extends Specification {
  import DataTableModelSpec.{TestElement, TestElementTableFormat}
  
  def is = "StringSortable" ^
    "通常の TableFormat" ^
      "列名ソート" ! normal.s1 ^
      "空白文字でソート無し" ! normal.s2 ^
      "カンマ区切りで複数条件" ! normal.s3 ^
      "逆方向ソート" ! normal.s4 ^
    bt ^ "AdvancedTableFormat" ^
      "列名ソート" ! advanced.s1 ^
      "空白文字でソート無し" ! advanced.s2 ^
      "null を返す列名でソート無し" ! advanced.s3 ^
      "逆方向ソート" ! advanced.s4
  
  object AdvancedTestElementTableFormat extends AdvancedTableFormat[TestElement] {
    def getColumnCount = TestElementTableFormat.getColumnCount
    def getColumnName(column: Int) = TestElementTableFormat.getColumnName(column)
    def getColumnValue(e: TestElement, column: Int) =
      TestElementTableFormat.getColumnValue(e, column)
    def getColumnClass(column: Int) = classOf[Object]
    def getColumnComparator(column: Int) = column match {
      case 0 => null
      case 1 => new Comparator[TestElement] {
        def compare(o1: TestElement, o2: TestElement) =
          o1.age.compareTo(o2.age)
      }
      case 2 => new Comparator[TestElement] {
        def compare(o1: TestElement, o2: TestElement) =
          o1.addr.compareTo(o2.addr)
      }
    }
  }
  
  private[StringSortableSpec] class TestBase {
    def createModel = new DataTableModel[TestElement](TestElementTableFormat)
      with StringSortable[TestElement]
    
    val model = createModel
    
    val e1 = TestElement("elementA", 30, "bread")
    val e2 = TestElement("elementC", 50, "apple")
    val e3 = TestElement("elementB", 10, "chair")
    val e4 = TestElement("elementA", 20, "dog")
    
    model.source = List(e1, e2, e3, e4)
    
    def viewSource = model.viewSource
    
    def sortWithEmpty = {
      model.orderStatement = ""
      viewSource
    }
  }
  
  def normal = new TestBase {
    def sortWithAddr = {
      model.orderStatement = "addr"
      viewSource
    }
    
    def sortWithNameAndAge = {
      model.orderStatement = "name, age"
      viewSource
    }
    
    def sortWithAgeDesc = {
      model.orderStatement = "age desc"
      viewSource
    }
    
    def s1 = sortWithAddr must contain(e2, e1, e3, e4).only.inOrder
    
    def s2 = sortWithEmpty must contain(e1, e2, e3, e4).only.inOrder
    
    def s3 = sortWithNameAndAge must contain(e4, e1, e3, e2).only.inOrder
    
    def s4 = sortWithAgeDesc must contain(e2, e1, e4, e3).only.inOrder
  }
  
  def advanced = new TestBase {
    override def createModel = new DataTableModel[TestElement](AdvancedTestElementTableFormat)
      with StringSortable[TestElement]
    
    def sortWithAge = {
      model.orderStatement = "age"
      viewSource
    }
    
    def sortWithNullColumn = {
      model.orderStatement = "name"
      viewSource
    }
    
    def sortWithAddrDesc = {
      model.orderStatement = "addr desc"
      viewSource
    }
    
    def s1 = sortWithAge must contain(e3, e4, e1, e2).only.inOrder
    
    def s2 = sortWithEmpty must contain(e1, e2, e3, e4).only.inOrder
    
    def s3 = sortWithNullColumn must contain(e1, e2, e3, e4).only.inOrder
    
    def s4 = sortWithAddrDesc must contain(e4, e3, e1, e2).only.inOrder
  }
}
