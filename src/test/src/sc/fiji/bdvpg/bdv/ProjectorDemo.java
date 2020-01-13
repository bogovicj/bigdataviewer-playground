package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.spimdata.importer.SpimDataOpener;

/**
 * Demonstrates average projection of two sources.
 *
 * TODO: make projection mode optional or dependent on some sourceandconverter metadata.
 */
public class ProjectorDemo
{
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		BdvService.InitScijavaServices();

		// Gets active BdvHandle instance
		BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

		// Import SpimData object
		SpimDataOpener sdix = new SpimDataOpener("src/test/resources/mri-stack.xml");

		AbstractSpimData asd = sdix.get();

		// Register to the sourceandconverter service
		BdvService.getSourceService().register(asd);


		BdvService.getSourceService().getSourceAndConverterFromSpimdata(asd).forEach(source -> {
			BdvService.getSourceDisplayService().show(bdvHandle, source);

			new ViewerTransformAdjuster(bdvHandle, source).run();
			new BrightnessAutoAdjuster(source, 0).run();
		});

		// Import SpimData object
		sdix = new SpimDataOpener("src/test/resources/mri-stack-shiftedX.xml");

		asd = sdix.get();

		// Register to the sourceandconverter service
		BdvService.getSourceService().register(asd);

		BdvService.getSourceService().getSourceAndConverterFromSpimdata(asd).forEach(source -> {
			BdvService.getSourceDisplayService().show(bdvHandle, source);

			new ViewerTransformAdjuster(bdvHandle, source).run();
			new BrightnessAutoAdjuster(source, 0).run();
		});


	}

	/*public static BdvHandle createBdv()
	{
		// specify average projector factory
		final BdvOptions bdvOptions = new BdvOptions()
				.accumulateProjectorFactory( AccumulateAverageProjectorARGB.factory );

		final BdvCreator bdvCreator = new BdvCreator( bdvOptions );
		bdvCreator.run();
		return bdvCreator.get();
	}*/
}
