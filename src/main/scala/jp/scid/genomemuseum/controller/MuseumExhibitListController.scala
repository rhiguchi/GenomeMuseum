package jp.scid.genomemuseum.controller

import java.io.{Reader, File, InputStreamReader}
import java.net.{URI, URL}

import javax.swing.{JTable, JTextField, JComponent, TransferHandler, JTabbedPane,
  SwingWorker}

import ca.odell.glazedlists.TextFilterator

import org.jdesktop.application.Action

import jp.scid.gui.model.{ValueModels, ValueModel}
import jp.scid.gui.control.{ViewValueConnector, UriDocumentLoader, EventListController,
  TextMatcherEditorRefilterator}
import jp.scid.gui.event.{ValueChange, DataListSelectionChanged}
import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.motifviewer.gui.MotifViewerController
import jp.scid.genomemuseum.{view, model, gui}
import model.{UserExhibitRoom, MuseumExhibit, FreeExhibitRoomModel, ExhibitRoomModel,
  MutableMuseumExhibitListModel, MuseumExhibitListModel}
import view.ExhibitListView
import gui.ExhibitTableFormat
import MuseumExhibit.FileType


object MuseumExhibitListController {
  import jp.scid.bio.{FastaParser, GenBankParser}
  /** データビューの表示モード */
  private[controller] object DataViewMode extends Enumeration {
    type DataViewMode = Value
    
    val Contents = Value(0)
    val MotifView = Value(1)
  }
  
  private[controller] class SequenceLoader(sequence: ValueModel[String], url: URL, format: BioFileFormat)
      extends SwingWorker[String, Unit] {
    def doInBackground(): String = {
      using(new InputStreamReader(url.openStream())) { reader =>
        format.getSequence(reader)
      }
    }
    
    override def done() = isCancelled match {
      case true =>
      case false => 
        val sequenceString = try {
          get()
        }
        catch {
          case e: java.util.concurrent.ExecutionException =>
            e.printStackTrace
            ""
        }
        sequence.setValue(sequenceString)
    }
  }
  
  private[controller] object BioFileFormat {
    def unapply(exhibitFileType: FileType.Value): Option[BioFileFormat] = exhibitFileType match {
      case FileType.GenBank => Some(GenBank)
      case FileType.FASTA => Some(FASTA)
      case _ => None
    }
  }
  
  private[controller] sealed abstract class BioFileFormat {
    def getSequence(source: Reader): String
  }
  
  private[controller] object GenBank extends BioFileFormat {
    def getSequence(source: Reader) = {
      val parser = new GenBankParser
      val data = parser parseFrom source
      data.origin.sequence
    }
  }
  
  private[controller] object FASTA extends BioFileFormat{
    def getSequence(source: Reader) = {
      val parser = new FastaParser
      val data = parser parseFrom source
      data.sequence.value
    }
  }
  
  private def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
    try f(s) finally s.close()
  }
}

/**
 * 展示物のテーブル表示と、フィルタリング、テーブルに表示されている項目の
 * 情報を表示する機能を提供する操作クラス。
 */
class MuseumExhibitListController extends EventListController[MuseumExhibit, ExhibitRoomModel] {
  import MuseumExhibitListController._
  
  private val ctrl = GenomeMuseumController(this);
  
  // プロパティ
  /** データビューが表示されているか */
  private val dataViewVisibled = ValueModels.newBooleanModel(false)
  
  /** データビューの表示モード */
  private val dataViewMode = ValueModels.newValueModel(DataViewMode.Contents)
  
  /** データビューに表示するファイルの URI */
  private val selectedUri = ValueModels.newNullableValueModel[URI]
  
  /** MotifViewer 用塩基配列 */
  private val motifViewerSequence = ValueModels.newValueModel("")
  
  /** 検索モデル */
  private val filterator = new TableFilterator
  
  /** タイトル */
  val title = ValueModels.newValueModel("");
  
  /** 検索文字列 */
  val searchText = ValueModels.newValueModel("");
  
  /** テーブル形式を返す */
  private[controller] val tableFormat = new ExhibitTableFormat
  
  // コントローラ
  /** MotifViewer */
  private[controller] val motifViewerController = new MotifViewerController
  
  /** 中身を読み込む操作 */
  private[controller] val documentLoader = new UriDocumentLoader
  documentLoader.setModel(selectedUri)
  
  /** 検索ハンドラ */
  private[controller] val tableRefilterator = new TextMatcherEditorRefilterator(filterator)
  setMatcherEditor(tableRefilterator.getTextMatcherEditor)
  tableRefilterator.setModel(searchText)
  
  /** 項目削除アクション */
  def tableDeleteAction = Some(removeSelectionAction.peer)
  /** 転送ハンドラ */
  private[controller] val tableTransferHandler = new MuseumExhibitListTransferHandler(this)
  /** ローカルソースの選択項目を除去するアクション */
  val removeSelectionAction = ctrl.getAction("removeSelections")
  
  /** 読み込みマネージャ */
  var loadManager: Option[MuseumExhibitLoadManager] = None
  
  /** 選択項目変化に処理を行うハンドラ */
  private[controller] val selectionChangeHandler = EventListHandler(getSelectionModel.getSelected) { exhibits =>
    setDataViewExhibits(exhibits.toList)
  }
  
  /** 使用するテーブル形式を指定 */
  override def createTableFormat = tableFormat
  
  /**
   * 選択項目を削除する
   */
  @Action(name="removeSelections")
  def removeSelections() {
    import collection.JavaConverters._
    
    getModel match {
      case model: FreeExhibitRoomModel => 
//        getSelectionModel.getSelected.asScala.toList foreach model.remove
      case _ =>
    }
  }
  
  /** ビューに結合処理を追加するため */
   def bind(view: ExhibitListView) {
    bindTable(view.dataTable)
    motifViewerController.bind(view.overviewMotifView)
    documentLoader.bindTextComponent(view.getContentViewComponent)
  }
  
  /** 転送ハンドラを作成 */
  override def getTransferHandler() = tableTransferHandler
  
  /** ファイルを読み込む */
  def importFile(files: List[File]) = getModel match {
    case model: FreeExhibitRoomModel =>
      files foreach (file => loadManager.get.loadExhibit(model, file))
      true
    case  _ => false
  }
  
  /** データビューに表示する展示物を設定する */
  def setDataViewExhibits(exhibits: List[MuseumExhibit]) {
    import MuseumExhibit.FileType
    
    val exhibit = exhibits.headOption
    val uri = exhibit.map(_.dataSourceUri).filter(_.nonEmpty).map(s => new URI(s))
    selectedUri.setValue(uri.getOrElse(null))
    
    val titleString = exhibit match {
      case Some(e) => e.name match {
        case "" => e.accession match {
          case "" => e.identifier match {
            case "" => e.dataSourceUri
            case identifier => identifier
          }
          case accession => accession
        }
        case name => name
      }
      case None => ""
    }
    
    title := titleString
    
    // motifViewerSequence
    motifViewerSequence.setValue("")
    
    val format = exhibit.map(_.fileType) flatMap BioFileFormat.unapply
    val source = uri map (_.toURL) flatMap (url => format.map(f => (url, f)))
    val task = source map { case (url, format) => 
      new SequenceLoader(motifViewerSequence, url, format)
    }
    task.foreach(_.execute())
  }
  
  def addElements(sourceRows: List[MuseumExhibit]) = getModel match {
    case model: FreeExhibitRoomModel => sourceRows map model.add contains true
    case _ => false
  }
  
  class TableFilterator extends TextFilterator[MuseumExhibit] {
    def getFilterStrings(baseList: java.util.List[String], exhibit: MuseumExhibit) {
      (0 until tableFormat.getColumnCount) foreach { index =>
        baseList.add(tableFormat.getColumnValue(exhibit, index).toString)
      }
    }
  }
}
