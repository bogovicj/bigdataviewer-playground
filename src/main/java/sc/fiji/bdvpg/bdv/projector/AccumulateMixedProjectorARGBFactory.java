package sc.fiji.bdvpg.bdv.projector;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class AccumulateMixedProjectorARGBFactory implements AccumulateProjectorFactory< ARGBType >
{

	public AccumulateMixedProjectorARGBFactory()
	{
	}

	public  VolatileProjector createProjector(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< SourceAndConverter< ? > > sources,
			final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > targetScreenImage,
			final int numThreads,
			final ExecutorService executorService )
	{
		return new AccumulateMixedProjectorARGB(
				sourceProjectors,
				sources,
				sourceScreenImages,
				targetScreenImage,
				numThreads,
				executorService );
	}

}
