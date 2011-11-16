package jp.scid.gui.table

import org.specs2._
import mock._

import javax.swing.table.TableColumn

class TableColumnSortableSpec extends Specification {
  import DataTableModelSpec.{TestElement, TestElementTableFormat}
  
  def is = "TableColumnSortable" ^
    "初期プロパティ" ^
      "orderStatemet" ! initial.s1 ^
      "sortColumn" ! initial.s2 ^
    bt ^ "sortColumn" ^
      "列名指定" ! sortColumn.s1 ^
      "無効な列名指定はソートをしない" ! sortColumn.s2 ^
      "不明な並び替え記述をもつ列名指定はソートをしない" ! sortColumn.s3 ^
    bt ^ "updateOrderStatement" ^
      "指定した記述で並び替え" ! updateOrderStatement.s1 ^
    bt ^ "JTableHeader との結合" ^
      "クリック動作で列名指定" ! handler.s1 ^
      "2クリック動作で逆順" ! handler.s2
  
  class TableColumnSortableModel extends DataTableModel[TestElement](TestElementTableFormat)
      with TableColumnSortable[TestElement] {
    override def orderStatementsFor(columName: String) = {
      columName match {
        case "addr" => List("order1", "order2")
        case _ => super.orderStatementsFor(columName)
      }
    }
  }
  
  private[TableColumnSortableSpec] class TestBase {
    val model = new TableColumnSortableModel
    
    val e1 = TestElement("elementA", 30, "apple")
    val e2 = TestElement("elementC", 50, "bread")
    val e3 = TestElement("elementC", 10, "chair")
    val e4 = TestElement("elementB", 20, "dog")
    
    model.source = List(e1, e2, e3, e4)
    
    def viewSource = model.viewSource
    
    def getColumn(index: Int) = model.columnModel.getColumn(index)
      .asInstanceOf[TableColumn with SortableColumn]
  }
  
  def initial = new TestBase {
    def s1_1 = model.orderStatement("name") must_== "name"
    def s1_2 = model.orderStatement("addr") must_== "order1"
    def s1 = s1_1 and s1_2
    
    def s2 = model.sortColumn must_== ""
  }
  
  def sortColumn = new TestBase {
    def nameCol = {
      model.sortColumn = "name"
      viewSource
    }
    
    def invalidColumn = {
      model.sortColumn = "123456"
      viewSource
    }
    
    def invalidStatemetColumn = {
      model.sortColumn = "addr"
      viewSource
    }
    
    def s1 = nameCol must contain(e1, e4, e2, e3).only.inOrder
    
    def s2 = invalidColumn must contain(e1, e2, e3, e4).only.inOrder
    
    def s3 = invalidStatemetColumn must contain(e1, e2, e3, e4).only.inOrder
  }
  
  def updateOrderStatement = new TestBase {
    def updateAge = {
      model.updateOrderStatement("name", "age")
      model.sortColumn = "name"
      
      viewSource
    }
    
    def s1 = updateAge must contain(e3, e4, e1, e2).only.inOrder
  }
  
  def handler = new TestBase {
    import javax.swing.table.JTableHeader
    def ageClick = {
      val tableHeader = new JTableHeader
      val connector = TableColumnSortable.connect(model, tableHeader)
      connector.headerClick(1)
      
      viewSource
    }
    
    def ageTwoClick = {
      val tableHeader = new JTableHeader
      val connector = TableColumnSortable.connect(model, tableHeader)
      connector.headerClick(1)
      connector.headerClick(1)
      
      model
    }
    
    def s1 = ageClick must contain(e3, e4, e1, e2).only.inOrder
    
    def s2_1 = ageTwoClick.orderStatement("age") must_== "age desc"
    def s2_2 = ageTwoClick.sortColumn must_== "age"
    def s2_3 = ageTwoClick.viewSource must contain(e2, e1, e4, e3).only.inOrder
    def s2 = s2_1 and s2_2 and s2_3
  }
}
