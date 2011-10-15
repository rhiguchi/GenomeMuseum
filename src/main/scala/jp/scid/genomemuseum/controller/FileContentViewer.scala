package jp.scid.genomemuseum.controller

import javax.swing.text.PlainDocument
import jp.scid.genomemuseum.view.FileContentView

/**
 * ファイルの中身を表示するコントローラ
 */
class FileContentViewer(view: FileContentView) {
  
  // View
  private def textArea = view.textArea
  
  // Model
  val document = new PlainDocument()
  
  private var currentSource: Iterator[String] = Iterator()
  
  textArea setDocument document
  
  def source = currentSource
  def source_=(newSource: Iterator[String]) {
    // TODO stop current loading
    
    currentSource = newSource
    clearDocument()
    loadSource()
  }
  
  /** Document の中身を消去する */
  private def clearDocument() {
    document.remove(0, document.getLength)
  }
  
  /** ソースからの読み込みを行う */
  private def loadSource() {
    def loadNext(source: Iterator[String]) {
      if (source.hasNext) {
        val line = source.next + "\n"
        document.insertString(document.getLength, line, null)
        loadNext(source)
      }
    }
    
    loadNext(currentSource)
  }
}