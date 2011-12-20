package jp.scid.genomemuseum.view

import javax.swing.{JFrame, JDialog}

/**
 * GenomeMuseum の画面と、それを表示する画面枠を保持する
 * オブジェクトの定義インターフェイス。
 * 
 * {@link #mainView()} が返す {@code MainView} は
 * {@link #frame()} が返す {@code JFrame} を親とする。
 */
class MainFrameView {
  /** @return メニューバー */
  val mainMenu = new MainViewMenuBar
  
  /** @return 画面オブジェクト */
  val mainView = new MainView
  
  /** @return この画面の親枠 */
  val frame = {
    val frame = createFrame()
    frame.setContentPane(mainView.contentPane)
    frame.setJMenuBar(mainMenu.container.peer)
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame
  }
  
  /** 列表示設定ビュー */
  val columnConfigPane = new ColumnVisibilitySetting
  
  /** 列表示設定ダイアログ */
  val columnConfigDialog = {
    val dialog = new JDialog(frame)
    dialog.setContentPane(columnConfigPane.contentPane)
    dialog
  }
  
  /** クローズボタンを無効化したフレームを作成する */
  private def createFrame() = {
    val frame = new JFrame()
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)
    frame
  }
}
