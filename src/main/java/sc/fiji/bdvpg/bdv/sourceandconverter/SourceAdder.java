package sc.fiji.bdvpg.bdv.sourceandconverter;

import bdv.BigDataViewer;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Consumer;

public class SourceAdder implements Runnable, Consumer<SourceAndConverter>
{
	SourceAndConverter srcIn;
	BigDataViewer bdvh;

	public SourceAdder(BigDataViewer bdvh, SourceAndConverter srcIn) {
		this.srcIn=srcIn;
		this.bdvh=bdvh;
	}

	public void run() {
		accept(srcIn);
	}

	@Override
	public void accept(SourceAndConverter source) {
		SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvh, source);
	}
}
