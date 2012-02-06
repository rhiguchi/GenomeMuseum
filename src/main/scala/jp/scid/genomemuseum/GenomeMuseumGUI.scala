package jp.scid.genomemuseum

import java.io.File
import org.jdesktop.application.{Application, Action, ProxyActions}
import view.{ApplicationViews, MainFrameView, MainView, MainViewMenuBar, ColumnVisibilitySetting}
import controller.{GenomeMuseumController, MainFrameViewController, MainViewController,
  MuseumExhibitLoadManager, ExhibitRoomListController, ApplicationController,
  MuseumExhibitListController, WebServiceResultController, GenomeMuseumControllerFactory}
import model.{MuseumSchema, MuseumExhibitLoader, DefaultMuseumExhibitFileLibrary,
  MuseumExhibitService}

/**
 * GenomeMuseum GUI アプリケーション実行クラス。
 * 
 * 各種コントローラのファクトリを担う。
 * 
 * @see #startup()
 * @see jp.scid.genomemuseum.controller.MainFrameViewController
 */
@ProxyActions(Array("selectAll"))
class GenomeMuseumGUI extends Application {
  import jp.scid.genomemuseum.model.MuseumExhibit
  import GenomeMuseumGUI._
  import RunMode._
  
  
  // リソースのネームスペースを無しに設定
  getContext.getResourceManager.setResourceFolder("")
  // アプリケーションのクラスを設定
  getContext.setApplicationClass(classOf[GenomeMuseumGUI])
  
  // プロパティ
  /** SAF のローカル保管場所を保持しておく */
  private val defaultLocalStorageDir = getContext.getLocalStorage.getDirectory
  /** アプリケーションのデータが保存されるディレクトリ */
  private var defaultApplicationHome: Option[File] = None
  /** データファイルが保存される基本ディレクトリ */
  var fileStorageDir: Option[File] = None
  /** データベース情報が保存される場所 */
  var databaseSource = "mem:" + util.Random.alphanumeric.take(5).mkString
  /** アプリケーションデータのディレクトリ */
  lazy val applicationHome: File = {
    getContext.getLocalStorage.setDirectory(defaultApplicationHome.getOrElse(createTempDir))
    getContext.getLocalStorage.getDirectory
  }
  
  // アプリケーションアクション
  /** 切り取りプロキシアクション */
  lazy val cutProxyAction = getAction("cut")
  /** コピープロキシアクション */
  lazy val copyProxyAction = getAction("copy")
  /** 貼付けプロキシアクション */
  lazy val pasteProxyAction = getAction("paste")
  /** 全てを選択プロキシアクション */
  lazy val selectAllProxyAction = getAction("selectAll")
  /** {@code chooseAndLoadFile} を呼び出すアクション */
  lazy val openAction = getAction("open")
  /** {@link GenomeMuseumGUI#exit} を呼び出すアクション */
  lazy val quitAction = getAction("quit")
  
  /**
   * 展示物のファイルライブラリオブジェクト
   * 
   * @see #fileStorageDir
   */
  lazy protected[genomemuseum] val exhibitFileLibrary: Option[DefaultMuseumExhibitFileLibrary] = {
    logger.info("Bio files stored on {}", fileStorageDir)
    
    fileStorageDir.map { dir => 
      dir.mkdirs
      new DefaultMuseumExhibitFileLibrary(dir)
    }
  }
  
  // モデル
  /**
   * アプリケーションのデータモデルのためのスキーマ
   * 
   * このスキーマオブジェクト遅延評価によって作成されるが、
   * オブジェクトは {@link databaseSource} の値によって変化し、
   * ファイルパスを返すとき、ファイルに保存されるスキーマを作成する。
   * この時の保存先はH2 Database の命名規則に従う（パスの最後に ".h2.db" ついたファイルが作成される）。
   * {@code mem:} から始まる文字列の時は、メモリー上にスキーマが作成される。
   * @see #databaseSource
   */
  lazy val museumSchema = {
    logger.info("Library on {}", databaseSource)
    val museumSchema = MuseumSchema.on(databaseSource)
    museumSchema.localFileStorage = exhibitFileLibrary.map(_.uriFileStorage)
    museumSchema
  }
  
  /**
   * バイオデータの読み込み操作オブジェクトを作成する。
   * 
   * ファイルライブラリとして {@code loadManager} が利用される。
   * @see #museumSchema
   * @see #loadManager
   */
  lazy val exhibitLoadManager = {
    val loadManager = new MuseumExhibitLoadManager(museumSchema.museumExhibitService)
    loadManager.fileLibrary = exhibitFileLibrary
    loadManager
  }
  
  // ビュー
  /**
   * このアプリケーションの画面オブジェクト
   * 
   * @see #createApplicationViews()
   */
  lazy val applicationViews = new ApplicationViews()
  
  // コントローラファクトリ
  /**
   * アプリケーションの操作オブジェクトの生成オブジェクトを作成
   */
  lazy val controllerFactory = new GenomeMuseumControllerFactory(this)
  
  // アプリケーション処理
  /**
   * アプリケーションのデータを保存するディレクトリを設定する。
   * 
   * 指定したディレクトリに則って、{@link #schemaFileSource} と {@link #fileStorageDir} が
   * 新しい値に設定される。
   */
  override protected[genomemuseum] def initialize(args: Array[String]) {
    if (args.contains("--LocalLibrary")) {
      val dir = defaultLocalStorageDir
      defaultApplicationHome = Option(dir)
      databaseSource = new File(dir, "Library/lib").toString
      fileStorageDir = Some(new File(dir, "BioFiles"))
    }
  }
  
  /**
   * 設定されたプロパティを利用して、メインビューの表示を行う。
   * 
   * Swing Application Framework によって起動時に一度呼び出される。
   * 
   * @see jp.scid.genomemuseu.controller.MainFrameViewController
   */
  override def startup() {
    logger.debug("startup")
    
    val mainFrameViewCtrl = controllerFactory.createMainFrameViewController()
    mainFrameViewCtrl.bind(applicationViews.mainVrameView)
    
    // 表示
    mainFrameViewCtrl.show()
  }
  
  override protected def ready() {
    logger.info("ready")
  }
  
  // アクションメソッド
  /**
   * ファイル選択ダイアログを表示し、選ばれたファイルをバイオデータとして読み込みを行う。
   * 
   * @see MuseumExhibitLoadManager#loadExhibit(File)
   */
  @Action(name="open")
  def chooseAndLoadFile() {
    val dialog = applicationViews.openDialog
    dialog.setVisible(true)
    
    Option(dialog.getFile).map(new File(dialog.getDirectory, _))
      .foreach(exhibitLoadManager.loadExhibit)
  }
  
  // コントローラ生成
  
  /**
   * アプリケーションのアクションを取得する
   */
  def getAction(key: String) = {
    // このクラスを Swing Application Framework 外でインスタンス化した時は
    // コンテキストオブジェクトにこのアプリケーションのクラスとインスタンスが設定されないための対応
    val context = getContext
    if (context.getApplication == null) {
      val setApplication = context.getClass.getDeclaredMethod("setApplication", classOf[Application])
      setApplication.setAccessible(true)
      setApplication.invoke(context, this)
    }
    
    ApplicationController.getAction(key, context.getActionMap)
  }
}

object GenomeMuseumGUI {
  import org.jdesktop.application.{ApplicationActionMap, ApplicationAction => SAFAction}
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


