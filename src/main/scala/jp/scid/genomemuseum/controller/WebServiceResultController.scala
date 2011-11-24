package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, JLabel, JComponent, SwingWorker}

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.{gui}
import gui.{ExhibitTableModel, WebSearchManager, WebServiceResultsModel}
import WebServiceResultsModel._

class WebServiceResultController(
  dataTable: JTable,
  quickSearchField: JTextField,
  statusField: JLabel,
  progressView: JComponent
) extends DataListController(dataTable, quickSearchField, statusField) {
  // モデル
  /** タスクが実行中であるかの状態を保持 */
  val isProgress = new ValueHolder(false)
  /** テーブルモデル */
  private[controller] val tableModel = new WebServiceResultsModel
  
  private var currentCount = 0
  
  // モデルバインド
  /** Web 検索文字列の変更 */
  searchTextModel.reactions += {
    case ValueChange(_, _, newValue) =>
      println("searching query: " + newValue)
      tableModel searchWith newValue.asInstanceOf[String]
  }
  
  class SearchingTask extends SwingWorker[Void, Void] {
    def doInBackground() = {
      null
    }
  }
  
  /** 検索状態の更新 */
  tableModel.reactions += {
    case Started() =>
      currentCount = 0
      isProgress := true
      statusTextModel := "該当数を取得中..."
    case CountRetrivingTimeOut() =>
      currentCount = -1
      statusTextModel := "Web サービスへの接続に失敗しました。"
    case CountRetrieved(count) =>
      currentCount = count
      statusTextModel := "%d 件のデータを取得中...".format(currentCount)
    case DataRetrivingTimeOut() =>
      currentCount = -1
      statusTextModel := "データを取得できませんでした。"
    case Done() =>
      if (currentCount >= 0) {
        statusTextModel := "%d 件".format(currentCount)
      }
      isProgress := false
  }
  
  override def bind() = {
    val connList = super.bind()
    val progConn = ValueHolder.connectVisible(isProgress, progressView)
    
    List(progConn) ::: connList
  }
}
