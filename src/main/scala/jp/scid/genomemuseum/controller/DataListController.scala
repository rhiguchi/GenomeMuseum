package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, JLabel, JComponent, TransferHandler, DropMode}

import jp.scid.gui.ValueHolder
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.model.UserExhibitRoom

/**
 * テーブルと検索フィールドの操作反応基底クラス。
 */
abstract class DataListController extends GenomeMuseumController {
  /**
   * 読み込みマネージャを利用してクラスを作成する
   */
  def this(loadManager: MuseumExhibitLoadManager) {
    this()
    this.loadManager = Option(loadManager)
  }
  
  // モデル
  /** テーブルがドラッグ不可設定 */
  protected[controller] def isTableDraggable = false
  /** テーブルモデル */
  def tableModel: DataTableModel[_] with TableColumnSortable[_]
  /**
   * 検索文字列モデル。
   * 
   * @see #searchTextChange {@code ValueChange} 発行時に呼び出される。
   */
  val searchTextModel: ValueHolder[String] = new ValueHolder("") {
    reactions += {
      case ValueChange(_, _, newValue: String) => searchTextChange(newValue)
    }
  }
  /** 状態文字列モデル */
  val statusTextModel: ValueHolder[String] = new ValueHolder("")
  
  // コントローラ
  /** 転送ハンドラ */
  val tableTransferHandler = new TransferHandler("")
  /** 読み込み処理 */
  var loadManager: Option[MuseumExhibitLoadManager] = None
  
  // アクション
  /** 項目削除アクション */
  def tableDeleteAction: Option[javax.swing.Action] = None
  
  /**
   * {@code searchTextModel} が変更された時に呼び出される。検索処理に利用する。
   */
  protected def searchTextChange(newValue: String)
  
  /**
   * 展示物ファイルの読み込みを行う
   */
  def loadExhibit(file: java.io.File, targetRoom: Option[UserExhibitRoom]) =
    loadManager.get.loadExhibit(file)
  
  /**
   * このコントローラの検索フィールドモデルを JTextField へ結合
   * 
   * @return 結合保持オブジェクト
   */
  def bindSearchField(field: JTextField) = {
    ValueHolder.connect(searchTextModel, field)
  }
  
  /**
   * このコントローラのテーブルモデルを JTable へ結合
   * 
   * @return 結合保持オブジェクト
   */
  def bindTable(table: JTable) = {
    DataTableModel.connect(tableModel, table)
    table.setDragEnabled(isTableDraggable)
    table.setDropMode(javax.swing.DropMode.INSERT_ROWS)
    table setTransferHandler tableTransferHandler
    table.getActionMap.put("delete", tableDeleteAction.getOrElse(null))
    // 親への転送ハンドラ適用
    table.getParent match {
      case parent: JComponent => parent setTransferHandler tableTransferHandler
      case _ =>
    }
    
    val headerConn = TableColumnSortable.connect(tableModel, table.getTableHeader)
    List(headerConn)
  }
}

