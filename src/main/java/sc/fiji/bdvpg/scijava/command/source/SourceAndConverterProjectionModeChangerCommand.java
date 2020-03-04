package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.display.ProjectionModeChanger;

import static sc.fiji.bdvpg.bdv.projector.Projection.PROJECTION_MODE_AVG;
import static sc.fiji.bdvpg.bdv.projector.Projection.PROJECTION_MODE_SUM;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Display>Set Sources Projection Mode")
public class SourceAndConverterProjectionModeChangerCommand implements Command {

    @Parameter ( label = "Projection Mode", choices = { "SUM", "AVERAGE" })
    String projectionMode = "SUM";

    @Parameter
    SourceAndConverter[] sacs;

    @Parameter
    int layerIndex = 0;

    @Override
    public void run() {
        switch (projectionMode) {
            case "SUM":
                new ProjectionModeChanger( sacs, PROJECTION_MODE_SUM, layerIndex ).run();
                break;
            case "AVERAGE":
                new ProjectionModeChanger( sacs, PROJECTION_MODE_AVG, layerIndex ).run();
                break;
        }
    }

}
