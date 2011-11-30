package jp.scid.genomemuseum.controller

import javax.swing.{JCheckBox, RootPaneContainer}
import scala.swing.Window

import jp.scid.genomemuseum.view
import view.ColumnVisibilitySetting
import org.jdesktop.application.Action

class ViewSettingDialogController(
  view: ColumnVisibilitySetting,
  rootPaneContainer: RootPaneContainer
) extends GenomeMuseumController {
  // Actions
  val showAction = getAction("show")
  val finishSettingAction = getAction("finishSetting")
  val cancelSettingAction = getAction("cancelSetting")
  
  view.okButton setAction finishSettingAction.peer
  view.cancelButton setAction cancelSettingAction.peer
  
  // Model
  /** 設定完了後に実行する処理 */
  var finishCallBack: List[String] => Any = (list) => {}
  /** チェックボックスにチェックを入れる列名リスト */
  var columnNameList: List[String] = Nil
  
  private def dialog = rootPaneContainer match {
    case window: java.awt.Window => Some(window)
    case _ => None
  }
  
  /** チェックボックスの状態を直して表示 */
  @Action(name="show")
  def show() {
    println("viewOption.show")
    reloadView()
    dialog.foreach { dialog =>
      dialog setLocationRelativeTo null
      dialog.pack()
      dialog setVisible true
    }
  }
  
  /** ダイアログを閉じて、コールバックを実行 */
  @Action(name="finishSetting")
  def finishSetting() {
    println("viewOption.finishSetting")
    val checked = checkedColumns
    columnNameList = checked
    close()
    finishCallBack(checked)
  }
  
  /** ダイアログを閉じる */
  @Action(name="cancelSetting")
  def cancelSetting() {
    println("viewOption.cancelSetting")
    close()
  }
  
  /** ダイアログを閉じる */
  protected def close() {
    dialog.foreach(_.setVisible(false))
  }
  
  /** モデルからチェックボックスの状態などを読み込み直す */
  protected def reloadView() {
    import collection.JavaConverters._
    val checks = view.getAllCheckBoxes.asScala
    checks foreach {_ setSelected false}
    columnNameList foreach { view.getCheckBox(_) setSelected true }
  }
  
  protected def checkedColumns = {
    import collection.mutable.{Buffer, ListBuffer}
    import collection.JavaConverters._
    
    def collectCheckColumns(names: Buffer[String], checks: List[JCheckBox]
        ): Buffer[String] = checks match {
      case check :: tail =>
        if (check.isSelected) names += check.getName
        collectCheckColumns(names, tail)
      case Nil => names
    }
    
    collectCheckColumns(ListBuffer.empty[String],
      view.getAllCheckBoxes.asScala.toList).toList
  }
}
