package jp.scid.genomemuseum.model

import javax.swing.SwingWorker.StateValue
import StateValue._

/**
 * 検索結果表現クラス
 */
case class SearchResult(
  val identifier: String,
  var accession: String = "",
  var definition: String = "",
  var length: Int = 0,
  var done: Boolean = false,
  var sourceUrl: Option[java.net.URL] = None
) extends TaskProgressModel {
  var progress = 0f
  var label = identifier
  var state: StateValue = PENDING
  
  def getProgress = progress
  
  def getLabel = label
  
  def isAvailable = true
  
  def getState = state
}
