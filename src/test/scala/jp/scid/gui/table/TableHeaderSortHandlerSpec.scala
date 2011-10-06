package jp.scid.gui.table

import org.specs2.mutable._
import java.util.Comparator
import javax.swing.table.{JTableHeader, TableColumn, TableColumnModel}

class TableHeaderSortHandlerSpec extends Specification {
  /** TableColumn 作成 */
  def createColumn(modelIndex: Int, identifier: String) = {
    val column = new TableColumn(modelIndex)
    column.setIdentifier(identifier)
    
    column
  }
  
  /** SortableColumn 作成 */
  def createSortableColumn(modelIndex: Int, identifier: String, stmts: List[String]) = {
    val column = new TableColumn(modelIndex) with SortableColumn {
      val orderStatements = stmts
    }
    column.setIdentifier(identifier)
    
    column
  }
  
  /** カラムモデルにカラムを追加 */
  def insertColumnsTo(columnModel: TableColumnModel) {
    columnModel.addColumn(createColumn(0, "col1"))
    columnModel.addColumn(createColumn(1, "col2"))
    columnModel.addColumn(createColumn(2, "col3"))
    columnModel.addColumn(createSortableColumn(3, "col4", List("name", "addr")))
    columnModel.addColumn(createColumn(4, "forupdate"))
    columnModel.addColumn(createSortableColumn(5, "forupdate", List("name", "addr", "age")))
  }
  
  "TableHeaderSortHandler" should {
    val tableHeader = new JTableHeader
    val handler = new TableHeaderSortHandler(tableHeader)
    insertColumnsTo(tableHeader.getColumnModel)
    val selectionModel = tableHeader.getColumnModel.getSelectionModel
    
    "列選択" in {
      selectionModel.clearSelection()
      
      handler.headerClick(0)
      selectionModel.isSelectedIndex(0) must beTrue
      
      handler.headerClick(1)
      selectionModel.isSelectedIndex(1) must beTrue
      
      handler.headerClick(2)
      selectionModel.isSelectedIndex(2) must beTrue
    }
    
    "列整列情報リストの取得" in {
      handler.ordersFor(0) must_== List("col1", "col1 desc")
      handler.ordersFor(1) must_== List("col2", "col2 desc")
      handler.ordersFor(2) must_== List("col3", "col3 desc")
    }
    
    "列整列情報の取得" in {
      handler.currentOrder(0) must_== "col1"
      handler.currentOrder(1) must_== "col2"
      handler.currentOrder(2) must_== "col3"
    }
    
    "SortableColumn での列整列情報リストの取得" in {
      handler.ordersFor(3) must_== List("name", "addr")
    }
    
    "SortableColumn での列整列情報の取得" in {
      handler.currentOrder(3) must_== "name"
    }
    
    "列整列情報の設定" in {
      handler.updateOrderFor(4, "newsort")
      handler.currentOrder(4) must_== "newsort"
      
      handler.updateOrderFor(4, "sort")
      handler.currentOrder(4) must_== "sort"
    }
    
    "SortableColumn への列整列情報の設定" in {
      val column = tableHeader.getColumnModel.getColumn(5).asInstanceOf[SortableColumn]
      handler.updateOrderFor(5, "newsort")
      column.orderStatement must_== "newsort"
    }
    
    "選択列の再クリックでの列整列情報の変更" in {
      val tableHeader = new JTableHeader
      val handler = new TableHeaderSortHandler(tableHeader)
      insertColumnsTo(tableHeader.getColumnModel)
      
      handler.currentOrder(2) must_== "col3"
      
      tableHeader.getColumnModel.getSelectionModel.setSelectionInterval(2, 2)
      handler.headerClick(2)
      handler.currentOrder(2) must_== "col3 desc"
    }
    
    "SortableColumn 選択列の再クリックでの列整列情報の変更" in {
      val tableHeader = new JTableHeader
      val handler = new TableHeaderSortHandler(tableHeader)
      insertColumnsTo(tableHeader.getColumnModel)
      
      handler.currentOrder(5) must_== "name"
      
      tableHeader.getColumnModel.getSelectionModel.setSelectionInterval(5, 5)
      handler.headerClick(5)
      handler.currentOrder(5) must_== "addr"
      
      handler.headerClick(5)
      handler.currentOrder(5) must_== "age"
    }
    
    "選択列の再クリックでの列整列情報の変更（周回）" in {
      val tableHeader = new JTableHeader
      val handler = new TableHeaderSortHandler(tableHeader)
      insertColumnsTo(tableHeader.getColumnModel)
      
      handler.updateOrderFor(5, "addr")
      
      tableHeader.getColumnModel.getSelectionModel.setSelectionInterval(5, 5)
      handler.headerClick(5)
      handler.currentOrder(5) must_== "age"
      
      handler.headerClick(5)
      handler.currentOrder(5) must_== "name"
    }
    
    "文字列比較器エディタへの値の適用" in {
      val tableHeader = new JTableHeader
      val handler = new TableHeaderSortHandler(tableHeader)
      insertColumnsTo(tableHeader.getColumnModel)
      
      handler.headerClick(5)
      handler.comparatorEditor.orderStatement must_== "name"
      
      handler.headerClick(5)
      handler.comparatorEditor.orderStatement must_== "addr"
      
      handler.headerClick(5)
      handler.comparatorEditor.orderStatement must_== "age"
    }
  }
}
