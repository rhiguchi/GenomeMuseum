package jp.scid.genomemuseum.controller

import java.util.ResourceBundle
import java.awt.FileDialog
import java.io.{File, FileInputStream}
import jp.scid.genomemuseum.view
import view.{MainView, MainViewMenuBar}
import javax.swing.JFrame
import scala.swing.Action

class MainViewController(
  mainView: MainView,
  frameOfMainView: JFrame,
  menu: MainViewMenuBar
) {
  val tableCtrl = new ExhibitTableController(mainView.dataTable)
  val openAction = Action("Open") { openFile }
  val quitAction = Action("Quit") { quitApplication }
  lazy val openDialog = new FileDialog(frameOfMainView, "", FileDialog.LOAD)
  
  bindActions(menu)
  reloadResources()
  
  def showFrame() {
    frameOfMainView.pack
    frameOfMainView setLocationRelativeTo null
    frameOfMainView setVisible true
  }
  
  def openFile() {
    println("openFile")
    openDialog setVisible true
    val fileName = Option(openDialog.getFile)
    
    fileName.map(new File(openDialog.getDirectory, _)).foreach(loadBioFile)
  }
  
  def quitApplication() {
    System.exit(0)
  }
  
  protected def loadBioFile(file: File) {
    import jp.scid.genomemuseum.model.MuseumExhibit
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
    
    e.map(tableCtrl.tableSource.add)
    
  }
  
  def bindActions(menu: MainViewMenuBar) {
    menu.open.action = openAction
    menu.quit.action = quitAction
  }
  
  /** リソースを設定する */
  private def reloadResources() {
    reloadResources(ResourceBundle.getBundle(
      classOf[MainViewController].getName))
  }
  
  private def reloadResources(res: ResourceBundle) {
    val rm = new ResourceManager(res)
    rm.injectTo(openAction, "openAction")
    rm.injectTo(quitAction, "quitAction")
    menu.reloadResources
  }
}

class ResourceManager(res: ResourceBundle) {
  import collection.JavaConverters._
  import java.lang.Boolean.parseBoolean
  import javax.swing.KeyStroke.getKeyStroke
  
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
