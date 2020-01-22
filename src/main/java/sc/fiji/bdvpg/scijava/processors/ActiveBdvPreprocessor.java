package sc.fiji.bdvpg.scijava.processors;

import bdv.BigDataViewer;
import net.imagej.display.process.SingleInputPreprocessor;
import org.scijava.Priority;
import org.scijava.command.CommandService;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;
import sc.fiji.bdvpg.scijava.services.GuavaWeakCacheService;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Fills single, unresolved module inputs with the active {@link BigDataViewer},
 * <em>or a newly created one if none</em>.
 *
 * @author Curtis Rueden, Nicolas Chiaruttini
 */
@Plugin(type = PreprocessorPlugin.class, priority = Priority.VERY_HIGH)
public class ActiveBdvPreprocessor extends SingleInputPreprocessor<BigDataViewer>  {

    @Parameter
    private ObjectService os;

    @Parameter
    CommandService cs;

    @Parameter
    GuavaWeakCacheService cacheService;

    public ActiveBdvPreprocessor() {
        super( BigDataViewer.class );
    }

    // -- SingleInputProcessor methods --

    @Override
    public BigDataViewer getValue() {

        List<BigDataViewer> bdvhs = os.getObjects(BigDataViewer.class);

        if ((bdvhs == null)||(bdvhs.size()==0)) {
             try
            {
                return (BigDataViewer)
                        cs.run(BdvWindowCreatorCommand.class,true,
                            "is2D", false,
                            "windowTitle", "Bdv")
                                .get()
                                .getOutput("bdvh");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (bdvhs.size()==1) {
            return bdvhs.get(0);
        } else {

            // Get the one with the most recent focus ?
            Optional<BigDataViewer> bdvh = bdvhs.stream().filter(b -> b.getViewer().hasFocus()).findFirst();
            if (bdvh.isPresent()) {
                return bdvh.get();
            } else {
                if (cacheService.get("LAST_ACTIVE_BDVH")!=null) {
                    WeakReference<BigDataViewer> wr_bdv_h = (WeakReference<BigDataViewer>) cacheService.get("LAST_ACTIVE_BDVH");
                    return wr_bdv_h.get();
                } else {
                    return null;
                }
            }
        }
    }

}
