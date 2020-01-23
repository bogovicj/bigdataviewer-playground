package sc.fiji.bdvpg.scijava.services;

import bdv.tools.brightness.ConverterSetup;
import bdv.BigDataViewer;
import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.util.Pair;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Scijava Service which handles the Display of Bdv SourceAndConverters in one or multiple Bdv Windows
 * Pairs with BdvSourceAndConverterService, but this service is optional
 *
 * Handling multiple Sources displayed in potentially multiple Bdv Windows
 * Make its best to keep in synchronizations all of this, without creating errors nor memory leaks
 */

@Plugin(type= Service.class)
public class SourceAndConverterBdvDisplayService extends AbstractService implements SciJavaService  {

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println( SourceAndConverterBdvDisplayService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println( SourceAndConverterBdvDisplayService.class.getSimpleName()+":"+str);

    public static String CONVERTER_SETUP = "ConverterSetup";


    /**
     * Used to add Aliases for BdvHandle objects
     **/
    @Parameter
    ScriptService scriptService;

    /**
     * Service containing all registered Bdv Sources
     **/
    @Parameter
    SourceAndConverterService bdvSourceAndConverterService;

    /**
     * Used to create Bdv Windows when necessary
     **/
    @Parameter
    CommandService cs;

    /**
     * Used to retrieved the last active Bdv Windows (if the activated callback has been set right)
     **/
    @Parameter
    GuavaWeakCacheService cacheService;
    @Parameter
    ObjectService os;

    /**
     * Map linking a Source to the different locations where it's been displayed
     * BdvHandles displaying Source
     **/
    Map<SourceAndConverter, Set<BigDataViewer>> sacToBdv;

    public BigDataViewer getNewBdv() {
        try
        {
            return (BigDataViewer)
                    cs.run(BdvWindowCreatorCommand.class,
                            true,
                            "is2D", false,
                            "windowTitle", "Bdv").get().getOutput("bdvh");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the last active Bdv or create a new one
     */
    public BigDataViewer getActiveBdv() {
        List<BigDataViewer> bdvhs = os.getObjects(BigDataViewer.class);
        if ((bdvhs == null)||(bdvhs.size()==0)) {
            return getNewBdv();
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

    /**
     * Displays a Source, the last active bdvh is chosen since none is specified in this method
     * @param sacs
     */
    public void show(SourceAndConverter... sacs) {
         show(getActiveBdv(), sacs);
    }

    /**
     * Makes visible a source, makes it visible in all bdvs according to BdvhReferences
     * @param sac
     */
    public void makeVisible(SourceAndConverter sac) {
        if ( sacToBdv.get(sac)!=null)
            sacToBdv.get(sac).forEach(bdvhr -> bdvhr.getViewer().state().setSourceActive(sac, true));
    }

    /**
     * Makes invisible a source, makes it invisible in all bdvs according to BdvhReferences
     * @param sac
     */
    public void makeInvisible(SourceAndConverter sac) {
        if ( sacToBdv.get(sac)!=null)
            sacToBdv.get(sac).forEach(bdvhr -> bdvhr.getViewer().state().setSourceActive(sac, false));

    }

    /**
     * Displays a Bdv sourceandconverter into the specified BdvHandle
     * This function really is the core of this service
     * It mimicks or copies the functions of BdvVisTools because it is responsible to
     * create converter, volatiles, convertersetups and so on
     * @param sacs
     * @param bdvh
     */
    public void show(BigDataViewer bdvh, SourceAndConverter... sacs) {

        List<SourceAndConverter<?>> sacsToDisplay = new ArrayList<>();
        for (SourceAndConverter sac:sacs) {
            if (!bdvSourceAndConverterService.isRegistered(sac)) {
                bdvSourceAndConverterService.register(sac);
            }

            boolean escape = false;

            if (bdvh.getViewer().state().getSources().contains(sac)) {
                escape = true;
            }

            // Do not display 2 times the same source and converter
            if (sacsToDisplay.contains(sac)) {
                escape = true;
            }

            if (!escape) {
                sacsToDisplay.add(sac);
                bdvh.getConverterSetups().put(sac,getConverterSetup(sac));

                // Store where the sac is display
                if (!(sacToBdv.containsKey(sac))) {
                    log.accept("Create locations display of sourceandconverter of " + sac.getSpimSource().getName());
                    sacToBdv.put(sac, new HashSet<>());
                }
                sacToBdv.get(sac).add(bdvh);
            }
        }

        // Actually display the sources -> repaint called only once!
        bdvh.getViewer().state().addSources(sacsToDisplay);
    }

    /**
     * Removes a sourceandconverter from all BdvHandle displaying this sourceandconverter
     * Updates all references of other Sources present
     * @param sacs
     */
    public void removeFromAllBdvs(SourceAndConverter... sacs) {
        // Filter per Bdv
        Map<BigDataViewer, List<SourceAndConverter>> sourcesToRemovePerBdvHandle = new HashMap<>();

        for (SourceAndConverter sac:sacs) {
            sacToBdv.get(sac).forEach(bdvHandleRef -> {
                if (!sourcesToRemovePerBdvHandle.containsKey(bdvHandleRef)) {
                    sourcesToRemovePerBdvHandle.put(bdvHandleRef, new ArrayList<>());
                }
                sourcesToRemovePerBdvHandle.get(bdvHandleRef).add(sac);
            });
        }

        // Only one call per bdvh
        sourcesToRemovePerBdvHandle.keySet().forEach(bdvHandle -> {
            remove(bdvHandle, sourcesToRemovePerBdvHandle.get(bdvHandle)
                    .toArray(new SourceAndConverter[sourcesToRemovePerBdvHandle.get(bdvHandle).size()]));
        });
    }

    /**
     * Removes a sourceandconverter from the active Bdv
     * Updates all references of other Sources present
     * @param sacs
     */
    public void removeFromActiveBdv(SourceAndConverter... sacs) {
        // This condition avoids creating a window for nothing
        if (os.getObjects(BigDataViewer.class).size()>0) {
            remove(getActiveBdv(), sacs);
        }
    }

    /**
     * Removes a sourceandconverter from a BdvHandle
     * Updates all references of other Sources present
     * @param bdvh
     * @param sacs Array of SourceAndConverter
     */
    public void remove(BigDataViewer bdvh, SourceAndConverter<?>... sacs) {
        bdvh.getViewer().state().removeSources(Arrays.asList(sacs));
        bdvh.getViewer().requestRepaint();
    }

    /**
     * Gets or create the associated ConverterSetup of a Source
     * While several converters can be associated to a Source (volatile and non volatile),
     * only one ConverterSetup is associated to a Source
     * @param sac
     * @return
     */
    public ConverterSetup getConverterSetup(SourceAndConverter sac) {
        if (!bdvSourceAndConverterService.isRegistered(sac)) {
            bdvSourceAndConverterService.register(sac);
        }

        // If no ConverterSetup is built then build it
        if ( bdvSourceAndConverterService.sacToMetadata.get(sac).get( CONVERTER_SETUP )== null) {
            ConverterSetup setup = SourceAndConverterUtils.createConverterSetup(sac);
            bdvSourceAndConverterService.sacToMetadata.get(sac).put( CONVERTER_SETUP,  setup );
        }

        return (ConverterSetup) bdvSourceAndConverterService.sacToMetadata.get(sac).get( CONVERTER_SETUP );
    }

    /**
     * Updates converter and ConverterSetup of a Source, + updates display
     * TODO: This method currently modifies the order of the sources shown in the bdvh window
     * While this is not important for most bdvhandle, this could affect the functionality
     * of BigWarp
     * LIMITATION : Cannot use LUT for ARGBType -> TODO check type and send an error
     * @param source
     * @param cvt
     */
    public void updateConverter(SourceAndConverter source, Converter cvt) {
        errlog.accept("Unsupported operation : a new SourceAndConverterObject should be built. (TODO) ");
        /*
        // Precaution : the sourceandconverter should be registered
        if (!bss.isRegistered(sourceandconverter)) {
            bss.register(sourceandconverter);
        }
        // Step 1 : build the proper objects
        // Build a new ConverterSetup from the converter
        ConverterSetup setup;
        if (cvt instanceof ColorConverter) {
            setup = new ARGBColorConverterSetup((ColorConverter) cvt);
        } else if (cvt instanceof RealLUTConverter) {
            setup = new LUTConverterSetup((RealLUTConverter) cvt);
        } else {
            errlog.accept("Cannot build convertersetup with converter of class "+cvt.getClass().getSimpleName());
            return;
        }

        // Callback when convertersetup is changed
        setup.setViewer(() -> {
            if (locationsDisplayingSource.get(sourceandconverter)!=null) {
                locationsDisplayingSource.get(sourceandconverter).forEach(bhr -> bhr.bdvh.getViewerPanel().requestRepaint());
            }
        });

        // Step 3 : store where the sources were displayed
        Set<BdvHandle> bdvhDisplayingSource= locationsDisplayingSource.get(sourceandconverter)
                .stream()
                .map(bdvHandleRef -> bdvHandleRef.bdvh)
                .collect(Collectors.toSet());

        // Step 4 : remove where the sourceandconverter was displayed
        removeFromAllBdvs(sourceandconverter);

        // Step 5 : updates cached objects
        bss.getAttachedSourceAndConverterData().get(sourceandconverter).put(CONVERTERSETUP, setup);

        // Step 6 : restore sourceandconverter display location
        bdvhDisplayingSource.forEach(bdvh -> show(bdvh, sourceandconverter));
        */
    }

    /**
     * Service initialization
     */
    @Override
    public void initialize() {
        scriptService.addAlias(BigDataViewer.class);
        sacToBdv = new HashMap<>();
        bdvSourceAndConverterService.setDisplayService(this);
        SourceAndConverterServices.setSourceAndConverterDisplayService(this);
        log.accept("Service initialized.");
    }

    /**
     * Closes appropriately a BdvHandle which means that it updates
     * the callbacks for ConverterSetups and updates the ObjectService
     * @param bdvh
     */
    public void closeBdv(BigDataViewer bdvh) {
        // Programmatically or User action
        // Before closing the Bdv Handle, we need to keep up to date all objects:
        // 1 sourcesDisplayedInBdvWindows
        // 2 locationsDisplayingSource
        sacToBdv.values().forEach(list ->
            list.removeIf(bdvhr -> bdvhr.equals(bdvh))
        );
        os.removeObject(bdvh);

        // Fix BigWarp closing issue
        boolean isPaired = pairedBdvs.stream().filter(p -> (p.getA()==bdvh)||(p.getB()==bdvh)).findFirst().isPresent();
        if (isPaired) {
            Pair<BigDataViewer, BigDataViewer> pair = pairedBdvs.stream().filter(p -> (p.getA()==bdvh)||(p.getB()==bdvh)).findFirst().get();
            pairedBdvs.remove(pair);
            if (pair.getA()==bdvh) {
                closeBdv(pair.getB());
            } else {
                closeBdv(pair.getA());
            }
        }
    }

    /**
     * Enables proper closing of Big Warp paired BdvHandles
     */
    List<Pair<BigDataViewer, BigDataViewer>> pairedBdvs = new ArrayList<>();
    public void pairClosing(BigDataViewer bdv1, BigDataViewer bdv2) {
        pairedBdvs.add(new Pair<BigDataViewer, BigDataViewer>() {
            @Override
            public BigDataViewer getA() {
                return bdv1;
            }

            @Override
            public BigDataViewer getB() {
                return bdv2;
            }
        });
    }

    /**
     * Registers a sourceandconverter which has originated from a BdvHandle
     * Useful for BigWarp where the grid and the deformation magnitude sourceandconverter are created
     * into bigwarp
     * @param bdvh_in
     */
    public void registerBdvSources(BigDataViewer bdvh_in) {
        bdvh_in.getViewer().state().getSources().forEach(sac -> {
            bdvSourceAndConverterService.register(sac);
            bdvSourceAndConverterService.sacToMetadata.get(sac).put( CONVERTER_SETUP, bdvh_in.getConverterSetups().getConverterSetup(sac));
        });
    }

    /**
     * For debug purposes, check that all is in sync
     */
    public void logLocationsDisplayingSource() {
        sacToBdv.forEach((sac, bdvHandleRefs) -> {
            log.accept(sac.getSpimSource().getName()+":"+sac.toString());
            bdvHandleRefs.forEach(bdvref -> {
                log.accept("\t bdvh = "+bdvref.toString());
            });
        });
    }

    /**
     * Updates bdvHandles which are displaying at least one of this sacs
     * Potentially improvement is to check whether the timepoint need an update ?
     * @param sacs
     */
    public void updateDisplays(SourceAndConverter... sacs)
    {
        // Two step process : first detect which BdvHandles need to be updated
        Map<BigDataViewer, List<SourceAndConverter>> sourcesToUpdatePerBdvHandle = new HashMap<>();
        for (SourceAndConverter sac:sacs) {
            sacToBdv.get(sac).forEach(bdvHandleRef -> {
                if (!sourcesToUpdatePerBdvHandle.containsKey(bdvHandleRef)) {
                    sourcesToUpdatePerBdvHandle.put(bdvHandleRef, new ArrayList<>());
                }
                sourcesToUpdatePerBdvHandle.get(bdvHandleRef).add(sac);
            });
        }
        // Then update them : there is only one requestRepaint call per bdvh
        sourcesToUpdatePerBdvHandle.keySet().forEach(bdvHandle -> bdvHandle.getViewer().requestRepaint());
    }

    /**
     * Returns the list of sacs held within a BdvHandle ( whether they are visible or not )
     * List is ordered by index in the BdvHandle -> complexification to implement
     * the mixed projector
     * @param bdvHandle
     * @return
     */
    public List<SourceAndConverter<?>> getSourceAndConverterOf(BigDataViewer bdvHandle) {
        return bdvHandle.getViewer().state().getSources();
    }

    /**
     * Returns a List of BdvHandle which are currently displaying a sac
     * Returns an empty set in case the sac is not displayed
     * @param sac
     * @return
     */
    public Set<BigDataViewer> getDisplaysOf(SourceAndConverter sac) {
        if (this.sacToBdv.get(sac)!=null) {
            return this.sacToBdv.get(sac).stream().collect(Collectors.toSet());
        } else {
            return new HashSet<>();
        }
    }

}
