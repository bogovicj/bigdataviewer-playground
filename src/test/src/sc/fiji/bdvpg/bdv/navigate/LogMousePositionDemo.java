package sc.fiji.bdvpg.bdv.navigate;

import bdv.BigDataViewer;
//import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imagej.ImageJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

/**
 * ViewTransformSetAndLogDemo
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class LogMousePositionDemo {
    public static void main(String... args) {

        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because Bdv needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes Bdv Source
        Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");
        SourceAndConverter sac = SourceAndConverterUtils.createSourceAndConverter(source);

        // Creates a BdvHandle
        BigDataViewer bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        // Show the sourceandconverter
        SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac);

        // Adjust Bdv View on the sourceandconverter
        new ViewerTransformAdjuster(bdvHandle, sac).run();

        // add a click behavior for logging mouse positions
        new ClickBehaviourInstaller( bdvHandle, (x, y ) -> new PositionLogger( bdvHandle ).run() ).install( "Log mouse position", "ctrl D" );

        // log the current position
        new PositionLogger( bdvHandle ).run();

    }

}
