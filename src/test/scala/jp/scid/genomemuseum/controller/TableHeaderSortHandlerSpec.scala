package jp.scid.genomemuseum.controller

import org.specs2.mutable._
import java.util.Comparator
import javax.swing.table.{JTableHeader, TableColumn, TableColumnModel}

class TableHeaderSortHandlerSpec extends Specification {
  def createColumn(modelIndex: Int, identifier: String) = {
    val column = new TableColumn(modelIndex)
    column.setIdentifier(identifier)
    
    column
  }
  def insertColumnsTo(columnModel: TableColumnModel) {
    columnModel.addColumn(createColumn(0, "col1"))
    columnModel.addColumn(createColumn(0, "col2"))
    columnModel.addColumn(createColumn(0, "col3"))
  }
  
  def factory[E](stmt: String) = new Comparator[E] {
    def compare(o1: E, o2: E) = 0
  }
  
  "TableHeaderSortHandler" should {
    "列選択" in {
      val tableHeader = new JTableHeader
      val columnModel = tableHeader.getColumnModel
      insertColumnsTo(columnModel)
      val selectionModel = columnModel.getSelectionModel
      selectionModel.clearSelection()
      
      val handler = new TableHeaderSortHandler[Nothing](tableHeader, factory _)
      
      handler.headerClick(0)
      selectionModel.isSelectedIndex(0) must beTrue
      
      handler.headerClick(1)
      selectionModel.isSelectedIndex(1) must beTrue
      
      handler.headerClick(2)
      selectionModel.isSelectedIndex(2) must beTrue
    }
    
    "列ソート状態" in {
      val tableHeader = new JTableHeader
      val columnModel = tableHeader.getColumnModel
      insertColumnsTo(columnModel)
      val selectionModel = columnModel.getSelectionModel
      selectionModel.clearSelection()
      
      val handler = new TableHeaderSortHandler[Nothing](tableHeader, factory _) {
        override def ordersFor(columnIndex: Int) = {
          if (columnIndex == 2)
            IndexedSeq("firstname", "secondname", "fullname")
          else
            super.ordersFor(columnIndex)
        }
      }
      
      handler.currentOrderFor(0) must_== ""
      handler.currentOrderFor(1) must_== ""
      handler.currentOrderFor(2) must_== ""
      
      handler.ordersFor(0) must_== IndexedSeq("col1 asc", "col1 desc")
      handler.ordersFor(1) must_== IndexedSeq("col2 asc", "col2 desc")
      
      handler.headerClick(0)
      handler.currentOrderFor(0) must_== "col1 asc"
      handler.headerClick(0)
      handler.currentOrderFor(0) must_== "col1 desc"
      
      handler.headerClick(1)
      handler.currentOrderFor(1) must_== "col2 asc"
      
      handler.headerClick(0)
      handler.currentOrderFor(0) must_== "col1 desc"
      
      handler.headerClick(0)
      handler.currentOrderFor(0) must_== "col1 asc"
      
      handler.headerClick(2)
      handler.currentOrderFor(2) must_== "firstname"
      
      handler.headerClick(2)
      handler.currentOrderFor(2) must_== "secondname"
      
      handler.headerClick(2)
      handler.currentOrderFor(2) must_== "fullname"
    }
    
    "エディタ値" in {
      val tableHeader = new JTableHeader
      val columnModel = tableHeader.getColumnModel
      insertColumnsTo(columnModel)
      val selectionModel = columnModel.getSelectionModel
      selectionModel.clearSelection()
      
      val handler = new TableHeaderSortHandler[Nothing](tableHeader, factory _)
      
      handler.headerClick(0)
      handler.comparatorEditor.orderStatement must_== "col1 asc"
      
      handler.headerClick(1)
      handler.comparatorEditor.orderStatement must_== "col2 asc"
      handler.headerClick(1)
      handler.comparatorEditor.orderStatement must_== "col2 desc"
      handler.headerClick(1)
      handler.comparatorEditor.orderStatement must_== "col2 asc"
      
      handler.headerClick(0)
      handler.comparatorEditor.orderStatement must_== "col1 asc"
      handler.headerClick(0)
      handler.comparatorEditor.orderStatement must_== "col1 desc"
      
      handler.headerClick(2)
      handler.comparatorEditor.orderStatement must_== "col3 asc"
    }
    
  }
}
