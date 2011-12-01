package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, JLabel, JComponent, SwingWorker}

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.{gui, view, model}
import gui.{ExhibitTableModel, WebSearchManager, WebServiceResultsModel}
import view.TaskProgressTableCell
import model.{SearchResult, HttpDownloader, TaskProgressModel}
import WebSearchManager._

object WebServiceResultController {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[WebServiceResultController])
}

class WebServiceResultController(
  dataTable: JTable,
  quickSearchField: JTextField
) extends DataListController(dataTable, quickSearchField) {
  import WebServiceResultController._
  
  // モデル
  /** タスクが実行中であるかの状態を保持 */
  val isProgress = new ValueHolder(false)
  /** テーブルモデル */
  private[controller] val tableModel = new WebServiceResultsModel
  /** 読み込みマネージャ */
  var loadManager: Option[MuseumExhibitLoadManager] = None
  /** 現在の検索の該当数 */
  private var currentCount = 0
  /** 現在の検索している文字列 */
  private var currentQuery = ""
  /** 検索実行 Actor ID */
  private val searchingId = new java.util.concurrent.atomic.AtomicInteger
  
  /** ダウンロードボタンアクション */
  val downloadAction = swing.Action("Download") {
    downloadBioDataOnEditingRow()
    // プログレスバー表示の有効化と、ソース変更時にエディタが残るのを防ぐため。
    dataTable.removeEditor()
  }
  
  // モデルバインド
  /** Web 検索文字列の変更 */
  searchTextModel.reactions += {
    case ValueChange(_, _, newValue) =>
      // 実行遅延
      val myId = searchingId.incrementAndGet()
      actors.Actor.actor {
        Thread.sleep(1000)
        if (searchingId.get == myId)
          researching()
      }
  }
  
  /**
   * クリックされたダウンロードボタンに対応するデータをダウンロードする。
   */
  def downloadBioDataOnEditingRow() {
    logger.debug("ダウンロード")
    
    val item = tableModel.viewItem(dataTable.getEditingRow)
    val task = createDownloadTask(item)
    
    DownloadTask.propertyBind(task) { (prop, value) =>
      import SwingWorker.StateValue._
      import util.control.Exception.catching
      // 値の更新
      item.state = task.getState match {
        case DONE => task.isCancelled match {
          case true => PENDING
          case false =>
            catching(classOf[Exception]).opt(task.get) match {
              case Some(_) => DONE
              case None => PENDING
            }
        }
        case state => state
      }
      item.progress = task.getProgress
      item.label = item.state match {
        case STARTED => item.identifier + " is downloaing..."
        case _ => item.identifier
      }
      // 更新通知
      tableModel.updated(item)
    }
    
    task.execute
  }
  
  private def researching() {
    val searchQuery = searchTextModel().trim.split("\\s+").mkString(" ")
    if (searchQuery.length >= 3 && currentQuery != searchQuery) {
      logger.debug("Search query: {}", searchQuery)
      currentQuery = searchQuery
      tableModel searchWith searchQuery
    }
  }
  
  def createDownloadTask(item: SearchResult) = {
    new DownloadTask(item.sourceUrl.get.toString) {
      override def done() {
        if (!isCancelled) {
          logger.trace("ダウンロード完了 {}", item.sourceUrl.get.toString)
          val file = get()
          loadManager.map(_.loadExhibits(List(file)))
        }
        else {
          logger.trace("ダウンロードキャンセル {}", item.sourceUrl.get.toString)
        }
      }
    }
  }
  
  def downloadingTableCell: Option[TaskProgressTableCell] = {
    dataTable.getDefaultRenderer(classOf[TaskProgressModel]) match {
      case renderer: TaskProgressTableCell => Some(renderer)
      case _ => None
    }
  }
  
  /** 検索状態の更新 */
  tableModel.reactions += {
    case Started() =>
      currentCount = 0
      isProgress := true
      statusTextModel := "%s - 検索中...".format(searchTextModel())
    case CountRetrieved(count) =>
      currentCount = count
      statusTextModel := "%s - %d 件該当...".format(searchTextModel(), currentCount)
    case IdentifiersRetrieved(_) =>
      statusTextModel := "%s - %d 件の識別を取得...".format(searchTextModel(), currentCount)
    case EntryValuesRetrieving() =>
      statusTextModel := "%s - %d 件の情報を取得中...".format(searchTextModel(), currentCount)
    case RetrievingTimeOut() =>
      statusTextModel := "%s - %d 件（Web サービスから応答が無いため切断しました）".format(searchTextModel(), currentCount)
    case Canceled() =>
      statusTextModel := "%s - %d 件".format(searchTextModel(), currentCount)
    case Success() =>
      statusTextModel := "%s - %d 件".format(searchTextModel(), currentCount)
    case Done() =>
      isProgress := false
  }
  
  override def bind() = {
    val connList = super.bind()
    
    downloadingTableCell.map(_.setExecuteButtonAction(downloadAction.peer))
    
    connList
  }
}

import java.net.URL
import actors.Actor
import java.io.File

object DownloadTask {
  def propertyBind(task: DownloadTask)(changeTask: (String, AnyRef) => Unit) {
    import java.beans.{PropertyChangeListener, PropertyChangeEvent}
    
    task addPropertyChangeListener new PropertyChangeListener {
      def propertyChange(evt: PropertyChangeEvent) {
        changeTask(evt.getPropertyName, evt.getNewValue)
      }
    }
  }
}

class DownloadTask(url: String) extends SwingWorker[File, Unit] {
  import HttpDownloader._
  
  private lazy val downloader = new HttpDownloader(url)
  
  @volatile var size = 0L
  @volatile var downloaded = 0L
  
  private object Publisher extends Actor {
    def act() {
      react {
        case Start(_, length) =>
          size = length
          publish()
          act()
        case InProgress(_, progress) =>
          downloaded = progress
          publish()
          act()
        case Done(_) =>
          exit
        }
    }
  }
  
  def doInBackground = {
    downloader.outputChannel = Some(Publisher)
    
    try {
      downloader.call()
    }
    catch {
      case e: Throwable => 
        e.printStackTrace
        Publisher ! Done(downloader)
        throw e
    }
  }
}

