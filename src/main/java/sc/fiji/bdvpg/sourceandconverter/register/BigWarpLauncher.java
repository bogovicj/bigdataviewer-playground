package sc.fiji.bdvpg.sourceandconverter.register;

import bdv.tools.brightness.ConverterSetup;
import bdv.BigDataViewer;
//import bdv.util.ViewerPanelHandle;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import mpicbg.spim.data.SpimDataException;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Can launch BigWarp with:
 * - Two lists of SourceAndConverter
 *
 * Output:
 * - The two BdvHandle
 *
 * Limitation :
 * - Cache is null in BigWarpInit
 *
 * In order to retrieve the transform, TODO
 */

public class BigWarpLauncher implements Runnable {

    BigWarp.BigWarpData<?> bwData;

    BigWarp<?> bigWarp;

    String bigWarpName;

    BigDataViewer bdvHandleP;

    BigDataViewer bdvHandleQ;

    // Issue with constructor :
    // Making a constructor with lists of SourceAndConverters:
    // public BigWarpLauncher(List<SourceAndConverter> movingSources, List<SourceAndConverter> fixedSources)
    // And a constructor with lists of Source:
    // public BigWarpLauncher(List<Source> movingSources, List<Source> fixedSources)
    // Do not work because constructors have same 'erasure'
    // Option choosen -> a single constructor which checks the types of their inner objects
    // Alternative maybe better option :
    // Use array : Source[] or SourceAndConverter[] (and maybe this issue was the reason for BigWarp choosing this in the beginning)

    List<SourceAndConverter> movingSources;
    List<SourceAndConverter> fixedSources;

    public BigWarpLauncher(List<SourceAndConverter> movingSources, List<SourceAndConverter> fixedSources, String bigWarpName, List<ConverterSetup> allConverterSetups) {

        this.movingSources = movingSources;
        this.fixedSources = fixedSources;

        this.bigWarpName = bigWarpName;

            List<SourceAndConverter> allSources = new ArrayList<>();
            allSources.addAll(movingSources);
            allSources.addAll(fixedSources);

            int[] mvSrcIndices = new int[movingSources.size()];
            for (int i = 0; i < movingSources.size(); i++) {
                mvSrcIndices[i] = i;
            }

            int[] fxSrcIndices = new int[fixedSources.size()];
            for (int i = 0; i < fixedSources.size(); i++) {
                fxSrcIndices[i] = i+movingSources.size();
            }

            if (allConverterSetups==null) {
                allConverterSetups = new ArrayList<>();
            }

            bwData = new BigWarp.BigWarpData(allSources, allConverterSetups, null, mvSrcIndices, fxSrcIndices);

    }

    @Override
    public void run() {
        try {
            bigWarp = new BigWarp(bwData, bigWarpName, null);
            // TODO : fix
            /*
            // What does P and Q stand for ? Not sure about who's moving and who's fixed
            bdvHandleP = new ViewerPanelHandle(bigWarp.getViewerFrameP().getViewerPanel(), bigWarp.getSetupAssignments(), bigWarpName+"_Moving");
            bdvHandleQ = new ViewerPanelHandle(bigWarp.getViewerFrameQ().getViewerPanel(), bigWarp.getSetupAssignments(), bigWarpName+"_Fixed");

            SourceAndConverter[] warpedSources = new SourceAndConverter[movingSources.size()];

            for (int i=0;i<warpedSources.length;i++) {
                warpedSources[i] = bdvHandleP.getViewerPanel().getState().getSources().get(i);
            }

            int nSources = bdvHandleP.getViewerPanel().getState().numSources();
            SourceAndConverter gridSource = bdvHandleP.getViewerPanel().getState().getSources().get(nSources-1);
            SourceAndConverter warpMagnitudeSource = bdvHandleP.getViewerPanel().getState().getSources().get(nSources-2);

            SourceAndConverterServices.getSourceAndConverterService().register(gridSource);
            SourceAndConverterServices.getSourceAndConverterService().register(warpMagnitudeSource);
            for (SourceAndConverter sac : warpedSources) {
                SourceAndConverterServices.getSourceAndConverterService().register(sac);
            }
            */
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

    // Outputs

    public BigDataViewer getBdvHandleP() {
        return bdvHandleP;
    }

    public BigDataViewer getBdvHandleQ() {
        return bdvHandleQ;
    }

    public BigWarp getBigWarp() {
        return bigWarp;
    }

}
