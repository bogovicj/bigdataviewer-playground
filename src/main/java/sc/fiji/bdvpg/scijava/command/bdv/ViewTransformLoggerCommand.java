package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.BigDataViewer;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformLogger;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

/**
 * ViewTransformLoggerCommand
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Log view transform")
public class ViewTransformLoggerCommand implements Command {

    @Parameter
    BigDataViewer bdvh;

    @Override
    public void run() {
        new ViewerTransformLogger(bdvh).run();
    }
}
