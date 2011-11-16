package jp.scid.genomemuseum

import java.awt.{BorderLayout, FileDialog}
import java.io.{File, FileInputStream, IOException}
import java.text.ParseException
import org.jdesktop.application.{Application, Action, ProxyActions}
import view.{MainView, MainViewMenuBar, ColumnVisibilitySetting}
import controller.{MainViewController, ViewSettingDialogController}
import model.{MuseumSchema, LibraryFileManager}
import scala.swing.{Frame, Dialog, Panel}

@ProxyActions(Array("selectAll"))
class GenomeMuseumGUI extends Application {
  import jp.scid.genomemuseum.model.MuseumExhibit
  import GenomeMuseumGUI._
  
  // リソースのネームスペースを無しに設定
  getContext.getResourceManager.setResourceFolder("")
  
  lazy val openDialog = new FileDialog(null.asInstanceOf[java.awt.Frame], "", FileDialog.LOAD)
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
  private var genomemuseumHome = new File(".", "GenomeMuseum")
  private var libFiles: Option[LibraryFileManager] = None
  
  override protected def initialize(args: Array[String]) {
    genomemuseumHome = getContext.getLocalStorage.getDirectory
  }
  
  override def startup() {
//    mainCtrl.dataSchema = scheme
    // サンプルデータ追加    
//    mainCtrl.sourceListModel.addListBox("test1")
//    mainCtrl.sourceListModel.addListBox("test2")
//    mainCtrl.sourceListModel.addListBox("test3")
    
//    loadSampleDataTo(this)
    
    
    
    // ビュー構築
    val mainViewFrame = new javax.swing.JFrame
    mainViewFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)
    
    // コントローラ構築
    val mainController = new controller.MuseumFrameViewController(mainViewFrame)
    bindApplicationActionsTo(mainController.mainMenu)
    
    // スキーマ構築
    println("LibraryDir: " + genomemuseumHome)
    val dataSchema = MuseumSchema.fromMemory
    val libFiles = Some(new LibraryFileManager(new File(genomemuseumHome, "BioFiles")))
    
    // スキーマ適用
    mainController.dataSchema = dataSchema
    
    // 表示
    mainController.show()
  }
  
  override protected def ready() {
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
    
    menu.reloadResources()
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


