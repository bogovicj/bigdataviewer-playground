package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.*;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.projector.AccumulateAverageProjectorARGB;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Create Empty BDV Frame",
    label = "Creates an empty Bdv window")
public class BdvWindowCreatorCommand implements Command {

    public static final String SUM_PROJECTOR = "Sum Projector";
    public static final String AVERAGE_PROJECTOR = "Average Projector";

    @Parameter(label = "Create a 2D Bdv window")
    public boolean is2D = false;

    @Parameter(label = "Title of the new Bdv window")
    public String windowTitle = "Bdv";

    @Parameter(type = ItemIO.OUTPUT)
    public BdvHandle bdvh;

    @Parameter(choices = { SUM_PROJECTOR, AVERAGE_PROJECTOR })
    public String projector;

    @Override
    public void run() {
        //------------ BdvHandleFrame
        BdvOptions opts = BdvOptions.options().frameTitle(windowTitle);
        if (is2D) opts = opts.is2D();
        switch (projector) {
            case "Sum Projector":
                // Default projector
                break;
            case "Average Projector":
                opts = opts.accumulateProjectorFactory(AccumulateAverageProjectorARGB.factory);
                break;
            default:
        }

        BdvCreator creator = new BdvCreator(opts);
        creator.run();
        bdvh = creator.get();
    }
}
