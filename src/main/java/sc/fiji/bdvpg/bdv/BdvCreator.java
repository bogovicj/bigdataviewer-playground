package sc.fiji.bdvpg.bdv;

import bdv.BigDataViewer;
import bdv.cache.CacheControl;
import bdv.export.ProgressWriterConsole;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.*;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.ArrayList;
import java.util.function.Supplier;

public class BdvCreator implements Runnable, Supplier<BigDataViewer>
{
	private ViewerOptions bdvOptions;
	private boolean interpolate;
	private BigDataViewer bdvHandle;

	public BdvCreator( ViewerOptions bdvOptions, boolean interpolate  )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = interpolate;
	}

	public BdvCreator( ViewerOptions bdvOptions  )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = false;
	}

	public BdvCreator(  boolean interpolate )
	{
		this.bdvOptions = ViewerOptions.options();
		this.interpolate = interpolate;
	}

	public BdvCreator( )
	{
		this.bdvOptions = ViewerOptions.options();
		this.interpolate = false;
	}

	@Override
	public void run()
	{
		createEmptyBdv();
	}

	/**
	 * Hack: add an image and remove it after the
	 * bdvHandle has been created.
	 */
	private void createEmptyBdv()
	{
		/*ArrayImg dummyImg = ArrayImgs.bytes(2, 2, 2);

		bdvOptions = bdvOptions.sourceTransform( new AffineTransform3D() );

		BdvStackSource bss = BdvFunctions.show( dummyImg, "dummy", bdvOptions );

		bdvHandle = bss.getBdvHandle();

		if ( interpolate ) bdvHandle.getViewerPanel().setInterpolation( Interpolation.NLINEAR );*/

		ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
		RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
		// Adds a third dimension because Bdv needs 3D
		rai = Views.addDimension( rai, 0, 0 );

		// Makes Bdv Source
		Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");
		SourceAndConverter sac = SourceAndConverterUtils.createSourceAndConverter(source);


		ArrayList<SourceAndConverter<?>> sacList = new ArrayList<>();
		sacList.add(sac);

		ArrayList<ConverterSetup> csList = new ArrayList<>();
		//ConverterSetup cs = SourceAndConverterUtils.createConverterSetup(sac);//, () -> {});
		//csList.add(cs);

		final ConverterSetup cs = BigDataViewer.createConverterSetup( sac, 0 ); // Why is setupid necessary ?
		csList.add(cs);

		bdvHandle = BigDataViewer.open(csList,sacList,1,new CacheControl.Dummy(),"Title",new ProgressWriterConsole(), bdvOptions);

		bdvHandle.getViewer().state().setDisplayMode(DisplayMode.FUSED);

		if (interpolate) {
			bdvHandle.getViewer().state().setInterpolation(Interpolation.NLINEAR);
		}

		// Let's get an empty BigDataViewer Window!
		// BROKEN FOR THE MOMENT
		// bdvHandle.getViewer().state().removeSource(sac);

	}

	public BigDataViewer get()
	{
		if ( bdvHandle == null ) run();

		return bdvHandle;
	}
}
