package jp.scid.genomemuseum

import java.awt.{BorderLayout, FileDialog}
import java.io.{File, FileInputStream, IOException}
import java.text.ParseException
import org.jdesktop.application.{Application, Action, ProxyActions}
import view.{ApplicationViews, MainFrameView, MainView, MainViewMenuBar, ColumnVisibilitySetting}
import controller.{GenomeMuseumController, MainViewController, MuseumExhibitLoadManager, ApplicationActionHandler}
import model.{MuseumSchema, LibraryFileManager, MuseumExhibitStorage, MuseumExhibitLoader,
  MuseumExhibitService}
import scala.swing.{Frame, Dialog, Panel}

/**
 * GenomeMuseum GUI アプリケーション実行クラス。
 * 
 * @see #startup()
 * @see jp.scid.genomemuseum.controller.MainFrameViewController
 */
@ProxyActions(Array("selectAll"))
class GenomeMuseumGUI extends Application {
  import jp.scid.genomemuseum.model.MuseumExhibit
  import GenomeMuseumGUI._
  import RunMode._
  
  def this(runMode: GenomeMuseumGUI.RunMode.Value) {
    this()
    GenomeMuseumGUI.runMode = runMode
  }
  
  // リソースのネームスペースを無しに設定
  getContext.getResourceManager.setResourceFolder("")
  
  // プロパティ
  /** スキーマをファイルに保存する時の保存先 */
  var schemaFileSource: Option[File] = None
  /** データファイルが保存される基本ディレクトリ */
  var fileStorageDir: Option[File] = None
  
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
  
  /**
   * アプリケーションのデータを保存するディレクトリを設定する。
   * 
   * 指定したディレクトリに則って、{@link #schemaFileSource} と {@link #fileStorageDir} が
   * 新しい値に設定される。
   */
  override protected[genomemuseum] def initialize(args: Array[String]) {
    args.contains("--LocalLibrary") match {
      case true =>
        schemaFileSource = Some(new File(applicationHome, "Library/lib"))
      case false =>
        applicationContext.getLocalStorage.setDirectory(createTempDir())
    }
    
    fileStorageDir = Some(new File(applicationHome, "BioFiles"))
  }
  
  /**
   * 設定されたプロパティを利用して、メインビューの表示を行う。
   * 
   * Swing Application Framework によって起動時に一度呼び出される。
   * 
   * @see ApplicationActionHandler#showMainFrameView()
   */
  override def startup() {
    logger.debug("startup")
    
    val handler = createApplicationHandler()
    handler.showMainFrameView()
  }
  
  override protected def ready() {
    logger.info("ready")
  }
  
  /** @return アプリケーションのデータが保存されるディレクトリ */
  def applicationHome = applicationContext.getLocalStorage.getDirectory
  
  /**
   * アプリケーションのデータスキーマを作成する。
   * 
   * 通常、 {@link startup} から一度呼び出され、このアプリケーションのデータモデルとなる。
   * 
   * 作成されるスキーマは {@link schemaFileSource} の値によって変化し、
   * 何らかのファイルパスを返すとき、ファイルに保存されるスキーマを作成する。
   * この時の保存先はH2 Database の命名規則に従う（パスの最後に ".h2.db" ついたファイルが作成される）。
   * {@code None} が返される時は、メモリー上のプライベート空間にスキーマが作成される。
   * @see #schemaFileSource
   */
  def createMuseumSchema(): MuseumSchema = {
    logger.info("Library on {}", schemaFileSource)
     
    schemaFileSource match {
      case Some(file) => MuseumSchema.onFile(file)
      case None => MuseumSchema.onMemory
    }
  }
  
  /**
   * アプリケーションのバイオファイル管理オブジェクトを作成する。
   * 
   * 通常、 {@link startup} から一度呼び出され、この読み込み処理管理オブジェクトとなる。
   * @see #fileStorageDir
   */
  protected[genomemuseum] def createFileStorage(): LibraryFileManager = {
    logger.info("Bio files stored on {}", fileStorageDir)
    
    fileStorageDir match {
      case Some(dir) => new LibraryFileManager(dir)
      case None => new LibraryFileManager(createTempDir())
    }
  }
  
  /**
   * アプリケーションの画面を作成する。
   * 
   * 通常、 {@link startup} から一度呼び出され、主画面として利用される。
   */
  def createApplicationViews() = {
    new ApplicationViews()
  }
  
  /**
   * アプリケーションにおける操作対応オブジェクトを作成する。
   * 
   * 通常、 {@link startup} から一度呼び出され、主画面の入出力処理オブジェクトとして利用される。
   */
  protected[genomemuseum] def createApplicationHandler() = {
    new ApplicationActionHandler(this)
  }
  
  /**
   * アプリケーションのアクションを取得する
   */
  def getAction(key: String): swing.Action = {
    val action = applicationContext.getActionManager.getActionMap().get(key)
    if (action == null)
      logger.warn("Action '%s' is not defined on GenomeMuseumGUI.".format(key))
    
    convertToScalaSwingAction(action)
  }
}

object GenomeMuseumGUI {
  import org.jdesktop.application.ApplicationActionMap
  import java.awt.event.ActionEvent
  import scala.swing.Action
  import model.MuseumExhibit
  import jp.scid.bio.GenBank
  
  /** アプリケーションの実行モードを表す列挙型 */
  protected[genomemuseum] object RunMode extends Enumeration {
    type RunMode = Value
    val Testing, Development, Production = Value
  }
  
  /** 現在のアプリケーションの実行モード */
  protected[genomemuseum] var runMode: RunMode.Value = RunMode.Development
  
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[GenomeMuseumGUI])
  
  /**
   * ディレクトリを一時ディレクトリ中に作成する。
   * @throws IllegalStateException 作成することができなかった時。
   */
  private[genomemuseum] def createTempDir() = {
    def getTempDir(trying: Int): File = {
      if (trying <= 0) throw new IllegalStateException("could not create temp dir.")
      
      val file = File.createTempFile("GenomeMuseum", "")
      lazy val dir = new File(file.getPath + ".d")
      if (file.delete && dir.mkdir)
        dir
      else
        getTempDir(trying - 1)
    }
    
    getTempDir(20)
  }
  
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


