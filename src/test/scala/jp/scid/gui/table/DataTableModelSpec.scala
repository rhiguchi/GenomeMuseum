package jp.scid.gui.table

import org.specs2._
import mock._

import java.util.Comparator
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.matchers.{Matcher, MatcherEditor}

class DataTableModelSpec extends Specification with Mockito {
  import DataTableModelSpec.{TestElement, TestElementTableFormat}
  
  def is = "DataTableModel" ^ sequential ^
    "初期プロパティ" ^
      "visibledColumns" ! initial.s1 ^
    bt ^ "tableModel" ^
      "列数" ! tableModel.s1 ^
      "行数" ! tableModel.s2 ^
      "列名" ! tableModel.s3 ^
      "値" ! tableModel.s4 ^
    bt ^ "columnModel" ^
      "列数" ! columnModel.s1 ^
    bt ^ "visibledColumns" ^
      "識別子からの変更" ! visibledColumns.s1 ^
      "列モデルからの変更" ! visibledColumns.s2
  
  private[DataTableModelSpec] class TestBase {
    val model = new DataTableModel(TestElementTableFormat)
    
    val e1 = TestElement("element1", 30, "apple")
    val e2 = TestElement("element2", 50, "bread")
    val e3 = TestElement("element3", 10, "chair")
    val e4 = TestElement("element4", 20, "dog")
    
    model.source = List(e1, e2, e3, e4)
    
    def tableModel = model.tableModel
    
    def columnModel = model.columnModel
  }
  
  def initial = new TestBase {
    def s1 = model.visibledColumns must contain("name", "age", "addr").only.inOrder
  }
  
  def tableModel = new TestBase {
    def getColumnName(index: Int) = tableModel.getColumnName(index)
    def getValueAt(row: Int, column: Int) = tableModel.getValueAt(row, column)
    
    def s1 = tableModel.getColumnCount must_== 3
    
    def s2 = tableModel.getRowCount must_== 4
    
    def s3_1 = getColumnName(0) must_== "name"
    def s3_2 = getColumnName(1) must_== "age"
    def s3_3 = getColumnName(2) must_== "addr"
    def s3 = s3_1 and s3_2 and s3_3
    
    def s4_1 = getValueAt(0, 0) must_== "element1"
    def s4_2 = getValueAt(1, 1) must_== 50
    def s4_3 = getValueAt(2, 2) must_== "chair"
    def s4 = s4_1 and s4_2 and s4_3
  }
  
  def columnModel = new TestBase {
    def s1 = columnModel.getColumnCount must_== 3
  }
  
  def visibledColumns = new TestBase {
    def byProperty = {
      model.visibledColumns = List("addr", "age")
      
      Range(0, model.visibledColumns.size) map columnModel.getColumn map
        (_.getIdentifier.toString) toList
    }
    
    def byColumnModel = {
      columnModel.moveColumn(0, 1)
      model.visibledColumns
    }
    
    def s1 = byProperty must contain("addr", "age").only.inOrder
    
    def s2 = byColumnModel must contain("age", "name", "addr").only.inOrder
  }
}

private[table] object DataTableModelSpec {
  case class TestElement(
    name: String,
    age: Int,
    addr: String
  )
  
  object TestElementTableFormat extends TableFormat[TestElement] {
    val columnNames = Array("name", "age", "addr")
    def getColumnCount = 3
    def getColumnName(column: Int) = columnNames(column)
    def getColumnValue(e: TestElement, column: Int) = column match {
      case 0 => e.name
      case 1 => e.age.asInstanceOf[AnyRef]
      case 2 => e.addr
    }
  }
}