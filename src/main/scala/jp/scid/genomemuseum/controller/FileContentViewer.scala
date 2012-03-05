package jp.scid.genomemuseum.controller

import java.io.{Reader, InputStreamReader, IOException}
import java.net.URI
import javax.swing.{JViewport}
import javax.swing.text.{JTextComponent, PlainDocument, Segment}
import javax.swing.event.{ChangeListener, ChangeEvent}
import jp.scid.gui.control.{ViewValueConnector}

/**
 * ファイルの中身を表示するコントローラ
 */
class FileContentViewer(textArea: JTextComponent) extends ViewValueConnector[JTextComponent, URI](textArea) {
  private val viewportChangeListener = new ChangeListener {
    def stateChanged(e: ChangeEvent) =
      contentScrollChanged(Some(e.getSource.asInstanceOf[JViewport]))
  }
  
  // View
  private var currentViewport: Option[JViewport] = None
  
  /** 読み込み中のReader */
  private var sourceReader: Option[Reader] = None
  
  private def contentScrollChanged(viewPort: Option[JViewport]): Unit = java.awt.EventQueue.invokeLater(new Runnable {
    def run() = sourceReader match {
      case Some(reader) => viewPort match {
        case Some(v) => isScrollPositionEnd(v) match {
          case true => readMore(reader, viewPort)
          case false =>
        }
        case None => readMore(reader, viewPort)
      }
      case None =>
    }
  })
  
  /** スクロールが末端に到達しているかを次の EDT 上で調べ、再読み込みを行う */
  private def readMore(reader: Reader, viewPort: Option[JViewport]) {
    loadSource(reader)
    contentScrollChanged(viewPort)
  }
  
  /** ビューワーのスクロールが最後まで到達したか */
  private def isScrollPositionEnd(viewPort: JViewport) = {
    val bottom = viewPort.getViewRect.getMaxY match {
      case bottom if bottom < 0 => 0
      case bottom => bottom
    }
    val viewHeight = viewPort.getViewSize.height
    
    // 32 ピクセルの余裕
    bottom >= viewHeight - 32
  }
  
  /** Document の中身を消去する */
  def clear() = textArea.setText("")
  
  /** Reader を閉じる */
  private def close(reader: Reader) = try reader.close catch { case e: IOException => }
  
  
  /** ビューワーに表示するソースを設定 */
  override def updateView(view: JTextComponent, newSource: URI) {
    clear()
    sourceReader foreach close
    sourceReader = Option(newSource) map (s => new InputStreamReader(s.toURL.openStream))
    
    val parent = view.getParent match {
      case null => None
      case parent => Some(parent.asInstanceOf[JViewport])
    }
    contentScrollChanged(parent)
  }
  
  /** textArea の親となる JViewport を設定する */
  def listenTo(viewPort: JViewport) {
    viewPort.addChangeListener(viewportChangeListener)
    contentScrollChanged(Some(viewPort))
  }
  
  /** 文字列を textArea に追加する */
  private def addString(text: String) {
    // キャレットの位置、選択範囲を保存する
    val caretPos = textArea.getCaretPosition
    val (selStart, selEnd) = (textArea.getSelectionStart, textArea.getSelectionEnd)
    
    val doc = textArea.getDocument
    doc.insertString(doc.getLength, text, null)
  
    textArea setCaretPosition caretPos
    textArea.setSelectionStart(selStart)
    textArea.setSelectionEnd(selEnd)
  }
  
  /** ソースからの読み込みを行う */
  private def loadSource(reader: Reader, size: Int = 8196) = {
    val cbuf = new Array[Char](size)
    val text = try reader.read(cbuf) match {
      case -1 => ""
      case read => new String(cbuf, 0, read)
    }
    catch {
      case e: IOException => ""
    }
    
    text.isEmpty match {
      case true => close(reader); false
      case false => addString(text); true
    }
  }
}
