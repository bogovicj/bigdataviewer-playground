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
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.ArrayList;
import java.util.function.Supplier;

public class BdvCreator implements Runnable, Supplier<BigDataViewer>
{
	//private BdvOptions bdvOptions;
	private boolean interpolate;
	private BigDataViewer bdvHandle;

	/*
	public BdvCreator( )
	{
		this.bdvOptions = BdvOptions.options();
		this.interpolate = false;
	}

	public BdvCreator( BdvOptions bdvOptions  )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = false;
	}*/

	public BdvCreator(  boolean interpolate )
	{
		this.interpolate = interpolate;
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
		ConverterSetup cs = SourceAndConverterUtils.createConverterSetup(sac, () -> {});
		csList.add(cs);

		bdvHandle = BigDataViewer.open(csList,sacList,1,new CacheControl.Dummy(),"Title",new ProgressWriterConsole(), ViewerOptions.options());

		cs.setupChangeListeners().add((converterSetup) -> {});//bdvHandle.getViewer().requestRepaint());
		//bdvHandle.getViewer().state().re
		//bss.removeFromBdv();
	}

	public BigDataViewer get()
	{
		if ( bdvHandle == null ) run();

		return bdvHandle;
	}
}
