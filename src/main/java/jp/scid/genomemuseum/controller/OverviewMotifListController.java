package jp.scid.genomemuseum.controller;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;

import jp.scid.genomemuseum.view.OverviewMotifListView;
import jp.scid.gui.control.BooleanPropertyBinder;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;
import jp.scid.motifviewer.controller.MotifViewerController;
import jp.scid.motifviewer.view.OverviewPaintingStrategy;
import jp.scid.motifviewer.view.OverviewPane;

public class OverviewMotifListController extends MotifViewerController {
    static enum StrandStyle {
        SINGLE, DOUBLE;
    }
    
    // Strand
    private final ValueModel<StrandStyle> strandStyleModel =
            ValueModels.newValueModel(StrandStyle.DOUBLE);
    
    private final BooleanPropertyBinder singleStrandSelected = new BooleanPropertyBinder(
            ValueModels.newSelectionBooleanModel(strandStyleModel, StrandStyle.SINGLE));
    
    private final BooleanPropertyBinder doubleStrandSelected = new BooleanPropertyBinder(
            ValueModels.newSelectionBooleanModel(strandStyleModel, StrandStyle.DOUBLE));

    // Shape
    private final ValueModel<OverviewPaintingStrategy> paintingStrategy =
            ValueModels.newValueModel(OverviewPaintingStrategy.CIRCULAR);
    
    private final BooleanPropertyBinder circularShapeSelected = new BooleanPropertyBinder(
            ValueModels.newSelectionBooleanModel(paintingStrategy, OverviewPaintingStrategy.CIRCULAR));
    
    private final BooleanPropertyBinder linearShapeSelected = new BooleanPropertyBinder(
            ValueModels.newSelectionBooleanModel(paintingStrategy, OverviewPaintingStrategy.LINEAR));
    
    // Actions
    private final StrandChangeAction singleStrandChangeAction = new StrandChangeAction(StrandStyle.SINGLE);
    private final StrandChangeAction doubleStrandChangeAction = new StrandChangeAction(StrandStyle.DOUBLE);
    
    private final ShapeChangeAction circularShapeChangeAction =
            new ShapeChangeAction(OverviewPaintingStrategy.CIRCULAR);
    private final ShapeChangeAction linearShapeChangeAction =
            new ShapeChangeAction(OverviewPaintingStrategy.LINEAR);
    
    
    public OverviewMotifListController() {
    }
    
    public void bind(OverviewMotifListView view) {
        bindSingleStrandButton(view.singeStrandButton);
        bindDoubleStrandButton(view.doubleStrandButton);
        
        bindCircularShapeButton(view.circularShapeButton);
        bindLinearShapeButton(view.linearShapeButton);
        
        bindSearchMotifField(view.searchMotifField);
        bindPositionMarkerTable(view.motifListTable);
        
        bindOverviewPane(view.overviewPane);
    }
    
    @Override
    public void bindOverviewPane(OverviewPane pane) {
        // TODO Auto-generated method stub
        super.bindOverviewPane(pane);
    }
    
    public void bindSingleStrandButton(AbstractButton button) {
        singleStrandSelected.bindButtonSelection(button);
        button.addActionListener(singleStrandChangeAction);
    }
    
    public void bindDoubleStrandButton(AbstractButton button) {
        doubleStrandSelected.bindButtonSelection(button);
        button.addActionListener(doubleStrandChangeAction);
    }
    
    public void bindCircularShapeButton(AbstractButton button) {
        circularShapeSelected.bindButtonSelection(button);
        button.addActionListener(circularShapeChangeAction);
    }
    
    public void bindLinearShapeButton(AbstractButton button) {
        linearShapeSelected.bindButtonSelection(button);
        button.addActionListener(linearShapeChangeAction);
    }
    
    // Action classes 
    class StrandChangeAction extends AbstractAction {
        private final StrandStyle strandStyle;
        
        public  StrandChangeAction(StrandStyle strandStyle) {
            this.strandStyle = strandStyle;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            strandStyleModel.setValue(strandStyle);
        }
    }
    
    class ShapeChangeAction extends AbstractAction {
        private final OverviewPaintingStrategy shape;
        
        public  ShapeChangeAction(OverviewPaintingStrategy shape) {
            this.shape = shape;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            paintingStrategy.setValue(shape);
        }
    }
}