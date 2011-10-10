package jp.scid.genomemuseum

import java.awt.{BorderLayout, FileDialog}
import java.io.{File, FileInputStream}
import org.jdesktop.application.{Application, Action}
import view.{MainView, MainViewMenuBar, ColumnVisibilitySetting}
import controller.{ExhibitTableController, MainViewController, ViewSettingDialogController}
import model.{MuseumScheme, MuseumDataSource}
import scala.swing.{Frame, Dialog, Panel}

class GenomeMuseumGUI extends Application {
  import jp.scid.genomemuseum.model.MuseumExhibit
  import GenomeMuseumGUI._
  
  // リソースのネームスペースを無しに設定
  getContext.getResourceManager.setResourceFolder("")
  
  // Views
  lazy val mainFrame = new Frame {
    val mainView = new MainView
    val mainMenu = new MainViewMenuBar
    
    // 列表示設定
    val columnConfigPane = new ColumnVisibilitySetting
    lazy val columnConfigDialog = new Dialog(this) {
      contents = new Panel{
        override lazy val peer = columnConfigPane.contentPane
      }
    }
    
    title = "GenomeMuseum"
    
    menuBar = mainMenu.container
    
    peer.getContentPane.setLayout(new BorderLayout)
    peer.getContentPane.add(mainView.contentPane, "Center")
  }
  lazy val openDialog = new FileDialog(mainFrame.peer, "", FileDialog.LOAD)
  
  
  // Controllers
  var mainCtrl: MainViewController = _
  
  // Actions
  private lazy val actionFor = GenomeMuseumGUI.actionFor(getContext.getActionMap(this))_
  lazy val openAction = actionFor("openFile")
  lazy val quitAction = actionFor("quit")
  
  // Model
  private var scheme = MuseumScheme.empty
  private var dataSource = new MuseumDataSource(scheme)
  
  override protected def initialize(args: Array[String]) {
    scheme = MuseumScheme.onMemory
    dataSource = new MuseumDataSource(scheme)
  }
  
  override def startup() {
    import jp.scid.genomemuseum.model.ExhibitListBox
    val frame = mainFrame.peer
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)
    
    mainCtrl = new MainViewController(this,
      mainFrame.mainView, mainFrame.peer)
    bindMenuBarActions(mainFrame.mainMenu, mainCtrl)
    
    mainCtrl.sourceModel.userBoxesSource = scheme.exhibitRoomService    
    mainCtrl.sourceModel.addListBox("test1")
    mainCtrl.sourceModel.addListBox("test2")
    mainCtrl.sourceModel.addListBox("test3")
    
    val tableSource = dataSource.allExibits
    mainCtrl.tableCtrl bindTableSource tableSource
    dataSource store GenomeMuseumGUI.loadSampleFiles
    
    val viewSettingDialogCtrl = new ViewSettingDialogController(
      mainFrame.columnConfigPane, mainFrame.columnConfigDialog)
    mainFrame.mainMenu.columnVisibility.action = viewSettingDialogCtrl.showAction
  }
  
  override protected def ready() {
    mainCtrl.showFrame
  }
  
  private def bindMenuBarActions(menu: MainViewMenuBar, controller: MainViewController) {
    menu.open.action = openAction
    menu.quit.action = quitAction
  }
  
  @Action
  def openFile() {
    println("openFile")
    openDialog setVisible true
    val fileName = Option(openDialog.getFile)
    
    fileName.map(new File(openDialog.getDirectory, _)).foreach(loadBioFile)
  }
  
  def loadBioFile(files: Seq[File]) {
    files foreach loadBioFile
  }
  
  protected def loadBioFile(file: File) {
    println("loadBioFile: " + file)
    
    def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
      try f(s) finally s.close()
    }
    
    def getVersionNumber(value: Int) =
      if (value == 0) None else Some(value)
    
    // 拡張子で判別
    // TODO ファイルの中身を読んで判別
    val e = if (file.getName.endsWith(".gbk")) {
      val parser = new jp.scid.bio.GenBankParser
      val data = using(new FileInputStream(file)) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      Some(MuseumExhibit(
        name = data.locus.name,
        sequenceLength = data.locus.sequenceLength,
        accession = data.accession.primary,
        identifier = data.version.identifier,
        namespace = data.locus.division,
        version = getVersionNumber(data.version.number),
        definition = data.definition.value,
        source = data.source.value,
        organism = data.source.taxonomy :+ data.source.organism mkString "\n",
        date = data.locus.date
      ))
    }
    else if (file.getName.endsWith(".faa") ||
        file.getName.endsWith(".fna") || file.getName.endsWith(".ffn") ||
        file.getName.endsWith(".fasta")) {
      val parser = new jp.scid.bio.FastaParser
      val data = using(new FileInputStream(file)) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      Some(MuseumExhibit(
        name = data.header.name,
        sequenceLength = data.sequence.value.length,
        accession = data.header.accession,
        identifier = data.header.identifier,
        namespace = data.header.namespace,
        version = getVersionNumber(data.header.version),
        definition = data.header.description
      ))
    }
    else {
      None
    }
    
    dataSource store e
  }
}

object GenomeMuseumGUI {
  import org.jdesktop.application.ApplicationActionMap
  import java.awt.event.ActionEvent
  import scala.swing.Action
  import model.MuseumExhibit
  import jp.scid.bio.GenBank
  
  def actionFor(actionMap: ApplicationActionMap)(key: String) = {
    val swingAction = actionMap.get(key) match {
      case null => throw new IllegalStateException(
        "Action '%s' is not defined on '%s'.".format(key, actionMap.getActionsClass))
      case action => action
    }
    convertToScalaSwingAction(swingAction)
  }
  
  implicit def convertToScalaSwingAction(swingAction: javax.swing.Action) = new Action("") {
    override lazy val peer = swingAction
    override def apply() {
      val e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "apply")
      apply(e)
    }
    def apply(event: ActionEvent) = peer.actionPerformed(event)
  }
  
  /**
   * サンプルデータを 10 件作成する
   */
  private def sampleExhibits() = {
    List(
      MuseumExhibit("item1", 500, "item1-source"),
      MuseumExhibit("item2", 1000, "item2-source"),
      MuseumExhibit("item3", 1500, "item3-source"),
      MuseumExhibit("item4", 5000, "item4-source"),
      MuseumExhibit("item5", 4000, "item5-source"),
      MuseumExhibit("item6", 3000, "item6-source"),
      MuseumExhibit("item7", 10000, "item7-source"),
      MuseumExhibit("item8", 1000000, "item8-source"),
      MuseumExhibit("item9", 200000, "item9-source"),
      MuseumExhibit("item10", 55500, "item10-source")
    )
  }
  
  /**
   * サンプルファイルを 10 件読み込む
   */
  private def loadSampleFiles() = {
    val samples = List("NC_004554.gbk", "NC_004555.gbk", "NC_006375.gbk",
      "NC_006376.gbk", "NC_006676.gbk", "NC_007504.gbk", "NC_009347.gbk",
      "NC_009517.gbk", "NC_009934.gbk", "NC_010550.gbk")
    val resourceBase = "sample_bio_files/"
    val cls = this.getClass
    
    val resources = samples.map(resourceBase.+).map(cls.getResource)
    
    def using[A <% java.io.Closeable, B](s: A)(f: A => B) = {
      try f(s) finally s.close()
    }
    
    val parser = new jp.scid.bio.GenBankParser
    
    val list = resources.map { r => using(r.openStream) { inst =>
      val source = io.Source.fromInputStream(inst)
      parser.parseFrom(source.getLines)
    }}
    .map( createMuseumExibit )
    
    list
  }
  
  private def getVersionNumber(value: Int) =
    if (value == 0) None else Some(value)
  
  private def createMuseumExibit(data: GenBank) = MuseumExhibit(
    name = data.locus.name,
    sequenceLength = data.locus.sequenceLength,
    accession = data.accession.primary,
    identifier = data.version.identifier,
    namespace = data.locus.division,
    version = getVersionNumber(data.version.number),
    definition = data.definition.value,
    source = data.source.value,
    organism = data.source.taxonomy :+ data.source.organism mkString "\n",
    date = data.locus.date
  )
}
