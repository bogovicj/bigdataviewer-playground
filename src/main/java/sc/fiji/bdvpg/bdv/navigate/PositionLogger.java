package sc.fiji.bdvpg.bdv.navigate;

import bdv.BigDataViewer;
import net.imglib2.RealPoint;
import sc.fiji.bdvpg.log.Logger;
import sc.fiji.bdvpg.log.Logs;
import sc.fiji.bdvpg.log.SystemLogger;

public class PositionLogger implements Runnable
{
	private final BigDataViewer bdvHandle;
	private final Logger logger;

	public PositionLogger( BigDataViewer bdvHandle )
	{
		this( bdvHandle, new SystemLogger() );
	}

	public PositionLogger(BigDataViewer bdvHandle, Logger logger )
	{
		this.bdvHandle = bdvHandle;
		this.logger = logger;
	}

	@Override
	public void run()
	{
		final RealPoint realPoint = new RealPoint( 3 );
		bdvHandle.getViewer().getGlobalMouseCoordinates( realPoint );
		logger.out( Logs.BDV + ": Position at Mouse: " + realPoint.toString() );
	}
}
