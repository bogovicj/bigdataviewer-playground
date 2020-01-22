package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.BigDataViewer;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Remove Sources From Bdv")
public class BdvSourcesRemoverCommand implements Command {

    @Parameter
    BigDataViewer bdvh;

    @Parameter
    SourceAndConverter[] srcs_in;

    @Override
    public void run() {
        for (SourceAndConverter src:srcs_in) {
            SourceAndConverterServices.getSourceAndConverterDisplayService().remove(bdvh, src);
        }
    }
}
