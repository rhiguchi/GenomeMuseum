package jp.scid.genomemuseum.controller

import java.util.ResourceBundle
import java.io.{File, FileInputStream}
import javax.swing.{JFrame, JTree, JTextField}

import org.jdesktop.application.{Application, Action, ResourceMap}

import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor

import jp.scid.gui.event.{DataListSelectionChanged, DataTreePathsSelectionChanged, StringValueChanged}
import jp.scid.gui.tree.DataTreeModel
import jp.scid.gui.StringValueModel
import DataTreeModel.Path
import jp.scid.genomemuseum.{view, model, gui, GenomeMuseumGUI}
import view.{MainView, MainViewMenuBar}
import model.{MuseumScheme, ExhibitRoom, MuseumStructure, ExhibitListBox, MuseumExhibit}
import gui.{ExhibitTableModel, MuseumSourceModel, WebServiceResultsModel}
import ExhibitListBox.BoxType._

class MainViewController(
  parent: GenomeMuseumGUI,
  mainView: MainView
) {
  import MainViewController._
  import ViewMode._
  // ビュー
  /** ソースリストショートカット */
  private def sourceList = mainView.sourceList
  private def contentViewerSplit = mainView.dataListContentSplit
  
  // コントローラ
  /** コンテントビューワー */
  private val contentViewer = new FileContentViewer(mainView.fileContentView)
  
  // モデル
  /** 現在のスキーマ */
  private var _dataSchema = MuseumScheme.empty
  /** テーブルモデル */
  val tableModel = new ExhibitTableModel
  /** テーブルモデルフィルタリング検索文字列 */
  private val tableFilteringText = new StringValueModel("")
  /** テーブルモデルフィルタリングマッチャー */
  private val filteringMatcherEditor = new SearchEngineTextMatcherEditor
  
  /** ソースリストのツリー構造 */
  val sourceStructure = new MuseumStructure()
  /** ソースリストのモデル */
  val sourceListModel = new MuseumSourceModel(sourceStructure)
  /** ソーティングの時の再選択処理に対応するための、前回の選択項目 */
  private var previousSelections: List[MuseumExhibit] = Nil
  
  /** ウェブソース検索モデル */
  val webServiceResultModel = new WebServiceResultsModel()
  /** ウェブソース検索文字列 */
  private val webSourceSearchText = new StringValueModel("")
  
  /** 現在の表示モード */
  private var currentViewMode: ViewMode = WebSource
  
  
  // モデルバインディング
  // テーブル
  updateDataView()
  
  // ソースリスト
  sourceListModel installTo sourceList
  sourceListModel.sourceListSelectionMode = true
  
  // モデルイベント処理
  // テーブル行選択
  tableModel.reactions += {
    case DataListSelectionChanged(source, isAdjusting, selections) =>
      val s = selections.map(_.asInstanceOf[MuseumExhibit])
      if (!isAdjusting) {
        if (previousSelections != s) {
          println("sel")
          showContent(s)
        }
        previousSelections = s
      }
  }
  
  // ソースリスト項目選択
  sourceListModel.reactions += {
    case e @ DataTreePathsSelectionChanged(source, newPaths, oldPaths) => newPaths match {
      case Nil => sourceListModel.selectPathLocalLibrary()
      case head :: _ =>
        println("treeSelection: " + head)
        setRoomContentsTo(head.last.asInstanceOf[ExhibitRoom])
        
        // ノード削除アクションの使用可不可
        deleteSelectedBoxAction.enabled = newPaths.find(pathDeletable).nonEmpty
    }
  }
  
  // フィルタリング文字列の変更
  tableFilteringText.reactions += {
    case StringValueChanged(source, newValue, oldValue) =>
      tableModel refilterWith newValue
  }
  
  // Web 検索文字列の変更
  webSourceSearchText.reactions += {
    case StringValueChanged(source, newValue, oldValue) =>
      webServiceResultModel searchWith newValue
  }
  
  // アクション
  val addListBoxAction = actionFor("addListBox")
  val addSmartBoxAction = actionFor("addSmartBox")
  addSmartBoxAction.enabled = false
  val addBoxFolderAction = actionFor("addBoxFolder")
  val deleteSelectedBoxAction = actionFor("deleteSelectedBox")
  val deleteSelectedExhibitAction = actionFor("deleteSelectedExhibit")
  
  // アクションバインディング
  // ファイルのドラッグ＆ドロップ追加ハンドラ
  protected val transferHandler = new BioFileTransferHandler(parent)
  mainView.dataTableScroll.setTransferHandler(transferHandler)
  
  mainView.addListBox.setAction(addListBoxAction.peer)
  mainView.addSmartBox.setAction(addSmartBoxAction.peer)
  mainView.addBoxFolder.setAction(addBoxFolderAction.peer)
  mainView.removeBoxButton.setAction(deleteSelectedBoxAction.peer)
  
  mainView.sourceList.getActionMap.put("delete", deleteSelectedBoxAction.peer)
  mainView.dataTable.getActionMap.put("delete", deleteSelectedExhibitAction.peer)
  
  /** リストボックスを追加する。*/
  @Action
  def addListBox() {
    println("addListBox")
    val boxDefaultName = resourceMap.getString("listBox.defaultName")
    // TODO 同名Boxがある時は、連番をつける
    
    processAddBox {
      case Some(parent) => sourceListModel.addListBox(boxDefaultName, parent)
      case None => sourceListModel.addListBox(boxDefaultName)
    }
  }
  
  /** スマートボックスを追加する。*/
  @Action
  def addSmartBox() {
    println("addSmartBox")
    // TODO implement
  }
  
  /** ボックスフォルダを追加する。*/
  @Action
  def addBoxFolder() {
    println("addBoxFolder")
    val boxDefaultName = resourceMap.getString("boxFolder.defaultName")
    // TODO 同名Boxがある時は、連番をつける
    
    processAddBox {
      case Some(parent) => sourceListModel.addBoxFolder(boxDefaultName, parent)
      case None => sourceListModel.addBoxFolder(boxDefaultName)
    }
  }
  
  /** 選択された ExhibitListBox を削除 */
  @Action
  def deleteSelectedBox() {
    println("deleteSelectedBox")
    val boxes = sourceListModel.selectedPaths().sortWith(_.length > _.length)
      .map(_.last).collect{ case b: ExhibitListBox => b }
    boxes foreach sourceListModel.removeElementFromParent
  }
  
  /** 選択された MuseumExhibit を削除 */
  @Action
  def deleteSelectedExhibit() {
    println("deleteSelectedExhibit")
    val selections = tableModel.selectedItemsWithReadLock(a => a)
    // 現在のテーブルモデルがライブラリだと、ファイル削除
    // ユーザーボックスだと、項目のみ削除
    val service = dataSchema.exhibitsService
    tableModel.dataService match {
      case `service` => parent.deleteFromLibrary(selections)
      case _ => tableModel removeElements selections
    }
  }
  
  /** Exhibit の中身を表示 */
  def showContent(exhibits: List[MuseumExhibit]) {
    // TODO 先頭のみ
    val source = exhibits.headOption match {
      case Some(exhibit) => getFileFor(exhibit) match {
        case Some(file) => io.Source.fromFile(file).getLines
        case None => Iterator.empty
      }
      case None => Iterator.empty
    }
    
    // ソースが存在する時はビューワーを開く
    if (source.hasNext)
      showContentViewer()
    
    contentViewer.source = source
  }
  
  /** コンテントビューワーを表示する */
  def showContentViewer() {
    if (mainView.isContentViewerClosed)
      mainView.openContentViewer(200)
  }
  
  /** リソース取得 */
  private def resourceMap = GenomeMuseumGUI.resourceMap(getClass)
  /** アクション取得 */
  private def actionFor(key: String) = GenomeMuseumGUI.actionFor(this, key)
    
  
  /** 現在のデータモデルを取得設定 */
  def dataSchema = _dataSchema
  
  /** ソースリストやデータリストの表示に使用するデータモデルを設定 */
  def dataSchema_=(newSchema: MuseumScheme) {
    _dataSchema = newSchema
    reloadSchema()
  }
  
  /** 選択されているソースの上流のボックスフォルダを探す */
  private def findSelectedBoxFolderPath() = {
    sourceListModel.selectedPaths.headOption
      .getOrElse(IndexedSeq.empty).reverse.dropWhile {
        case parent: ExhibitListBox => parent.boxType != BoxFolder
        case _ => true
      }
      .reverse
  }
  
  /** ボックスの追加前と追加後の処理を行う */
  private def processAddBox(addTask: Option[ExhibitListBox] => ExhibitListBox) {
    import jp.scid.gui.tree.DataTreeModel.convertPathToTreePath
    
    val parentPath = findSelectedBoxFolderPath()
    
    val newBox = parentPath match {
      case Seq(_, _*) =>
        val parent = parentPath.last.asInstanceOf[ExhibitListBox]
        addTask(Some(parent))
      case _ =>
        addTask(None)
    }
    val newBoxPath = parentPath match {
      case Seq(_, _*) => parentPath :+ newBox
      case _ => sourceListModel.pathForUserBoxes :+ newBox
    }
    // 新しいボックスを選択
    sourceListModel.selectPath(newBoxPath)
    // 名前を編集中状態にする
    sourceList.startEditingAtPath(convertPathToTreePath(newBoxPath))
  }
  
  /**
   * データテーブル領域に表示するコンテンツを設定する
   * 通常は、ソースリストの選択項目となる
   */
  private def setRoomContentsTo(newRoom: ExhibitRoom) {
    newRoom match {
      case sourceStructure.entrez =>
        dataViewMode = WebSource
      case other =>
        dataViewMode = LocalSource
        tableModel.dataService = other match {
          case listBox: ExhibitListBox => dataSchema.dataServiceFor(listBox)
          case _ => dataSchema.exhibitsService
        }
    }
  }
  
  /** 表示モードを取得 */
  def dataViewMode = currentViewMode
  
  /** 表示モードを設定 */
  def dataViewMode_=(newMode: ViewMode) {
    if (currentViewMode != newMode) {
      currentViewMode = newMode
      updateDataView()
    }
  }
  
  /** 表示モードを変更 */
  private def updateDataView() {
    import com.jgoodies.binding.adapter.Bindings._
    
    val dataTable = mainView.dataTable
    val quickSearchField = mainView.quickSearchField
    // 全てのバインディングリスナの削除
    removeComponentPropertyHandler(quickSearchField)
    
    currentViewMode match {
      case WebSource =>
        webServiceResultModel installTo dataTable
        bind(quickSearchField, webSourceSearchText)
      case LocalSource =>
        tableModel installTo dataTable
        bind(quickSearchField, tableFilteringText)
    }
  }
  
  /** データスキーマからモデルの再設定 */
  private def reloadSchema() {
    sourceListModel.selectPathLocalLibrary
    sourceListModel.userBoxesSource = dataSchema.exhibitRoomService
  }
  
  /** パスが削除可能であるか */
  private def pathDeletable(path: Path[_]): Boolean = path.last match {
    case b: ExhibitListBox => true
    case _ => false
  }
  
  /** ファイルパスの取得 */
  private def getFileFor(exhibit: MuseumExhibit) = parent.filePathFor(exhibit)
  
  /** リソースを設定する */
  private def reloadResources() {
  }
  
  private def reloadResources(res: ResourceBundle) {
    val rm = new ResourceManager(res)
  }
  
  // ビューをリセット
  reloadResources()
  // モデルをリセット
  reloadSchema()
}

object MainViewController {
  object ViewMode extends Enumeration {
    type ViewMode = Value
    val LocalSource = Value
    val WebSource = Value
  }
}

class ResourceManager(res: ResourceBundle) {
  import collection.JavaConverters._
  import java.lang.Boolean.parseBoolean
  import javax.swing.KeyStroke.getKeyStroke
  import scala.swing.Action
  
  def injectTo(action: Action, keyPrefix: String) {
    val resKeys = res.getKeys.asScala.filter(_.startsWith(keyPrefix))
    if (resKeys.isEmpty)
      throw new IllegalArgumentException(
        "No resource which starts with '%s' found.".format(keyPrefix))
    
    resKeys.foreach { resKey =>
      resKey.substring(keyPrefix.length) match {
        case ".title" => action.title = res.getString(resKey)
        case ".enabled" => action.enabled = parseBoolean(res.getString(resKey))
        case ".accelerator" =>
          action.accelerator = Some(getKeyStroke(res.getString(resKey)))
        case ".toolTip" => action.toolTip = res.getString(resKey)
        case _ => // TODO log warnings
          println("unsupported key: " + resKey)
      }
    }
  }
}

import javax.swing.{JComponent, TransferHandler}
import java.awt.datatransfer._
import scala.swing.Swing

/**
 * ファイル転送ハンドラ
 */
class BioFileTransferHandler(controller: GenomeMuseumGUI) extends TransferHandler {
  import java.awt.datatransfer.DataFlavor
  
  override def canImport(comp: JComponent, flavors: Array[DataFlavor]): Boolean = {
    flavors contains DataFlavor.javaFileListFlavor
  }
  
  /**
   * データの読み込み
   */
  override def importData(comp: JComponent, t: Transferable): Boolean = {
    import java.{util => ju}
    import collection.JavaConverters._
    try {
      val paths = t.getTransferData(DataFlavor.javaFileListFlavor).asInstanceOf[ju.List[File]]
      val files = searchFiles(paths.asScala)
      Swing.onEDT {
        controller.loadBioFile(files)
      }
      files.nonEmpty
    }
    catch {
      case e: UnsupportedFlavorException =>
        e.printStackTrace
        false
      case e: java.io.IOException =>
        e.printStackTrace
        false
    }
  }
  
  protected def searchFiles(files: Seq[File]) = {
    import collection.mutable.{Buffer, ListBuffer, HashSet}
    // 探索済みディレクトリ
    val checkedDirs = HashSet.empty[String]
    // ディレクトリがすでに探索済みであるか
    def alreadyChecked(dir: File) = checkedDirs contains dir.getCanonicalPath
    // 探索済みに追加
    def addCheckedDir(dir: File) = checkedDirs += dir.getCanonicalPath
    
    @annotation.tailrec
    def collectFiles(files: List[File], accume: Buffer[File] = ListBuffer.empty[File]): List[File] = {
      files match {
        case head :: tail =>
          if (head.isHidden) {
            collectFiles(tail, accume)
          }
          else if (head.isFile) {
            accume += head
            collectFiles(tail, accume)
          }
          else if (head.isDirectory && !alreadyChecked(head)) {
            addCheckedDir(head)
            collectFiles(head.listFiles.toList ::: tail, accume)
          }
          else {
            collectFiles(tail, accume)
          }
        case Nil => accume.toList
      }
    }
    
    collectFiles(files.toList)
  }
}
