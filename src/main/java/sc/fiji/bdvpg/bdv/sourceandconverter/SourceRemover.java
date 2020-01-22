package sc.fiji.bdvpg.bdv.sourceandconverter;

import bdv.BigDataViewer;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Consumer;

public class SourceRemover implements Runnable, Consumer<SourceAndConverter>
{
	SourceAndConverter srcIn;
	BigDataViewer bdvh;

	public SourceRemover(BigDataViewer bdvh, SourceAndConverter srcIn) {
		this.srcIn=srcIn;
		this.bdvh=bdvh;
	}

	public SourceRemover(SourceAndConverter srcIn) {
		this.srcIn=srcIn;
		this.bdvh=null;
	}

	public SourceRemover() {
		this.srcIn=null;
		this.bdvh=null;
	}

	public void run() {
		accept(srcIn);
	}

	@Override
	public void accept(SourceAndConverter source) {
		if (bdvh==null) {
			// Remove from all displays
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs(source);
		} else {
			// Remove from a specific bdvHandle
			SourceAndConverterServices.getSourceAndConverterDisplayService().remove(bdvh, source);
		}
	}
}
