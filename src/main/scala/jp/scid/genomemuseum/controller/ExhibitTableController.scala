package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField}
import java.awt.event.{KeyAdapter, KeyEvent}
import java.util.Comparator

import scala.collection.{mutable, script}
import mutable.{ObservableBuffer, Undoable}

import ca.odell.glazedlists.{swing => glswing, BasicEventList,
  TextFilterator, FilterList, SortedList}
import glswing.{EventTableModel, SearchEngineTextFieldMatcherEditor}

import jp.scid.genomemuseum.{gui, model}
import gui.{ExhibitTableFormat, ComparatorEditor, TableFormatComparatorFactory}
import model.{MuseumExhibit}

class ExhibitTableController(
  table: JTable, quickSearchField: JTextField
) {
  val tableSource = new BasicEventList[MuseumExhibit]
  protected val filterator = new ExhibitTableTextFilterator
  protected val matcherEditor = new SearchEngineTextFieldMatcherEditor(
    quickSearchField, filterator)
  protected val filteredSource = new FilterList(tableSource, matcherEditor)
  protected val sortedSource = new SortedList(filteredSource, null)
  protected val tableFormat = new ExhibitTableFormat
  protected val tableModel = new EventTableModel(sortedSource, tableFormat)
  protected val comparatorFactory = new TableFormatComparatorFactory(tableFormat)
  protected val tableHeaderSortHandler = new TableHeaderSortHandler[MuseumExhibit](
    table.getTableHeader, comparatorFactory)
  
  /** FormatTableColumnManager の接続除去関数 */
  private var comparatorEditorReactionRemover: () => Unit = () => ()
  
  // インクリメンタルサーチ
  quickSearchField addKeyListener new KeyAdapter {
    override def keyReleased(e: KeyEvent) {
      matcherEditor refilter quickSearchField.getText
    }
  }
  
  // データバインディング
  table.setModel(tableModel)
  
  // ソーティング
  sortWith(tableHeaderSortHandler.comparatorEditor)
  
  def bindTableSource(buf: ObservableBuffer[MuseumExhibit]) {
    import scala.collection.JavaConverters._
    type Event = script.Message[MuseumExhibit] with Undoable
    val subscription = new buf.Sub {
      import script._
      
      def notify(src: buf.Pub, event: Event) {
        tableSource.getReadWriteLock().writeLock().lock();
        try {
          processEvent(event)
        }
        finally {
          tableSource.getReadWriteLock().writeLock().unlock();
        }
      }
      
      def processEvent(event: Message[MuseumExhibit]): Unit = event match {
        case Include(loc, elm) =>
          tableSource add elm
        case Remove(loc, elm) =>
          tableSource remove elm
        case Update(loc, elm) => loc match {
          case Index(index) => tableSource.set(index, elm)
          case Start => tableSource.set(0, elm)
          case End => tableSource.set(tableSource.size, elm)
          case NoLo => // TODO logging
        }
        case event: Script[_] =>
          event.asInstanceOf[Script[MuseumExhibit]].foreach(processEvent)
        case Reset() => // TODO logging
      }
    }
    
    tableSource.getReadWriteLock().writeLock().lock();
    try {
      tableSource.clear()
      tableSource.addAll(buf.asJava)
      buf subscribe subscription
    }
    finally {
      tableSource.getReadWriteLock().writeLock().unlock();
    }
  }
  
  /** ソート用の Comparator を設定 */
  def sortWith(c: Comparator[MuseumExhibit]) {
    sortedSource setComparator c
  }
  
  /** ソート用に ComparatorEditor を設定 */
  private def sortWith(e: ComparatorEditor[MuseumExhibit]) {
    comparatorEditorReactionRemover()
    
    def resorting() = sortWith(e.comparator)
    val reaction: swing.Reactions.Reaction = {
      case ComparatorEditor.ComparatorChanged(e) => resorting()
    }
    e.reactions += reaction
    
    comparatorEditorReactionRemover = () => {
      e.reactions -= reaction
    }
    
    resorting()
  }
}

protected class ExhibitTableTextFilterator
    extends TextFilterator[MuseumExhibit] {
  import java.{util => ju}
  override def getFilterStrings(baseList: ju.List[String],
      element: MuseumExhibit) {
    baseList add element.name
    baseList add element.source
  }
}