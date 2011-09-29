package jp.scid.genomemuseum

import java.awt.{BorderLayout}
import view.MainView
import controller.ExhibitTableController
import scala.swing.{MainFrame}

class GenomeMuseumGUI {
  lazy val mainFrame = new MainFrame {
    val mainView = new MainView
    title = "GenomeMuseum"
    peer.getContentPane.setLayout(new BorderLayout)
    peer.getContentPane.add(mainView.contentPane, "Center")
  }
  
  def start() {
    val tableCtrl = new ExhibitTableController(
      mainFrame.mainView.dataTable)
    loadSampleFilesTo(tableCtrl)
    mainFrame.pack()
    mainFrame.peer setLocationRelativeTo null
    mainFrame.visible = true
  }
  
  /**
   * サンプルデータを 10 件設定する
   * @param controller 流し込み先コントローラ
   */
  private def loadSampleExhibitsTo(controller: ExhibitTableController) {
    import model.MuseumExhibit
    import ca.odell.glazedlists.GlazedLists
    import scala.collection.JavaConverters._
    
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
    val target = controller.tableSource
    target.getReadWriteLock().writeLock().lock();
    try {
      GlazedLists.replaceAll(target, samples.asJava, true)
    }
    finally {
      target.getReadWriteLock().writeLock().unlock();
    }
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