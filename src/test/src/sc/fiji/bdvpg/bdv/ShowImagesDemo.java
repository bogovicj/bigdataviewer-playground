package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.spimdata.importer.SpimDataOpener;

/**
 *
 */
public class ShowImagesDemo
{
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		BdvService.InitScijavaServices();

		// Gets active BdvHandle instance
		BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

		// Open images
		new SpimDataOpener("src/test/resources/mri-stack.xml").run();
		new SpimDataOpener("src/test/resources/mri-stack-shiftedX.xml").run();

		// Show and adjust ViewerTransform and brightness
		BdvService.getSourceService().getSourceAndConverters().forEach( sac -> {
			BdvService.getSourceDisplayService().show(bdvHandle, sac);
			new ViewerTransformAdjuster(bdvHandle, sac).run();
			new BrightnessAutoAdjuster(sac, 0).run();
		});
	}
}
