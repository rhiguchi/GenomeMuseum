package jp.scid.genomemuseum.controller

import javax.swing.JViewport
import javax.swing.text.{PlainDocument, Segment}
import javax.swing.event.{ChangeListener, ChangeEvent}
import jp.scid.genomemuseum.view.FileContentView

/**
 * ファイルの中身を表示するコントローラ
 */
class FileContentViewer(view: FileContentView) {
  import swing.Swing.onEDT
  
  private val viewportChangeListener = new ChangeListener {
    def stateChanged(e: ChangeEvent) {
      contentScrollChanged()
    }
  }
  
  // View
  private def textArea = view.textArea
  private def scrollViewport = view.textAreaScroll.getViewport
  scrollViewport.addChangeListener(viewportChangeListener)
  
  // Model
  val document = new PlainDocument()
  
  private def contentScrollChanged() {
    if (isTextPaneVisibled && isScrollPositionEnd &&
        currentSource.hasNext) onEDT {
      if (currentSource.hasNext) {
        loadSource()
        contentScrollChanged()
      }
    }
  }
  
  /** ビューワーのスクロールが最後までいっているか */
  private def isScrollPositionEnd: Boolean = {
    val bottom = scrollViewport.getViewRect.getMaxY match {
      case bottom if bottom < 0 => 0
      case bottom => bottom.asInstanceOf[Int]
    }
    val viewHeight = scrollViewport.getViewSize.height
    
    // 32 ピクセルの余裕
    bottom >= viewHeight - 32
  }
  
  /** ビューが表示状態になっているか */
  private def isTextPaneVisibled: Boolean = {
    scrollViewport.getExtentSize.height > 0
  }
  
  private var currentSource: Iterator[String] = Iterator()
  
  textArea setDocument document
  
  /** 現在のソースを取得 */
  def source = currentSource
  
  /** ビューワーに表示するソースを設定 */
  def source_=(newSource: Iterator[String]) {
    currentSource = newSource
    clearDocument()
    loadSource()
  }
  
  /** Document の中身を消去する */
  private def clearDocument() {
    document.remove(0, document.getLength)
  }
  
  /** ソースからの読み込みを行う */
  private def loadSource(lines: Int = 10) {
    def loadNext(source: Iterator[String], lines: Int) {
      if (source.hasNext && lines > 0) {
        loadOne(source)
        loadNext(source, lines - 1)
      }
    }
    
    val caretPos = textArea.getCaretPosition
    loadNext(currentSource, lines)
    textArea setCaretPosition caretPos
  }
  
  /** ソースから 1 要素読み込む */
  private def loadOne(source: Iterator[String]) {
    val line = source.next + "\n"
    document.insertString(document.getLength, line, null)
  }
}
