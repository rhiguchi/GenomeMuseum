package jp.scid.genomemuseum

import java.awt.{BorderLayout, FileDialog}
import java.io.{File, FileInputStream, IOException}
import java.text.ParseException
import org.jdesktop.application.{Application, Action, ProxyActions}
import view.{MainView, MainViewMenuBar, ColumnVisibilitySetting}
import controller.{GenomeMuseumController, MainViewController, ViewSettingDialogController,
  MuseumExhibitLoadManager}
import model.{MuseumSchema, LibraryFileManager, MuseumExhibitLoader}
import scala.swing.{Frame, Dialog, Panel}

@ProxyActions(Array("selectAll"))
class GenomeMuseumGUI extends Application {
  import jp.scid.genomemuseum.model.MuseumExhibit
  import GenomeMuseumGUI._
  
  // リソースのネームスペースを無しに設定
  getContext.getResourceManager.setResourceFolder("")
  
  lazy val openDialog = new FileDialog(null.asInstanceOf[java.awt.Frame], "", FileDialog.LOAD)
  // Actions
  lazy val openAction = getAction("openFile")
  lazy val quitAction = getAction("quit")
  lazy val cutAction = getAction("cut")
  lazy val copyAction = getAction("copy")
  lazy val pasteAction = getAction("paste")
  lazy val deleteAction = getAction("delete")
  lazy val selectAllAction = getAction("selectAll")
  
  // Model
  /** データ保管のディレクトリ */
  private var genomemuseumHome = {
    def getTempDir(trying: Int): File = {
      if (trying <= 0) throw new IllegalStateException("could not create temp dir.")
      
      val file = File.createTempFile("GenomeMuseum", "")
      if (file.delete && file.mkdir)
        file
      else
        getTempDir(trying - 1)
    }
    
    getTempDir(20)
  }
  /** データベースの構築をメモリー内で行うか */
  private var useInMemoryDatabase = false
  
  override protected def initialize(args: Array[String]) {
    if (args.contains("-production")) {
      genomemuseumHome = getContext.getLocalStorage.getDirectory
    }
  }
  
  override def startup() {
//    mainCtrl.dataSchema = scheme
    // サンプルデータ追加    
//    mainCtrl.sourceListModel.addListBox("test1")
//    mainCtrl.sourceListModel.addListBox("test2")
//    mainCtrl.sourceListModel.addListBox("test3")
    
//    loadSampleDataTo(this)
    
    logger.debug("startup")
    
    // ビュー構築
    val mainViewFrame = new javax.swing.JFrame
    mainViewFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)
    
    // コントローラ構築
    val mainController = new controller.MuseumFrameViewController(mainViewFrame)
    bindApplicationActionsTo(mainController.mainMenu)
    
    // スキーマ構築
    logger.info("LibraryDir: {}", genomemuseumHome)
    logger.debug("useInMemoryDatabase: {}", useInMemoryDatabase)
    
    val dataSchema = useInMemoryDatabase match {
      case true => MuseumSchema.onMemory
      case false =>
        val databaseFile = new File(genomemuseumHome, "Library/lib")
        MuseumSchema.onFile(databaseFile)
    }
    val fileStorage = useInMemoryDatabase match {
      case true => None
      case false => Some(new LibraryFileManager(new File(genomemuseumHome, "BioFiles")))
    }
    
    // 読み込み処理操作
    val loadManager = new MuseumExhibitLoadManager(fileStorage)
    
    // スキーマ適用
    mainController.dataSchema = dataSchema
    mainController.mainCtrl.loadManager = loadManager
    
    // 表示
    mainController.show()
  }
  
  override protected def ready() {
    logger.info("ready")
  }
  
  /** アプリケーションの持つアクションをメニューバーに設定 */
  private def bindApplicationActionsTo(menu: MainViewMenuBar) {
    menu.open.action = openAction
    menu.quit.action = quitAction
    
    menu.cut.action = cutAction
    menu.copy.action = copyAction
    menu.paste.action = pasteAction
    menu.delete.action = deleteAction
    menu.selectAll.action = selectAllAction
    
    applicationContext.getResourceManager.getResourceMap(menu.getClass, classOf[Object])
      .injectComponents(menu.container.peer)
  }
  
  @Action
  def openFile() {
    println("openFile")
    openDialog setVisible true
    val fileName = Option(openDialog.getFile)
    
//    fileName.map(new File(openDialog.getDirectory, _)).foreach(loadBioFile)
  }
  
  /** データをライブラリから削除 */
  def deleteFromLibrary(exhibits: Seq[MuseumExhibit]) {
    // TODO 削除確認ダイアログの表示
    // TODO ファイルも削除するか確認
    
    exhibits foreach { exhibit =>
      val uri = exhibit.filePathAsURI
      // データベースから削除
//      scheme.exhibitsService.remove(exhibit)
      
      // URI が妥当であればファイルを削除
//      libFiles foreach { lib =>
//        if (lib.isLibraryURI(uri))
//          lib.delete(uri) 
//      }
    }
  }
  
  /** Exhibit のファイルを取得 */
  def filePathFor(exhibit: MuseumExhibit): Option[File] = {
    exhibit.filePathAsURI match {
      case uri if uri.toString == "" => None
      case uri if uri.getScheme == "file" => Some(new File(uri))
//      case uri => libFiles.map(_.getFile(uri))
    }
  }
}

object GenomeMuseumGUI {
  import org.jdesktop.application.ApplicationActionMap
  import java.awt.event.ActionEvent
  import scala.swing.Action
  import model.MuseumExhibit
  import jp.scid.bio.GenBank
  
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[GenomeMuseumGUI])
  
  /**
   * アプリケーションのコンテキストオブジェクトを取得
   */
  lazy val applicationContext = {
    import util.control.Exception.catching
    
    def getInstance = Application.getInstance(classOf[GenomeMuseumGUI])
    
    val application = catching(classOf[IllegalStateException]).opt
        {getInstance}.getOrElse {
      java.beans.Beans.setDesignTime(true)
      getInstance
    }
    application.getContext()
  }
  
  /**
   * リソースマップを取得
   * 指定したコントローラのオブジェクトから、GenomeMuseumController までの
   * 階層内のマップを返す。
   */
  def resourceMapOf(obj: AnyRef) = applicationContext
    .getResourceManager.getResourceMap(obj.getClass, classOf[Object])
  
  /**
   * アプリケーションのアクションを取得する
   */
  def getAction(key: String) = {
    val action = applicationContext.getActionManager.getActionMap(classOf[GenomeMuseumGUI],
        Application.getInstance).get(key)
    
    if (action == null)
      logger.warn("Action '%s' is not defined on GenomeMuseumGUI.".format(key))
    
    convertToScalaSwingAction(action)
  }
  
  /**
   * Swing のアクションを scala.swing のアクションに変換する
   */
  implicit def convertToScalaSwingAction(swingAction: javax.swing.Action)
      = new scala.swing.Action("") {
    import java.awt.event.ActionEvent
    
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
  
  private def loadSampleDataTo(loader: GenomeMuseumGUI) {
    val samples = List("NC_004554.gbk", "NC_004555.gbk", "NC_006375.gbk",
      "NC_006376.gbk", "NC_006676.gbk", "NC_007504.gbk", "NC_009347.gbk",
      "NC_009517.gbk", "NC_009934.gbk", "NC_010550.gbk")
    val resourceBase = "sample_bio_files/"
    val cls = this.getClass
    
    val resources = samples.map(resourceBase.+).map(cls.getResource)
    
//    resources foreach loader.loadBioFile
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


