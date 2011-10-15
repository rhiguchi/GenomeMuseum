package jp.scid.genomemuseum

import java.awt.{BorderLayout, FileDialog}
import java.io.{File, FileInputStream, IOException}
import java.text.ParseException
import org.jdesktop.application.{Application, Action, ProxyActions}
import view.{MainView, MainViewMenuBar, ColumnVisibilitySetting}
import controller.{ExhibitTableController, MainViewController, ViewSettingDialogController}
import model.{MuseumScheme, MuseumExhibitParser, LibraryFileManager}
import scala.swing.{Frame, Dialog, Panel}

@ProxyActions(Array("selectAll"))
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
  lazy val cutAction = actionFor("cut")
  lazy val copyAction = actionFor("copy")
  lazy val pasteAction = actionFor("paste")
  lazy val deleteAction = actionFor("delete")
  lazy val selectAllAction = actionFor("selectAll")
  
  // Model
  private var scheme = MuseumScheme.empty
  private var libFiles: Option[LibraryFileManager] = None
  
  private val exhibitParser = new MuseumExhibitParser
  
  override protected def initialize(args: Array[String]) {
    val genomemuseumHome = getContext.getLocalStorage.getDirectory
    
    println("LibraryDir: " + genomemuseumHome)
    scheme = MuseumScheme.onMemory
    libFiles = Some(new LibraryFileManager(new File(genomemuseumHome, "BioFiles")))
  }
  
  override def startup() {
    import jp.scid.genomemuseum.model.ExhibitListBox
    val frame = mainFrame.peer
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)
    
    mainCtrl = new MainViewController(this, mainFrame.mainView)
    bindMenuBarActions(mainFrame.mainMenu, mainCtrl)
    
    GenomeMuseumGUI.loadSampleFiles.foreach(scheme.exhibitsService.add)
    mainCtrl.dataSchema = scheme
    // サンプルデータ追加    
    mainCtrl.sourceListModel.addListBox("test1")
    mainCtrl.sourceListModel.addListBox("test2")
    mainCtrl.sourceListModel.addListBox("test3")
    
    
    val viewSettingDialogCtrl = new ViewSettingDialogController(
      mainFrame.columnConfigPane, mainFrame.columnConfigDialog)
    mainFrame.mainMenu.columnVisibility.action = viewSettingDialogCtrl.showAction
  }
  
  override protected def ready() {
    mainFrame.peer.pack
    mainFrame.peer setLocationRelativeTo null
    mainFrame.peer setVisible true
  }
  
  private def bindMenuBarActions(menu: MainViewMenuBar, controller: MainViewController) {
    menu.open.action = openAction
    menu.quit.action = quitAction
    
    menu.cut.action = cutAction
    menu.copy.action = copyAction
    menu.paste.action = pasteAction
    menu.delete.action = deleteAction
    menu.selectAll.action = selectAllAction
    
    menu.newListBox.action = controller.addListBoxAction
    menu.newSmartBox.action = controller.addSmartBoxAction
    menu.newGroupBox.action = controller.addBoxFolderAction
    
    menu.reloadResources()
  }
  
  @Action
  def openFile() {
    println("openFile")
    openDialog setVisible true
    val fileName = Option(openDialog.getFile)
    
    fileName.map(new File(openDialog.getDirectory, _)).foreach(loadBioFile)
  }
  
  /** データをライブラリから削除 */
  def deleteFromLibrary(exhibits: Seq[MuseumExhibit]) {
    // TODO 削除確認ダイアログの表示
    // TODO ファイルも削除するか確認
    
    exhibits foreach { exhibit =>
      val uri = exhibit.filePathAsURI
      // データベースから削除
      scheme.exhibitsService.remove(exhibit)
      
      // URI が妥当であればファイルを削除
      libFiles foreach { lib =>
        if (lib.isLibraryURI(uri))
          lib.delete(uri) 
      }
    }
  }
  
  /** Exhibit のファイルを取得 */
  def filePathFor(exhibit: MuseumExhibit): Option[File] = {
    exhibit.filePathAsURI match {
      case uri if uri.toString == "" => None
      case uri if uri.getScheme == "file" => Some(new File(uri))
      case uri => libFiles.map(_.getFile(uri))
    }
  }
  
  def loadBioFile(files: Seq[File]) {
    files foreach loadBioFile
  }
  
  /** ローカル環境にファイルが保存できるか */
  private def isLocalStorable(): Boolean = libFiles.nonEmpty
  
  @throws(classOf[IOException])
  @throws(classOf[ParseException])
  protected def loadBioFile(file: File) {
    
    println("loadBioFile: " + file)
    
    val e = exhibitParser.parseFrom(file)
    
    if (isLocalStorable) e foreach { e =>
      libFiles.get.store(e, file)
    }
    
    e.map(mainCtrl.tableModel.addElement)
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
   * リソースマップの取得
   */
  def resourceMap(c: Class[_]) =
    Application.getInstance().getContext().getResourceMap(c)
  
  /**
   * アクションの取得
   */
  def actionFor(controller: AnyRef, key: String): Action =
    actionFor(Application.getInstance().getContext().getActionMap(controller))(key)
    
    
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
