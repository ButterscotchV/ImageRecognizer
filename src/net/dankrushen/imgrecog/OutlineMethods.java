package net.dankrushen.imgrecog;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;

public class OutlineMethods {
	private static double splitFraction = 0.05;
	private static double minimumSideFraction = 0.1;
	
	public static BufferedImage fitBinaryImage(BufferedImage in) {

		GrayF32 input = ConvertBufferedImage.convertFromSingle(in, null, GrayF32.class);

		GrayU8 binary = new GrayU8().createNew(input.width, input.height);
		BufferedImage polygon = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);

		// the mean pixel value is often a reasonable threshold when creating a binary image
		double mean = ImageStatistics.mean(input);

		// create a binary image by thresholding
		ThresholdImageOps.threshold(input, binary, (float) mean, true);

		// reduce noise with some filtering
		GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
		filtered = BinaryImageOps.dilate8(filtered, 1, null);

		// Find the contour around the shapes
		List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT,null);

		// Fit a polygon to each shape and draw the results
		Graphics2D g2 = polygon.createGraphics();
		g2.setStroke(new BasicStroke(2));

		g2.setColor(Color.WHITE);

		for( Contour c : contours ) {
			// Fit the polygon to the found external contour.  Note loop = true
			List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external,true,
					splitFraction, minimumSideFraction,100);

			//g2.setColor(Color.RED);
			VisualizeShapes.drawPolygon(vertexes,true,g2);

			// handle internal contours now
			//g2.setColor(Color.BLUE);
			for( List<Point2D_I32> internal : c.internal ) {
				vertexes = ShapeFittingOps.fitPolygon(internal,true, splitFraction, minimumSideFraction,100);
				VisualizeShapes.drawPolygon(vertexes,true,g2);
			}
		}

		return polygon;

		//gui.addImage(polygon, "Binary Blob Contours");
	}

	public static BufferedImage fitCannyEdges(BufferedImage in) {

		GrayF32 input = ConvertBufferedImage.convertFromSingle(in, null, GrayF32.class);

		BufferedImage displayImage = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);

		// Finds edges inside the image
		CannyEdge<GrayF32,GrayF32> canny =
				FactoryEdgeDetectors.canny(2, true, true, GrayF32.class, GrayF32.class);

		canny.process(input,0.1f,0.3f,null);
		List<EdgeContour> contours = canny.getContours();

		Graphics2D g2 = displayImage.createGraphics();
		g2.setStroke(new BasicStroke(2));

		g2.setColor(Color.WHITE);

		// used to select colors for each line
		//Random rand = new Random(234);

		for( EdgeContour e : contours ) {
			//g2.setColor(new Color(rand.nextInt()));

			for(EdgeSegment s : e.segments ) {
				// fit line segments to the point sequence.  Note that loop is false
				List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(s.points,false,
						splitFraction, minimumSideFraction,100);

				VisualizeShapes.drawPolygon(vertexes, false, g2);
			}
		}

		//gui.addImage(displayImage, "Canny Trace");

		return displayImage;
	}
}
