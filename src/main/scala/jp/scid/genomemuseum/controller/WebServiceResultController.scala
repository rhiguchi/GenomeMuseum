package jp.scid.genomemuseum.controller

import java.awt.event.ActionEvent
import javax.swing.{JTable, JTextField, JLabel, JComponent, SwingWorker, SwingUtilities}
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}

import org.jdesktop.application.Action

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.ValueChange
import jp.scid.genomemuseum.{gui, view, model}
import gui.{ExhibitTableModel, WebSearchManager, WebServiceResultsModel}
import view.TaskProgressTableCell
import model.{SearchResult, HttpDownloader, TaskProgressModel}
import WebSearchManager._

private object WebServiceResultController {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[WebServiceResultController])
}

class WebServiceResultController extends DataListController {
  import WebServiceResultController._
  /**
   * 読み込みマネージャを利用してクラスを作成する
   */
  def this(loadManager: MuseumExhibitLoadManager) {
    this()
    this.loadManager = Option(loadManager)
  }
  
  // モデル
  /** タスクが実行中であるかの状態を保持 */
  val isProgress = new ValueHolder(false)
  /** テーブルモデル */
  val tableModel = new WebServiceResultsModel
  /** 現在の検索の該当数 */
  private var currentCount = 0
  /** 検索遅延実行用のスケジューラー */
  private val searchScheduler = Executors.newSingleThreadScheduledExecutor
  /** 最新の検索実行のタスク */
  private var scheduledSearchTask: Option[ScheduledFuture[_]] = None
  
  /** ダウンロードボタンアクション */
  val downloadAction = getAction("downloadBioDataOnEditingRow")
  
  /** 指定した文字列で検索を行う */
  protected def searchTextChange(newValue: String) = scheduleSearch(newValue)
  
  /**
   * 検索を遅延実行する。
   * 
   * 検索は最後に追加されてから 1 秒後に実行される。
   * 遅延中に追加を行うと、前のクエリは実行されない。
   * 文字列が前後空白を除いて 3 文字以下のときは、検索を行わない。
   * @return 遅延検索処理の Future
   */
  def scheduleSearch(text: String, delayMillis: Long = 1000) = synchronized {
    // 遅延実行のキャンセル
    scheduledSearchTask.filterNot(_.isDone).map(_.cancel(true))
    scheduledSearchTask = None
    
    val searchQuery = text.trim.split("\\s+").mkString(" ")
    if (searchQuery.length >= 3 && searchQuery != tableModel.searchQuery) {
      // 検索実行タスク
      val runnner = new Runnable {
        def run() { tableModel searchWith searchQuery }
      }
      // 実行遅延
      val future = searchScheduler.schedule(runnner, delayMillis, TimeUnit.MILLISECONDS)
      logger.debug("scheduleSearch: {}", searchQuery)
      scheduledSearchTask = Some(future)
    }
    
    scheduledSearchTask
  }
  
  /**
   * クリックされたダウンロードボタンに対応するデータをダウンロードする。
   */
  @Action
  def downloadBioDataOnEditingRow(event: ActionEvent) {
    val button = event.getSource().asInstanceOf[JComponent]
    val table = SwingUtilities.getAncestorOfClass(classOf[JTable], button).asInstanceOf[JTable]
    logger.debug("ダウンロード")
    
    val item = tableModel.viewItem(table.getEditingRow)
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
    // プログレスバー表示の有効化と、ソース変更時にエディタが残るのを防ぐため。
    table.removeEditor()
  }
  
  def createDownloadTask(item: SearchResult) = {
    new DownloadTask(item.sourceUrl.get.toString) {
      override def done() {
        if (!isCancelled) {
          logger.trace("ダウンロード完了 {}", item.sourceUrl.get.toString)
          val file = get()
          loadExhibit(file, None)
        }
        else {
          logger.trace("ダウンロードキャンセル {}", item.sourceUrl.get.toString)
        }
      }
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
  
  /*
   * テーブル結合
   */
  override def bindTable(table: JTable) = {
    // ダウンロードアクションの設定
    table.getDefaultRenderer(classOf[TaskProgressModel]) match {
      case renderer: TaskProgressTableCell =>
        renderer.setExecuteButtonAction(downloadAction.peer)
      case _ =>
    }
    
    super.bindTable(table)
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

