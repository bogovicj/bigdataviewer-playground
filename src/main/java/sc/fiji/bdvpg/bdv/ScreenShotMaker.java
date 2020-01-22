package sc.fiji.bdvpg.bdv;

import bdv.BigDataViewer;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.LUT;
import net.imglib2.*;
import net.imglib2.Cursor;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.converter.lut.Luts;

import java.awt.*;
import java.util.ArrayList;
import java.util.Set;

import static sc.fiji.bdvpg.bdv.BdvUtils.*;

/**
 * ScreenShotMaker
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf, @tischi
 *         December 2019
 */
public class ScreenShotMaker {

    CompositeImage screenShot = null;
    private BigDataViewer bdvHandle;
    private double physicalPixelSpacingInXY = 1;
    private String physicalUnit = "Pixels";
    private boolean sourceInteractionWithViewerPlaneOnly2D = true;


    public ScreenShotMaker(BigDataViewer bdvHandle) {
        this.bdvHandle = bdvHandle;
    }

    public void setPhysicalPixelSpacingInXY(double spacing, String unit) {
        screenShot = null;
        this.physicalPixelSpacingInXY = spacing;
        this.physicalUnit = unit;
    }

    public void setSourceInteractionWithViewerPlaneOnly2D(boolean sourceInteractionWithViewerPlaneOnly2D) {
        screenShot = null;
        this.sourceInteractionWithViewerPlaneOnly2D = sourceInteractionWithViewerPlaneOnly2D;
    }

    private void process() {
        if (screenShot != null) {
            return;
        }
        screenShot = captureView(bdvHandle, physicalPixelSpacingInXY, physicalUnit, sourceInteractionWithViewerPlaneOnly2D);
    }

    public ImagePlus getScreenshot() {
        process();
        return screenShot;
    }

    private static < R extends RealType< R >> CompositeImage captureView(
            BigDataViewer bdv,
            double outputVoxelSpacing,
            String voxelUnits,
            boolean checkSourceIntersectionWithViewerPlaneOnlyIn2D )
    {
        final AffineTransform3D viewerTransform = new AffineTransform3D();
        bdv.getViewer().state().getViewerTransform( viewerTransform );

        final double viewerVoxelSpacing = getViewerVoxelSpacing( bdv );

        double dxy = outputVoxelSpacing / viewerVoxelSpacing;

        final int w = bdv.getViewer().getWidth();
        final int h = bdv.getViewer().getHeight();

        final long captureWidth = ( long ) Math.ceil( w / dxy );
        final long captureHeight = ( long ) Math.ceil( h / dxy );

        // TODO: Maybe capture as ARGBType images?
        final ArrayList<RandomAccessibleInterval<UnsignedShortType>> captures = new ArrayList<>();
        final ArrayList<ARGBType> colors = new ArrayList<>();
        final ArrayList< Boolean > isSegmentations = new ArrayList<>();
        final ArrayList< double[] > displayRanges = new ArrayList<>();

        //final List< Integer > sourceIndices = getVisibleSourceIndices( bdv );

        final int t = bdv.getViewer().state().getCurrentTimepoint();

        Set<SourceAndConverter<?>> sacs = bdv.getViewer().state().getVisibleAndPresentSources();

        for ( SourceAndConverter sac : sacs )
        {
            if ( ! isSourceIntersectingCurrentView( bdv, t, sac, checkSourceIntersectionWithViewerPlaneOnlyIn2D ) ) continue;

            final RandomAccessibleInterval< UnsignedShortType > capture
                    = ArrayImgs.unsignedShorts( captureWidth, captureHeight );

            Source< ? > source = sac.getSpimSource();

            final int level = getLevel( source, outputVoxelSpacing );
            final AffineTransform3D sourceTransform =
                    getSourceTransform( source, t, level );

            AffineTransform3D viewerToSourceTransform = new AffineTransform3D();

            viewerToSourceTransform.preConcatenate( viewerTransform.inverse() );
            viewerToSourceTransform.preConcatenate( sourceTransform.inverse() );

            isSegmentations.add( false );

            Grids.collectAllContainedIntervals(
                    Intervals.dimensionsAsLongArray( capture ),
                    new int[]{100, 100}).parallelStream().forEach( interval ->
            {
                RealRandomAccess< ? extends RealType< ? > > sourceAccess =
                        getInterpolatedRealRandomAccess( t, source, level, true );

                final IntervalView< UnsignedShortType > crop = Views.interval( capture, interval );
                final Cursor< UnsignedShortType > captureCursor = Views.iterable( crop ).localizingCursor();
                final RandomAccess< UnsignedShortType > captureAccess = crop.randomAccess();

                final double[] canvasPosition = new double[ 3 ];
                final double[] sourceRealPosition = new double[ 3 ];

                while ( captureCursor.hasNext() )
                {
                    captureCursor.fwd();
                    captureCursor.localize( canvasPosition );
                    captureAccess.setPosition( captureCursor );
                    canvasPosition[ 0 ] *= dxy;
                    canvasPosition[ 1 ] *= dxy;
                    viewerToSourceTransform.apply( canvasPosition, sourceRealPosition );
                    sourceAccess.setPosition( sourceRealPosition );
                    captureAccess.get().setReal( sourceAccess.get().getRealDouble() );
                }
            });

            captures.add( capture );
            colors.add( new ARGBType(ARGBType.rgba(255,255,255,0)));// TODO fix getSourceColor( bdv, sourceIndex ) );
            displayRanges.add( new double[]{ 0, 255 });// TODO fix getDisplayRange( bdv, sourceIndex) );
        }

        final double[] captureVoxelSpacing = getCaptureVoxelSpacing( outputVoxelSpacing, viewerVoxelSpacing );

        if ( captures.size() > 0 )
            return asCompositeImage( captureVoxelSpacing, voxelUnits, captures, colors, displayRanges, isSegmentations );
        else
            return null;
    }

    private static double[] getCaptureVoxelSpacing( double outputVoxelSpacing, double viewerVoxelSpacing )
    {
        final double[] captureVoxelSpacing = new double[ 3 ];
        for ( int d = 0; d < 2; d++ )
            captureVoxelSpacing[ d ] = outputVoxelSpacing;

        captureVoxelSpacing[ 2 ] = viewerVoxelSpacing; // TODO: makes sense?
        return captureVoxelSpacing;
    }

    public static RealRandomAccess< ? extends RealType< ? > >
    getInterpolatedRealRandomAccess( int t, Source< ? > source, int level, boolean interpolate )
    {
        if ( interpolate )
            return (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource(t, level, Interpolation.NLINEAR).realRandomAccess();
        else
            return (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource(t, level, Interpolation.NEARESTNEIGHBOR).realRandomAccess();

            /*
        RealRandomAccess< ? extends RealType< ? > > sourceAccess;
        if ( interpolate )
            sourceAccess = getInterpolatedRealTypeNonVolatileRealRandomAccess( sourceandconverter, t, level, Interpolation.NLINEAR );
        else
            sourceAccess = getInterpolatedRealTypeNonVolatileRealRandomAccess( sourceandconverter, t, level, Interpolation.NEARESTNEIGHBOR );

        return sourceAccess;*/
    }

    public static boolean isInterpolate( Source< ? > source )
    {
        boolean interpolate = true;
        return interpolate;
    }


    public static CompositeImage asCompositeImage(
            double[] voxelSpacing,
            String voxelUnit,
            ArrayList< RandomAccessibleInterval< UnsignedShortType > > rais,
            ArrayList< ARGBType > colors,
            ArrayList< double[] > displayRanges,
            ArrayList< Boolean > isSegmentations )
    {
        final RandomAccessibleInterval< UnsignedShortType > stack = Views.stack( rais );

        final ImagePlus imp = ImageJFunctions.wrap( stack, "Bdv View Capture" );

        // duplicate: otherwise it is virtual and cannot be modified
        final ImagePlus dup = new Duplicator().run( imp );

        IJ.run( dup,
                "Properties...",
                "channels="+rais.size()
                        +" slices=1 frames=1 physicalUnit="+voxelUnit
                        +" pixel_width=" + voxelSpacing[ 0 ]
                        +" pixel_height=" + voxelSpacing[ 1 ]
                        +" voxel_depth=" + voxelSpacing[ 2 ] );

        final CompositeImage compositeImage = new CompositeImage( dup );

        for ( int channel = 1; channel <= compositeImage.getNChannels(); ++channel )
        {
            final Boolean isSegmentation = isSegmentations.get( channel - 1 );

            if ( isSegmentation )
            {
                final LUT lut = Luts.glasbeyLutIJ();
                compositeImage.setC( channel );
                compositeImage.setChannelLut( lut );
            }
            else
            {
                Color color = new Color( colors.get( channel - 1 ).get() );
                final LUT lut = compositeImage.createLutFromColor( color );
                compositeImage.setC( channel );
                compositeImage.setChannelLut( lut );
                final double[] range = displayRanges.get( channel - 1 );
                compositeImage.setDisplayRange( range[ 0 ], range[ 1 ] );
            }
        }

        compositeImage.setTitle( "Bdv View Capture" );
        IJ.run( compositeImage, "Make Composite", "" );

        return compositeImage;
    }


    public static boolean isSourceIntersectingCurrentView( BigDataViewer bdv, int t, SourceAndConverter sac, boolean is2D )
    {
        final Interval interval = getSourceGlobalBoundingInterval( t, sac );

        final Interval viewerInterval =
                Intervals.smallestContainingInterval(
                        getViewerGlobalBoundingInterval( bdv ) );

        boolean intersects = false;
        if (is2D) {
            intersects = !Intervals.isEmpty(
                    intersect2D(interval, viewerInterval));
        } else {
            intersects = ! Intervals.isEmpty(
                    Intervals.intersect( interval, viewerInterval ) );
        }
        return intersects;
    }

    // TODO : check whether level 0 is good
    public static Interval getSourceGlobalBoundingInterval( int t, SourceAndConverter sac )
    {

        final AffineTransform3D sourceTransform = new AffineTransform3D();

        sac.getSpimSource().getSourceTransform( t, 0, sourceTransform );

        final RandomAccessibleInterval< ? > rai = sac.getSpimSource().getSource(t,0);
               // getRandomAccessibleInterval( bdvHandle, sourceId );
        final Interval interval =
                Intervals.smallestContainingInterval( sourceTransform.estimateBounds( rai ) );
        return interval;
    }

}
