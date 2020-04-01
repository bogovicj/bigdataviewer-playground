package spimdata.util;

import bdv.img.WarpedSource;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.registration.ViewTransformGeneric;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import org.jdom2.Element;

public class BigWarpViewTransform implements ViewTransformGeneric {

    public BigWarpViewTransform() {
        WarpedSource ws;
    }

    @Override
    public void init(Element elem) {
        // Do nothing yet
    }

    @Override
    public Element toXml() {
        return XmlHelpers.affineTransform3DElement("affine", new AffineTransform3D());
    }

    @Override
    public boolean hasName() {
        return true;
    }

    @Override
    public String getName() {
        return "WarpTest";
    }

    @Override
    public AffineGet asAffine3D() {
        return new AffineTransform3D();
    }
}
