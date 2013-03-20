package jp.scid.gui.control;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import jp.scid.gui.model.ValueModel;

import org.junit.Test;

public class AbstractControllerTest {

    <T> AbstractController<ValueModel<T>> createController() {
        return new SimpleAbstractController<T>();
    }
    
    @Test
    public void testExecute() {
        String method = "toString";
        String testString = "test";
        AbstractController.execute(testString, method);
        assertTrue(true);
    }
    
    @Test
    public void testExecute_exception() {
        String method = "====";
        String testString = "test";
        try {
            AbstractController.execute(testString, method);
        }
        catch (IllegalStateException expect) {
            assertTrue(true);
            return;
        }
        fail("must throws exception");
    }
    
    @Test
    public void getModel() throws Exception {
        AbstractController<ValueModel<String>> controller = createController();
        assertNull("model initial value", controller.getModel());
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void setModel() throws Exception {
        AbstractController<ValueModel<String>> controller = createController();
        
        ValueModel<String> model = mock(ValueModel.class);
        controller.setModel(model);
        
        assertSame("model set", model, controller.getModel());
        verify(model).addPropertyChangeListener(controller.modelPropertyChangeListener);
        verify(model, never()).removePropertyChangeListener(controller.modelPropertyChangeListener);
        
        ValueModel<String> model2 = mock(ValueModel.class);
        controller.setModel(model2);
        
        verify(model).removePropertyChangeListener(controller.modelPropertyChangeListener);
        assertSame("model set", model2, controller.getModel());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void setModel_property() throws Exception {
        AbstractController<ValueModel<String>> controller = createController();
        
        ValueModel<String> model = mock(ValueModel.class);
        controller.setModel(model, "prop");
        
        assertEquals("property set", "prop", controller.getProperty());
        
        controller.setModel(model, "prop2");
        
        assertEquals("property set", "prop2", controller.getProperty());
        
        controller.setModel(model);
        
        assertNull("property reset", controller.getProperty());
    }
    
    @Test
    public void getProperty() throws Exception {
        AbstractController<ValueModel<String>> controller = createController();
        
        assertNull("property initial value", controller.getProperty());
    }
    
    static class SimpleAbstractController<T> extends AbstractController<ValueModel<T>> {
        @Override
        protected void processPropertyChange(ValueModel<T> model, String property) {
        }
    }
}
