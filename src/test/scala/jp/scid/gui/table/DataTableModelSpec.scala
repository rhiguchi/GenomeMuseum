package jp.scid.gui.table

import org.specs2._
import mock._

import java.util.Comparator
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.matchers.{Matcher, MatcherEditor}

class DataTableModelSpec extends Specification with Mockito {
  import DataListModelSpec.TestElement
  
  def is = "DataTableModel" ^
    "tableModel" ^
      "列数" ! test.s1 ^
      "行数" ! test.s2 ^
      "列名" ! test.s3 ^
      "値" ! test.s4 ^
    bt ^ "columnModel" ^
      "列数" ! test.s5 ^
      "識別子取得" ! test.s6 ^
      "TableColumn" ! test.s7
      
  val format = new TableFormat[TestElement] {
    def getColumnCount() = 2
    def getColumnName(column: Int) = column match {
      case 0 => "name"
      case 1 => "age"
      case _ => throw new IllegalArgumentException
    }
    def getColumnValue(baseObject: TestElement, column: Int) = column match {
      case 0 => baseObject.name
      case 1 => baseObject.age.asInstanceOf[AnyRef]
      case _ => throw new IllegalArgumentException
    }
  }
  
  trait ModelPrep  {
    val model = new DataTableModel(format)
    
    val elements = List(
      TestElement("user4", 20),
      TestElement("user2", 40),
      TestElement("user1", 30),
      TestElement("user3", 10)
    )
    
    model.source = elements
    
    def tableModel = model.tableModel
    
    def columnModel = model.columnModel
  }
    
  def test = new ModelPrep {
    def s1 = tableModel.getColumnCount must_== 2
    
    def s2 = tableModel.getRowCount must_== 4
    
    def s3 = tableModel.getColumnName(0) must_== "name" and
      (tableModel.getColumnName(1) must_== "age")
    
    def s4_1 = tableModel.getValueAt(0, 0) must_== "user4"
    def s4_2 = tableModel.getValueAt(2, 1) must_== 30
    def s4 = s4_1 and s4_2
    
    def s5 = columnModel.getColumnCount must_== 2
    
    def s6_1 = columnModel.getColumnIndex("name") must_== 0
    def s6_2 = columnModel.getColumnIndex("age") must_== 1
    def s6 = s6_1 and s6_2
    
    def s7_1 = columnModel.getColumn(0).getHeaderValue must_== "name"
    def s7_2 = columnModel.getColumn(1).getHeaderValue must_== "age"
    def s7_3 = columnModel.getColumn(0).getIdentifier must_== "name"
    def s7_4 = columnModel.getColumn(1).getIdentifier must_== "age"
    def s7_5 = columnModel.getColumn(0).getModelIndex must_== 0
    def s7_6 = columnModel.getColumn(1).getModelIndex must_== 1
    def s7 = s7_1 and s7_2 and s7_3 and s7_4 and s7_5 and s7_6
  }
}

object DataListModelSpec {
  case class TestElement(
    name: String,
    age: Int
  )
}