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
    loadSampleExhibitsTo(tableCtrl)
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
}