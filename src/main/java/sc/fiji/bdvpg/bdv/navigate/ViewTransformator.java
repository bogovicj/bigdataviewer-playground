package sc.fiji.bdvpg.bdv.navigate;

import bdv.BigDataViewer;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * ViewTransformator
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class ViewTransformator implements Runnable {

    private BigDataViewer bdvHandle;
    private AffineTransform3D transform;

    public ViewTransformator(BigDataViewer bdvHandle, AffineTransform3D transform) {
        this.bdvHandle = bdvHandle;
        this.transform = transform;
    }

    @Override
    public void run() {
        // get current transform
        AffineTransform3D view = new AffineTransform3D();
        bdvHandle.getViewer().state().getViewerTransform(view);

        // change the transform
        view = view.concatenate(transform);

        // submit to BDV
        bdvHandle.getViewer().setCurrentViewerTransform(view);

    }
}
