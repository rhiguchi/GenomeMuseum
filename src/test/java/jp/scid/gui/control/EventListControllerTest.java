package jp.scid.gui.control;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

import org.junit.Before;
import org.junit.Test;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

public class EventListControllerTest {
    EventListController<String, ValueModel<List<String>>> ctrl = null;
    
    public EventListController<String, ValueModel<List<String>>> createController() {
        return new EventListController<String, ValueModel<List<String>>>();
    }
    
    @Before
    public void setup() throws Exception {
        ctrl = createController();
    }
    
    @Test
    public void setModel() {
        EventList<String> list = new BasicEventList<String>();
        ValueModel<List<String>> model = ValueModels.<List<String>>newValueModel(list);
        
        ctrl.setModel(model);
        
        assertSame(model, ctrl.getModel());
    }

    @Test
    public void getArrangedList() {
        EventList<String> list = new BasicEventList<String>();
        ValueModel<List<String>> model = ValueModels.<List<String>>newValueModel(list);
        
        ctrl.setModel(model);
        
        List<String> values = Arrays.asList("a", "s", "d");
        list.addAll(values);
        
        assertEquals("source value", values, ctrl.getSourceList());
        assertEquals("source value", values, ctrl.getArrangedList());
    }
    
    @Test
    public void getSelectedElements() {
        EventList<String> list = GlazedLists.eventListOf("a", "s", "d");
        ValueModel<List<String>> model = ValueModels.<List<String>>newValueModel(list);
        
        ctrl.setModel(model);
        
        assertTrue("empty", ctrl.getSelectedElements().isEmpty());
        
        ctrl.getSelectionModel().addSelectionInterval(0, 1);
        assertEquals("select 1", Arrays.asList("a", "s"), ctrl.getSelectedElements());
        
        ctrl.getSelectionModel().setSelectionInterval(2, 2);
        assertEquals("select 2", Arrays.asList("d"), ctrl.getSelectedElements());
    }
}
