package sc.fiji.bdvpg.bdv.navigate;

import bdv.BigDataViewer;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.log.Logger;
import sc.fiji.bdvpg.log.Logs;
import sc.fiji.bdvpg.log.SystemLogger;

public class ViewerTransformLogger implements Runnable
{
	private final BigDataViewer bdvHandle;
	private final Logger logger;

	public ViewerTransformLogger( BigDataViewer bdvHandle )
	{
		this( bdvHandle, new SystemLogger() );
	}

	public ViewerTransformLogger(BigDataViewer bdvHandle, Logger logger )
	{
		this.bdvHandle = bdvHandle;
		this.logger = logger;
	}

	@Override
	public void run()
	{
		final AffineTransform3D view = new AffineTransform3D();
		bdvHandle.getViewer().state().getViewerTransform( view );
		logger.out( Logs.BDV + ": Viewer Transform: " + view.toString() );
	}
}
