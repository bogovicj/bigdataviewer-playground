package sc.fiji.bdvpg.bdv.projector;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static sc.fiji.bdvpg.bdv.projector.Projection.*;

public class AccumulateMixedProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{
	private final int[] projectionModes;
	private final int[] projectionLayerIndex;
	private int[] sourceOrder;
	SourceAndConverter[] sacs;

	public AccumulateMixedProjectorARGB(
			BdvHandle bdvHandle,
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< Source< ? > > sources,
			final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		super( sourceProjectors, sourceScreenImages, target, numThreads, executorService );
		//this.projectionModes = getProjectionModes( bdvHandle, sources );
		//this.projectionLayer = getProjectionLayer( bdvHandle, sources );
		//setSourceOrderAccordingToExclusiveness();

		sacs = getSourceAndConverters(bdvHandle, sources);

		projectionModes = new int[sacs.length];
		projectionLayerIndex = new int[sacs.length];
		sourceOrder = new int[sacs.length];

		for (int i=0;i<sacs.length;i++) {
			projectionModes[i] = (int)
					SourceAndConverterServices
							.getSourceAndConverterService()
							.getMetadata(sacs[i], PROJECTION_MODE);
			projectionLayerIndex[i] = (int)
					SourceAndConverterServices
							.getSourceAndConverterService()
							.getMetadata(sacs[i], PROJECTION_LAYER);
		}

		setSourceComputation();

	}

	public void setSourceComputation()
	{
		/*boolean containsExclusiveProjectionMode = false;
		for ( int projectionMode : projectionModes )
		{
			if ( projectionMode.contains( Projection.PROJECTION_BELONGS_TO_OCCLUDING_LAYER) )
			{
				containsExclusiveProjectionMode = true;
				break;
			}
		}

		final int numSources = projectionModes.length;

		sourceOrder = new int[ numSources ];
		if ( containsExclusiveProjectionMode )
		{
			int j = 0;

			// first the exclusive ones
			for ( int i = 0; i < numSources; i++ )
				if ( projectionModes[ i ].contains( Projection.PROJECTION_BELONGS_TO_OCCLUDING_LAYER) )
					sourceOrder[ j++ ] = i;

			// then the others
			for ( int i = 0; i < numSources; i++ )
				if ( ! projectionModes[ i ].contains( Projection.PROJECTION_BELONGS_TO_OCCLUDING_LAYER) )
					sourceOrder[ j++ ] = i;
		}
		else*/
		{
			for ( int i = 0; i < sacs.length; i++ )
				sourceOrder[ i ] = i;
		}
	}

	@Override
	protected void accumulate(
			final Cursor< ? extends ARGBType >[] accesses,
			final ARGBType target )
	{
		int aAvg = 0, rAvg = 0, gAvg = 0, bAvg = 0, n = 0;
		int aAccu = 0, rAccu = 0, gAccu = 0, bAccu = 0;

		boolean skipNonExclusiveSources = false;

		for ( int sourceIndex : sourceOrder )
		{
			final int argb = accesses[ sourceIndex ].get().get();
			final int a = ARGBType.alpha( argb );
			final int r = ARGBType.red( argb );
			final int g = ARGBType.green( argb );
			final int b = ARGBType.blue( argb );

			if ( a == 0 ) continue;

			final boolean isExclusive = false;//projectionModes[ sourceIndex ].contains(PROJECTION_BELONGS_TO_OCCLUDING_LAYER);

			if ( a != 0 && isExclusive ) skipNonExclusiveSources = true;

			if ( skipNonExclusiveSources && ! isExclusive ) continue;

			if ( true ) //projectionModes[ sourceIndex ].contains( Projection.PROJECTION_MODE_SUM ) )
			{
				aAccu += a;
				rAccu += r;
				gAccu += g;
				bAccu += b;
			}
			else if ( false ) //projectionModes[ sourceIndex ].contains( Projection.PROJECTION_MODE_AVG ) )
			{
				aAvg += a;
				rAvg += r;
				gAvg += g;
				bAvg += b;
				n++;
			}

		}

		if ( n > 0 )
		{
			aAvg /= n;
			rAvg /= n;
			gAvg /= n;
			bAvg /= n;
		}

		aAccu += aAvg;
		rAccu += rAvg;
		gAccu += gAvg;
		bAccu += bAvg;

		if ( aAccu > 255 )
			aAccu = 255;
		if ( rAccu > 255 )
			rAccu = 255;
		if ( gAccu > 255 )
			gAccu = 255;
		if ( bAccu > 255 )
			bAccu = 255;

		target.set( ARGBType.rgba( rAccu, gAccu, bAccu, aAccu ) );
	}

	private SourceAndConverter[] getSourceAndConverters( BdvHandle bdvHandle, ArrayList< Source< ? > > sources )
	{
		// We need to reconstitute the sequence of action that lead to the current indexes

		// Getting the sources present in the BdvHandle
		List<SourceAndConverter> sacsInBdvHandle = SourceAndConverterServices
				.getSourceAndConverterDisplayService()
				.getSourceAndConverterOf(bdvHandle);

		// Fetching the indexes of visible sources in the BdvHandle
		List<Integer> visibleIndexes = bdvHandle.getViewerPanel().getState().getVisibleSourceIndices();
		// In ascending order
		Collections.sort(visibleIndexes);

		SourceAndConverter[] sacArray = new SourceAndConverter[visibleIndexes.size()];

		for (int idx = 0; idx<visibleIndexes.size(); idx++) {
			sacArray[idx] = sacsInBdvHandle.get(visibleIndexes.get(idx));
		}

		return sacArray;

		/*final List< SourceAndConverter > sacs = Arrays.asList(sacArray);//SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters();
		final String[] projectionModes = new String[ sources.size() ];

		int sourceIndex = 0;

		for ( SourceAndConverter<?> sac : sacs )
		{

			final String projectionMode = (String) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sac, PROJECTION_MODE );

			if ( projectionMode == null ) {
				projectionModes[sourceIndex++] = PROJECTION_MODE_SUM;
			} else {
				projectionModes[sourceIndex++] = projectionMode;
			}

		}

		return projectionModes;*/
	}

}
