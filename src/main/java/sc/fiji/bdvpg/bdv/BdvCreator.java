package sc.fiji.bdvpg.bdv;

import bdv.BigDataViewer;
import bdv.viewer.Interpolation;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;

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

		bdvHandle = BigDataViewer.open(null,null,1,null,"Title",null,null);

		//bdvHandle.getViewer().state().re
		//bss.removeFromBdv();
	}

	public BigDataViewer get()
	{
		if ( bdvHandle == null ) run();

		return bdvHandle;
	}
}
