package jp.scid.genomemuseum.view

import java.awt.Component
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.{JTree, DefaultCellEditor, JTextField}

import com.explodingpixels.widgets.TextProvider

import jp.scid.genomemuseum.model.UserExhibitRoom

class SourceListCellEditor(field: JTextField) extends DefaultCellEditor(field) {
  def this() = this(new JTextField)
  
  override def getTreeCellEditorComponent(tree: JTree, value: AnyRef,
      isSelected: Boolean, expanded: Boolean, leaf: Boolean, row: Int) = {
    val cell = super.getTreeCellEditorComponent(
      tree, value, isSelected, expanded, leaf, row)
    
    value match {
      case node: TextProvider =>
        delegate setValue node.getText
      case _ =>
    }
    
    cell
  }
  
  override def isCellEditable(event: EventObject) = {
    event match {
      case e: MouseEvent => event.getSource.asInstanceOf[JTree].getPathForLocation(e.getX, e.getY) match {
        case null => false
        case path => path.getLastPathComponent match {
          case room: UserExhibitRoom => true
          case _ => false
        }
      }
      case _ => super.isCellEditable(event)
    }
  }
}
