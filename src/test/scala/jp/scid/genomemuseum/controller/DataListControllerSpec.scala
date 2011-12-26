package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField}

import org.specs2._

import DataListController.View

trait DataListControllerSpec {
  this: Specification =>
  
  private type Factory = View => _ <: DataListController
  
  class TestBase(f: Factory) {
    val searchField = new JTextField
    val table = new JTable
    val view = View(table, searchField)
    val ctrl = f(view)
  }
  
  def searchTextModelSpec(f: Factory) =
    "初期内容は空" ! searchTextModel(f).isEmptyOnDefault ^
    "フィールドの内容が適用" ! searchTextModel(f).appliedFromSearchField ^
    bt
  
  // テーブルビュー
  def viewTableSpec(f: Factory) =
    "tableModel のモデルが結合" ! viewTable(f).bindsTableModel ^
    "転送ハンドラの適用" ! viewTable(f).hasTransferrable ^
    bt
  
  // 検索文字列モデル
  def searchTextModel(f: Factory) = new TestBase(f) {
    def isEmptyOnDefault = ctrl.searchTextModel() must beEmpty
    
    private def setTextAndModelGet(text: String) = {
      searchField.setText(text)
      ctrl.searchTextModel()
    }
    
    def appliedFromSearchField = {
      val values = List("12345", "", "abcde")
      val results = values map setTextAndModelGet
      results must_== values
    }
  }
  
  // ビューのテーブル
  def viewTable(f: Factory) = new TestBase(f) {
    def bindsTableModel = table.getModel must_==
      ctrl.tableModel.tableModel
    
    def hasTransferrable = table.getTransferHandler must_== ctrl.tableTransferHandler
  }
}
