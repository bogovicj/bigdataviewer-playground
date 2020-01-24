package sc.fiji.bdvpg.scijava.services;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.command.source.*;
import sc.fiji.bdvpg.scijava.services.ui.BdvSourceServiceUI;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Scijava Service which centralizes Bdv Sources, independently of their display
 * Bdv Sources can be registered to this Service.
 * This service adds the Source to the ObjectService, but on top of it,
 * It contains a Map which contains any object which can be linked to the sourceandconverter.
 *
 * It also handles SpimData object, but split all Sources into individual ones
 *
 * The most interesting of these objects are actually created by the BdvSourceAndConverterDisplayService:
 * - Converter to ARGBType, ConverterSetup, and Volatile view
 *
 * TODO : Think more carefully : maybe the volatile sourceandconverter should be done here...
 * Because when multiply wrapped sourceandconverter end up here, it maybe isn't possible to make the volatile view
 */

@Plugin(type=Service.class)
public class SourceAndConverterService extends AbstractService implements SciJavaService, ISourceAndConverterService
{

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println( SourceAndConverterService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println( SourceAndConverterService.class.getSimpleName()+":"+str);

    /**
     * Scijava Object Service : will contain all the sourceAndConverters
     */
    @Parameter
    ObjectService objectService;

    /**
     * Scriptservice : used for adding Source alias to help for scripting
     */
    @Parameter
    ScriptService scriptService;

    /**
     * uiService : used ot check if an UI is available to create a Swing Panel
     */
    @Parameter
    UIService uiService;

    /**
     * Display service : cannot be set through Parameter annotation due to 'circular dependency'
     */
    SourceAndConverterBdvDisplayService bsds = null;

    /**
     * Map containing objects that are 1 to 1 linked to a Source
     * TODO : ask if it should contain a WeakReference to Source keys (Potential Memory leak ?)
     */
    Map<SourceAndConverter, Map<String, Object>> sacToMetadata;

    /**
     * Reserved key for the data map. data.get(sourceandconverter).get(SPIM_DATA)
     * is expected to return a List of Spimdata Objects which refer to this sourceandconverter
     * whether a list of necessary is not obvious at the moment
     * TODO : make an example
     */
    final public static String SPIM_DATA_INFO = "SPIMDATA";

    /**
     * Test if a Source is already registered in the Service
     * @param src
     * @return
     */
    public boolean isRegistered(SourceAndConverter src) {
        return sacToMetadata.containsKey(src);
    }

    public void setDisplayService( SourceAndConverterBdvDisplayService bsds) {
        assert bsds instanceof SourceAndConverterBdvDisplayService;
        this.bsds = ( SourceAndConverterBdvDisplayService ) bsds;
    }

    /**
     * Gets lists of associated objects and data attached to a Bdv Source
     * @return
     */
    @Override
    public Map<SourceAndConverter, Map<String, Object>> getSacToMetadata() {
        return sacToMetadata;
    }

    @Override
    public void setMetadata( SourceAndConverter sac, String key, Object data )
    {
        sacToMetadata.get( sac ).put( key, data );
    }

    @Override
    public Object getMetadata( SourceAndConverter sac, String key )
    {
        if (sacToMetadata.containsKey(sac)) {
            return sacToMetadata.get(sac).get(key);
        } else {
            return null;
        }
    }

    /**
     * Register a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param sac
     */
    public void register(SourceAndConverter sac) {
        if ( sacToMetadata.containsKey(sac)) {
            log.accept("Source already registered");
            return;
        }
        final int numTimepoints = 1;
        Map<String, Object> sourceData = new HashMap<>();
        sacToMetadata.put(sac, sourceData);
        objectService.addObject(sac);
        if (uiAvailable) ui.update(sac);
    }

    public void register(Collection<SourceAndConverter> sources) {
        for (SourceAndConverter sac:sources) {
            this.register(sac);
        }
    }

    public void register(AbstractSpimData asd) {
        Map<Integer, SourceAndConverter> sacs = SourceAndConverterUtils.createSourceAndConverters(asd);
        this.register(sacs.values());
        sacs.forEach((id,sac) -> {
            this.linkToSpimData(sac, asd, id);
            if (uiAvailable) ui.update(sac);
        });
    }

    @Override
    public void remove(SourceAndConverter sac ) {
        // Remove displays
        if (bsds!=null) {
            bsds.removeFromAllBdvs( sac );
        }
        sacToMetadata.remove( sac );
        objectService.removeObject( sac );
        if (uiAvailable) {
            ui.remove( sac );
        }
    }

    @Override
    public List<SourceAndConverter> getSourceAndConverters() {
        return objectService.getObjects(SourceAndConverter.class);
    }

    @Override
    public List<SourceAndConverter> getSourceAndConverterFromSpimdata(AbstractSpimData asd) {
        return objectService.getObjects(SourceAndConverter.class)
                .stream()
                .filter(s -> ((SpimDataInfo)sacToMetadata.get(s).get(SPIM_DATA_INFO)!=null))
                .filter(s -> ((SpimDataInfo)sacToMetadata.get(s).get(SPIM_DATA_INFO)).asd.equals(asd))
                .collect(Collectors.toList());
    }

    public void linkToSpimData( SourceAndConverter sac, AbstractSpimData asd, int idSetup) {
        sacToMetadata.get( sac ).put( SPIM_DATA_INFO, new SpimDataInfo(asd,idSetup));
    }


    /**
     * Swing UI for this Service, exists only if an UI is available in the current execution context
     */
    BdvSourceServiceUI ui;

    /**
     * Flags if the Inner UI exists
     */
    boolean uiAvailable = false;

    public BdvSourceServiceUI getUI() {
        return ui;
    }

    /**
     * Service initialization
     */
    @Override
    public void initialize() {
        scriptService.addAlias(SourceAndConverter.class);
        // -- TODO Check whether it's a good idea to add this here
        scriptService.addAlias(RealTransform.class);
        scriptService.addAlias(AffineTransform3D.class);
        scriptService.addAlias(AbstractSpimData.class);
        // -- TODO End of to check
        sacToMetadata = new HashMap<>();
        if (uiService!=null) {
            log.accept("uiService detected : Constructing JPanel for BdvSourceAndConverterService");
            ui = new BdvSourceServiceUI(this);
            uiAvailable = true;
        }
        registerPopupActions();
        SourceAndConverterServices.setSourceAndConverterService(this);
        log.accept("Service initialized.");
    }

    //Map<String, Consumer<SourceAndConverter[]>> popupactions = new HashMap<>();

    public void registerPopupSourcesAction(Consumer<SourceAndConverter[]> action, String actionName) {
        if (uiAvailable) {
            //popupactions.put(actionName, action);
            ui.addPopupAction(action, actionName);
        }
    }

    public List<SourceAndConverter> getSourceAndConvertersFromSource(Source src) {
        return getSourceAndConverters().stream().filter( sac -> sac.getSpimSource().equals(src)).collect(Collectors.toList());
    }

    @Parameter
    CommandService commandService;

    public void registerScijavaCommand(Class<? extends Command> commandClass ) {
        CommandInfo ci =commandService.getCommand(commandClass);

        int nCountSourceAndConverter = 0;
        int nCountSourceAndConverterList = 0;
        if (ci.inputs()!=null) {
            for (ModuleItem input: ci.inputs()) {
                if (input.getType().equals(SourceAndConverter.class)) {
                    nCountSourceAndConverter++;
                }
                if (input.getType().equals(SourceAndConverter[].class)) {
                    nCountSourceAndConverterList++;
                }
            }
            if (nCountSourceAndConverter+nCountSourceAndConverterList==1) {
                // Can be automatically mapped to popup action
                for (ModuleItem input: ci.inputs()) {
                    if (input.getType().equals(SourceAndConverter.class)) {
                        // It's an action which takes a SourceAndConverter
                        registerPopupSourcesAction(
                                (sacs) -> {
                                    // Todo : improve by sending the parameters all over again
                                    //try {
                                    for (SourceAndConverter sac:sacs) {
                                        commandService.run(ci, true, input.getName(), sac);//.get(); TODO understand why get is impossible
                                    }
                                    //} catch (InterruptedException e) {
                                    //    e.printStackTrace();
                                    //} catch (ExecutionException e) {
                                    //    e.printStackTrace();
                                    //}
                                },
                                ci.getTitle());

                        log.accept("Registering action entitled "+ci.getTitle()+" from command "+ci.getClassName());

                    }
                    if (input.getType().equals(SourceAndConverter[].class)) {
                        // It's an action which takes a SourceAndConverter List
                        registerPopupSourcesAction(
                                (sacs) -> {
                                    //try {
                                    commandService.run(ci, true, input.getName(), sacs);//.get();
                                    //} catch (InterruptedException e) {
                                    //    e.printStackTrace();
                                    //} catch (ExecutionException e) {
                                    //    e.printStackTrace();
                                    //}
                                },
                                ci.getTitle());
                        log.accept("Registering action entitled "+ci.getTitle()+" from command "+ci.getClassName());
                    }
                }
            }
        }

    }

    public void registerPopupActions() {
        this.registerPopupSourcesAction((srcs) -> {
            for (SourceAndConverter src:srcs){
                System.out.println(src.getSpimSource().getName());
            }}, "Display names");
        // Bdv add and remove
        registerScijavaCommand(BdvSourcesAdderCommand.class);
        registerScijavaCommand(BdvSourcesRemoverCommand.class);
        this.getUI().addPopupLine();
        // Display
        registerScijavaCommand(SourcesInvisibleMakerCommand.class);
        registerScijavaCommand(SourcesVisibleMakerCommand.class);
        registerScijavaCommand(BrightnessAdjusterCommand.class);
        registerScijavaCommand(SourceColorChangerCommand.class);
        registerScijavaCommand(SacProjectionModeChangerCommand.class);
        this.getUI().addPopupLine();
        // Create new sources
        registerScijavaCommand(SourcesDuplicatorCommand.class);
        registerScijavaCommand(ManualTransformCommand.class);
        registerScijavaCommand(TransformedSourceWrapperCommand.class);
        registerScijavaCommand(SourcesResamplerCommand.class);
        registerScijavaCommand(ColorSourceCreatorCommand.class);
        registerScijavaCommand(LUTSourceCreatorCommand.class);
        this.getUI().addPopupLine();
        // Export and remove
        registerScijavaCommand(SourcesRemoverCommand.class);
        registerScijavaCommand(XmlHDF5ExporterCommand.class);

    }

   public class SpimDataInfo {

        public AbstractSpimData asd;
        public int setupId;

        public SpimDataInfo(AbstractSpimData asd, int setupId) {
            this.asd = asd;
            this.setupId =setupId;
        }
   }

}
