package jp.scid.genomemuseum

import java.awt.{BorderLayout, FileDialog}
import java.io.{File, FileInputStream}
import org.jdesktop.application.{Application, Action}
import view.{MainView, MainViewMenuBar}
import controller.{ExhibitTableController, MainViewController}
import model.{MuseumScheme, MuseumDataSource}
import scala.swing.Frame

class GenomeMuseumGUI extends Application {
  import jp.scid.genomemuseum.model.MuseumExhibit
  
  // リソースのネームスペースを無しに設定
  getContext.getResourceManager.setResourceFolder("")
  
  // Views
  lazy val mainFrame = new Frame {
    val mainView = new MainView
    val mainMenu = new MainViewMenuBar
    
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
  private var dataSource = new MuseumDataSource(MuseumScheme.empty)
  
  override protected def initialize(args: Array[String]) {
    
    val scheme = MuseumScheme.onMemory
    dataSource = new MuseumDataSource(scheme)
  }
  
  override def startup() {
    val frame = mainFrame.peer
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)
    
    mainCtrl = new MainViewController(this,
      mainFrame.mainView, mainFrame.peer)
    bindMenuBarActions(mainFrame.mainMenu, mainCtrl)
    
    val tableSource = dataSource.allExibits
    mainCtrl.tableCtrl bindTableSource tableSource
    tableSource ++= sampleExhibits
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
    
    // 拡張子で判別
    // TODO ファイルの中身を読んで判別
    val e = if (file.getName.endsWith(".gbk")) {
      val parser = new jp.scid.bio.GenBankParser
      val data = using(new FileInputStream(file)) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      Some(MuseumExhibit(data.locus.name, data.locus.sequenceLength))
    }
    else if (file.getName.endsWith(".faa") ||
        file.getName.endsWith(".fna") || file.getName.endsWith(".ffn") ||
        file.getName.endsWith(".fasta")) {
      val parser = new jp.scid.bio.FastaParser
      val data = using(new FileInputStream(file)) { inst =>
        val source = io.Source.fromInputStream(inst)
        parser.parseFrom(source.getLines)
      }
      Some(MuseumExhibit(data.header.accession, data.sequence.value.length))
    }
    else {
      None
    }
    
    e.map(insertElement)
  }
  
  private def insertElement(e: MuseumExhibit) {
    mainCtrl.tableCtrl.tableSource add e
  }
  
  /**
   * サンプルデータを 10 件設定する
   * @param controller 流し込み先コントローラ
   */
  private def sampleExhibits() = {
    import model.MuseumExhibit
    
    val samples = List(
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
    samples
  }
  
  /**
   * サンプルファイルを 10 件設定する
   * @param controller 流し込み先コントローラ
   */
  private def loadSampleFilesTo(controller: ExhibitTableController) {
    import model.MuseumExhibit
    import ca.odell.glazedlists.GlazedLists
    import scala.collection.JavaConverters._
    
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
    .map( e => MuseumExhibit(e.locus.name, e.locus.sequenceLength) )
    
    val target = controller.tableSource
    target.getReadWriteLock().writeLock().lock();
    try {
      GlazedLists.replaceAll(target, list.asJava, true)
    }
    finally {
      target.getReadWriteLock().writeLock().unlock();
    }
  }
}

object GenomeMuseumGUI {
  import org.jdesktop.application.ApplicationActionMap
  import java.awt.event.ActionEvent
  import scala.swing.Action
  
  def actionFor(actionMap: ApplicationActionMap)(key: String) = new Action(key) {
    override lazy val peer = actionMap.get(key) match {
      case null => throw new IllegalStateException(
        "Action '%s' is not defined on '%s'.".format(key, actionMap.getActionsClass))
      case action => action
    }
    override def apply() {
      val e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "apply")
      apply(e)
    }
    def apply(event: ActionEvent) = peer.actionPerformed(event)
  }
}
