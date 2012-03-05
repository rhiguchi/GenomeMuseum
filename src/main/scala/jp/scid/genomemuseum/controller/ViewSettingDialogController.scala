package jp.scid.genomemuseum.controller

import javax.swing.{JDialog, JCheckBox, RootPaneContainer}
import scala.swing.Window

import jp.scid.genomemuseum.view
import view.ColumnVisibilitySetting
import org.jdesktop.application.Action

import jp.scid.gui.model.ValueModels
import jp.scid.gui.control.BooleanPropertyBinder

class ViewSettingDialogController(
  view: ColumnVisibilitySetting,
  rootPaneContainer: RootPaneContainer
) {
  
  private val ctrl = GenomeMuseumController(this)
  
  // Property
  /** ダイアログの表示状態を保持するモデル */
  private val dialogVisibled = ValueModels.newBooleanModel(false)
  
  // binding
  private val dialogVisibledBinder = new BooleanPropertyBinder(dialogVisibled)
  
  // Actions
  val showAction = ctrl.getAction("show")
  val finishSettingAction = ctrl.getAction("finishSetting")
  val cancelSettingAction = ctrl.getAction("cancelSetting")
  
//  view.okButton setAction finishSettingAction.peer
//  view.cancelButton setAction cancelSettingAction.peer
  
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
    reloadView()
    dialogVisibled.setValue(true)
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
  
  /** 変更を適用せずダイアログを閉じる */
  @Action(name="cancelSetting")
  def close() {
    dialogVisibled.setValue(false)
  }
  
  /** ビューを表示するダイアログと結合する */
  def bindDialog(view: JDialog) {
    view.setLocationRelativeTo(null)
    view.pack()
    dialogVisibledBinder.bindVisible(view)
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
