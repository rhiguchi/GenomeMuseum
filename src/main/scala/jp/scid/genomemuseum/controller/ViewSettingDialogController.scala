package jp.scid.genomemuseum.controller

import jp.scid.genomemuseum.view
import view.ColumnVisibilitySetting
import javax.swing.JCheckBox
import scala.swing.Window
import org.jdesktop.application.Action

class ViewSettingDialogController(
  view: ColumnVisibilitySetting,
  dialog: Window
) extends ApplicationController {
  // Actions
  val showAction = actionFor("show")
  val finishSettingAction = actionFor("finishSetting")
  val cancelSettingAction = actionFor("cancelSetting")
  
  view.okButton setAction finishSettingAction
  view.cancelButton setAction cancelSettingAction
  
  // Model
  /** 設定完了後に実行する処理 */
  var finishCallBack: List[String] => Any = (list) => {}
  /** チェックボックスにチェックを入れる列名リスト */
  var columnNameList: List[String] = Nil
  
  /** チェックボックスの状態を直して表示 */
  @Action
  def show() {
    println("viewOption.show")
    reloadView()
    dialog.peer setLocationRelativeTo null
    dialog.pack()
    dialog.visible = true
  }
  
  /** ダイアログを閉じて、コールバックを実行 */
  @Action
  def finishSetting() {
    println("viewOption.finishSetting")
    val checked = checkedColumns
    columnNameList = checked
    close()
    finishCallBack(checked)
  }
  
  /** ダイアログを閉じる */
  @Action
  def cancelSetting() {
    println("viewOption.cancelSetting")
    close()
  }
  
  /** ダイアログを閉じる */
  protected def close() {
    dialog.visible = false
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

import org.jdesktop.application.{Application, ApplicationActionMap}

trait ApplicationController {
  import javax.swing.Action
  
  private lazy val myActionMap = Application.getInstance.getContext.getActionMap(this)
  
  protected def actionFor(key: String, actionMap: ApplicationActionMap = myActionMap): Action =
    actionMap.get(key) match {
      case null => throw new IllegalStateException(
        "Action '%s' is not defined on '%s'.".format(key, actionMap.getActionsClass))
      case action => action
    }
}