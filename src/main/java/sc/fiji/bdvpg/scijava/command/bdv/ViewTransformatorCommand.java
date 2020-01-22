package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.BigDataViewer;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewTransformator;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

/**
 * ViewTransformLoggerCommand
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Change view transform")
public class ViewTransformatorCommand implements Command {

    @Parameter
    BigDataViewer bdvh;

    @Parameter
    public Double translateX = 0.0;
    @Parameter
    public Double translateY = 0.0;
    @Parameter
    public Double translateZ = 0.0;

    @Parameter
    public Double rotateAroundX = 0.0;
    @Parameter
    public Double rotateAroundY = 0.0;
    @Parameter
    public Double rotateAroundZ = 0.0;

    @Override
    public void run() {
        AffineTransform3D affineTransform3D = new AffineTransform3D();
        affineTransform3D.translate(translateX, translateY, translateZ);
        affineTransform3D.rotate(0, rotateAroundX);
        affineTransform3D.rotate(1, rotateAroundY);
        affineTransform3D.rotate(2, rotateAroundZ);

        new ViewTransformator(bdvh, affineTransform3D).run();
    }
}
