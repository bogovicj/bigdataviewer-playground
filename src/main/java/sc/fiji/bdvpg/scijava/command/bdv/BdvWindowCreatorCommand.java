package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.*;
import bdv.viewer.render.AccumulateProjectorFactory;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.projector.AccumulateAverageProjectorARGB;
import sc.fiji.bdvpg.bdv.projector.AccumulateMixedProjectorARGBFactory;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Create Empty BDV Frame",
    label = "Creates an empty Bdv window")
public class BdvWindowCreatorCommand implements Command {

    @Parameter(label = "Create a 2D Bdv window")
    public boolean is2D = false;

    @Parameter(label = "Title of the new Bdv window")
    public String windowTitle = "Bdv";

    @Parameter(label = "Interpolate")
    public boolean interpolate = false;

    /**
     * This triggers: BdvHandlePostprocessor
     */
    @Parameter(type = ItemIO.OUTPUT)
    public BigDataViewer bdvh;

    @Parameter(choices = { Projection.MIXED_PROJECTOR, Projection.SUM_PROJECTOR, Projection.AVERAGE_PROJECTOR})
    public String projector;

    @Override
    public void run() {
        //------------ BdvHandleFrame
        //BdvOptions opts = BdvOptions.options().frameTitle(windowTitle);
        //if (is2D) opts = opts.is2D();

        // Create accumulate projector factory
        AccumulateProjectorFactory< ARGBType > factory = null;
        switch (projector) {
            case Projection.MIXED_PROJECTOR:
                factory = new AccumulateMixedProjectorARGBFactory(  );
                //opts = opts.accumulateProjectorFactory(factory);
            case Projection.SUM_PROJECTOR:
                // Default projector
                break;
            case Projection.AVERAGE_PROJECTOR:
                factory = AccumulateAverageProjectorARGB.factory;
                //opts = opts.accumulateProjectorFactory(factory);
                break;
            default:
        }

        BdvCreator creator = new BdvCreator(true); //opts, interpolate);
        creator.run();
        bdvh = creator.get();

        // Now we can add the bdvHandle to the projector factory
        switch (projector) {
            case Projection.MIXED_PROJECTOR:
                ((AccumulateMixedProjectorARGBFactory) factory).setBdvHandle( bdvh );
            case Projection.SUM_PROJECTOR:
                break;
            case Projection.AVERAGE_PROJECTOR:
                break;
            default:
        }
    }
}
