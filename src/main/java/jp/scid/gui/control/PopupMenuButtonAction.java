package jp.scid.gui.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class PopupMenuButtonAction implements ActionListener {
    private final JPopupMenu popupMenu;

    public PopupMenuButtonAction(JPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

    public void installTo(JToggleButton toggle) {
        toggle.addActionListener(this);

        ButtonSelectionCancelHandler cancelHandler = new ButtonSelectionCancelHandler(toggle);
        popupMenu.addPopupMenuListener(cancelHandler);
        popupMenu.addPropertyChangeListener(cancelHandler);

        // Install a special client property on the button to prevent it from
        // closing of the popup when the down arrow is pressed.
        Object preventHide = new JComboBox().getClientProperty("doNotCancelPopup");
        toggle.putClientProperty("doNotCancelPopup", preventHide);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AbstractButton button = (AbstractButton) e.getSource();
        if( button.isSelected() )
            popupMenu.show(button, 0, button.getHeight());
        else
            popupMenu.setVisible(false);
    }

    private static class ButtonSelectionCancelHandler implements PopupMenuListener, PropertyChangeListener {
        private final AbstractButton button;
        public ButtonSelectionCancelHandler(AbstractButton button) {
            this.button = button;
        }

        public void popupMenuWillBecomeVisible ( PopupMenuEvent e ) {}
        public void popupMenuWillBecomeInvisible ( PopupMenuEvent e ) {}
        public void popupMenuCanceled ( PopupMenuEvent e ) {
            button.setSelected(false);
        }

        public void propertyChange(PropertyChangeEvent e) {
            if(Boolean.FALSE.equals(e.getNewValue()))
                button.setSelected(false);
        }
    }
}
