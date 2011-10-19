package jp.scid.genomemuseum.view

import java.awt.Component
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.{JTree, DefaultCellEditor, JTextField}

import com.explodingpixels.widgets.TextProvider

import jp.scid.genomemuseum.model.ExhibitListBox

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
    (event, event.getSource) match {
      case (e: MouseEvent, tree: JTree) =>
        tree.getPathForLocation(e.getX, e.getY) match {
          case null => false
          case path => path.getLastPathComponent match {
            case listBox: ExhibitListBox =>
              true
            case _ =>
              false
          }
        }
      case _ =>
        super.isCellEditable(event)
    }
  }
}
