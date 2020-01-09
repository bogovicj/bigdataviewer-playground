package sc.fiji.bdvpg.scijava.widget;

import bdv.viewer.SourceAndConverter;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;

import javax.swing.*;
import java.awt.*;

/**
 * Swing implementation of {@link SourceAndConverterListWidget}.
 *
 * @author Nicolas Chiaruttini
 */

@Plugin(type = InputWidget.class)
public class SwingSourceAndConverterListWidget extends SwingInputWidget<SourceAndConverter[]> implements
        SourceAndConverterListWidget<JPanel> {

    /**
     * Scijava Object Service : contains all the sources
     */
    @Parameter
    private ObjectService objectService;

    @Override
    protected void doRefresh() {
    }

    @Override
    public boolean supports(final WidgetModel model) {
        return super.supports(model) && model.isType(SourceAndConverter[].class);
    }

    @Override
    public SourceAndConverter[] getValue() {
        if (sources.isSelectionEmpty()) {
            return null;
        } else {
            return sources.getSelectedValuesList().toArray(new SourceAndConverter[sources.getSelectedValuesList().size()]);
        }
    }

    JList<SourceAndConverter> sources;

    @Override
    public void set(final WidgetModel model) {
        super.set(model);
        sources = new JList<>((objectService.getObjects(SourceAndConverter.class).toArray(new SourceAndConverter[0])));
        sources.setDragEnabled(true);
        sources.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(sources);
        scrollPane.setPreferredSize(new Dimension(350, 100));
        getComponent().add(scrollPane);
        refreshWidget();
        model.setValue(null);
        sources.addListSelectionListener((e)-> {
            model.setValue(getValue());
        });
    }

}