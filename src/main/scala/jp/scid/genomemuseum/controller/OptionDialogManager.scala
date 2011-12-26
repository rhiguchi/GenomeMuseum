package jp.scid.genomemuseum.controller

object OptionDialogManager {
  object MessageType extends Enumeration {
    type MessageType = Value
    val Error = Value
    val Warning = Value
    val Information = Value
  }
}

/**
 * 利用者に通知を行う操作
 */
trait OptionDialogManager {
  import OptionDialogManager.MessageType._
  
  def showMessage(message: String, description: Option[String] = None)
}