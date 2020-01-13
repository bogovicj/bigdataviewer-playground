package sc.fiji.bdvpg.services;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Service which centralizes Bdv Sources, independently of their display
 * Bdv Sources can be registered to this Service.
 * This service stores Sources but on top of it,
 * It contains a Map which contains any object which can be linked to the sourceandconverter.
 *
 * Objects needed for display should be created by a  IBdvSourceAndConverterDisplayService
 * - Converter to ARGBType, ConverterSetup, and Volatile view
 *
 * TODO : Think more carefully : maybe the volatile sourceandconverter should be done here...
 * Because when multiply wrapped sourceandconverter end up here, it maybe isn't possible to make the volatile view
 */

public interface IBdvSourceAndConverterService {

    /**
     * Test if a Source is already registered in the Service
     * @param src
     * @return
     */
    boolean isRegistered(SourceAndConverter src);

    /**
     * Register a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    void register(SourceAndConverter src);

    /**
     * Registers all sources contained with a SpimData Object in this Service.
     * Called in the BdvSourcePostProcessor
     */
    void register(AbstractSpimData asd);

    /**
     * @return list of all registered sources
     */
    List<SourceAndConverter> getSourceAndConverters();

    /**
     * Return sources assigned to a SpimDataObject
     */
    List<SourceAndConverter> getSourceAndConverterFromSpimdata(AbstractSpimData asd);

    /**
     * Removes a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    void remove(SourceAndConverter src);


    void linkToSpimData(SourceAndConverter src, AbstractSpimData asd);

    /**
     * Gets lists of associated objects and data attached to a Bdv Source
     * @return
     */
    Map<SourceAndConverter, Map<String, Object>> getAttachedSourceAndConverterData();

    /**
     * If supported, this will allow to display actions in a popup window in a ui
     * @param action
     * @param actionName
     */
    void registerPopupSourcesAction(Consumer<SourceAndConverter[]> action, String actionName);
}
