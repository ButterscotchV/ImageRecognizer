package net.dankrushen.imgrecog;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import javax.swing.JFrame;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

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
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;
import net.iharder.dnd.FileDrop;

import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import java.awt.event.ActionEvent;
import javax.swing.JRadioButton;
import java.awt.Toolkit;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class ImageRecog {

	private static double splitFraction = 0.05;
	private static double minimumSideFraction = 0.1;
	private String lastBrowseLoc = System.getProperty("user.home") + File.separator + "Pictures";
	private File[] imgComp;
	private HashMap<String, Double> matchPer = new HashMap<String, Double>();
	BufferedImage in;
	BufferedImage in2;
	private JFrame frmImageRecognizer;
	private JLabel lblPath;
	private JButton btnFind;
	private JLabel lblStaticText1;
	private JLabel lblOut;
	private JRadioButton method1;
	private JRadioButton method2;
	private JLabel lblStaticText2;
	private JSlider sldQual;
	private JLabel lblQual;
	
	//TODO Add a way to automatically sort the images. This can be done by finding similar images (using ImageCompare), and grouping them into folders. You can name the folders afterwards or I might add a way to also automatically name the folders using the Internet.

	/**
	 * Methods
	 */
	public static BufferedImage fitBinaryImage(BufferedImage in) {

		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(in, null, ImageFloat32.class);

		ImageUInt8 binary = new ImageUInt8(input.width,input.height);
		BufferedImage polygon = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);

		// the mean pixel value is often a reasonable threshold when creating a binary image
		double mean = ImageStatistics.mean(input);

		// create a binary image by thresholding
		ThresholdImageOps.threshold(input, binary, (float) mean, true);

		// reduce noise with some filtering
		ImageUInt8 filtered = BinaryImageOps.erode8(binary, 1, null);
		filtered = BinaryImageOps.dilate8(filtered, 1, null);

		// Find the contour around the shapes
		List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT,null);

		// Fit a polygon to each shape and draw the results
		Graphics2D g2 = polygon.createGraphics();
		g2.setStroke(new BasicStroke(2));

		for( Contour c : contours ) {
			// Fit the polygon to the found external contour.  Note loop = true
			List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external,true,
					splitFraction, minimumSideFraction,100);

			g2.setColor(Color.RED);
			VisualizeShapes.drawPolygon(vertexes,true,g2);

			// handle internal contours now
			g2.setColor(Color.BLUE);
			for( List<Point2D_I32> internal : c.internal ) {
				vertexes = ShapeFittingOps.fitPolygon(internal,true, splitFraction, minimumSideFraction,100);
				VisualizeShapes.drawPolygon(vertexes,true,g2);
			}
		}

		return polygon;

		//gui.addImage(polygon, "Binary Blob Contours");
	}

	public static BufferedImage fitCannyEdges(BufferedImage in) {

		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(in, null, ImageFloat32.class);

		BufferedImage displayImage = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);

		// Finds edges inside the image
		CannyEdge<ImageFloat32,ImageFloat32> canny =
				FactoryEdgeDetectors.canny(2, true, true, ImageFloat32.class, ImageFloat32.class);

		canny.process(input,0.1f,0.3f,null);
		List<EdgeContour> contours = canny.getContours();

		Graphics2D g2 = displayImage.createGraphics();
		g2.setStroke(new BasicStroke(2));

		// used to select colors for each line
		Random rand = new Random(234);

		for( EdgeContour e : contours ) {
			g2.setColor(new Color(rand.nextInt()));

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

	public Entry<String,Double> getName() {
		Entry<String,Double> maxEntry = null;
		for(Entry<String,Double> entry : matchPer.entrySet()) {
			if (maxEntry == null || entry.getValue() < maxEntry.getValue()) {
				maxEntry = entry;
			}
		}
		return maxEntry;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ImageRecog window = new ImageRecog();
					window.frmImageRecognizer.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ImageRecog() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmImageRecognizer = new JFrame();
		frmImageRecognizer.setIconImage(Toolkit.getDefaultToolkit().getImage(ImageRecog.class.getResource("/net/dankrushen/imgrecog/ImgRecogIcon.png")));
		frmImageRecognizer.setTitle("Image Recognizer");
		frmImageRecognizer.setResizable(false);
		frmImageRecognizer.setBounds(100, 100, 450, 300);
		frmImageRecognizer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmImageRecognizer.getContentPane().setLayout(null);

		JButton btnFileChoose = new JButton("Choose Image");
		btnFileChoose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.addChoosableFileFilter(new ImageFilter());
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setCurrentDirectory(new File(lastBrowseLoc));
				int result = fileChooser.showOpenDialog(fileChooser);
				if (result == JFileChooser.APPROVE_OPTION) {
					lblPath.setText(fileChooser.getSelectedFile().getAbsolutePath());
				}
				lastBrowseLoc = fileChooser.getCurrentDirectory().getAbsolutePath();
			}
		});
		btnFileChoose.setBounds(10, 11, 424, 43);
		frmImageRecognizer.getContentPane().add(btnFileChoose);

		lblPath = new JLabel("Nothing Selected");
		lblPath.setHorizontalAlignment(SwingConstants.CENTER);
		lblPath.setBounds(10, 65, 424, 14);
		frmImageRecognizer.getContentPane().add(lblPath);

		btnFind = new JButton("What is this?");
		btnFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(lblPath.getText() != "Nothing Selected") {
					lblOut.setText("Processing...");
					Thread th = new Thread() {
						public void run() {
							matchPer.clear();
							imgComp = null;
							imgComp = (new File("C:\\ImageRecognizer")).listFiles();
							if(method1.isSelected()){
								try {
									in2 = fitBinaryImage(ImageIO.read(new File(lblPath.getText())));
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							if(method2.isSelected()) {
								try {
									in2 = fitCannyEdges(ImageIO.read(new File(lblPath.getText())));
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							for(File folder : imgComp){
								if(folder.listFiles().length != 0){
									matchPer.put(folder.getName(), (double) 0);
									for(File img : folder.listFiles()){
										if(method1.isSelected()){
											try {
												in = fitBinaryImage(ImageIO.read(img));
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
										if(method2.isSelected()) {
											try {
												in = fitCannyEdges(ImageIO.read(img));
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
										ImageCompare ic = new ImageCompare(in, in2);
										ic.setParameters(sldQual.getValue(), sldQual.getValue(), 1, sldQual.getValue()*2);
										ic.compare();
										double diff = Math.round(ic.difference());
										System.out.println(img.getName() + " " + diff + "%");
										matchPer.replace(folder.getName(), matchPer.get(folder.getName()) + diff);
									}
									System.out.println("!!" + folder.getName() + " total " + Math.round(matchPer.get(folder.getName())/folder.listFiles().length) + "%!!");
									System.out.println("");
									matchPer.replace(folder.getName(), (double) Math.round(matchPer.get(folder.getName())/folder.listFiles().length));
								}
							}
							
							Entry<String,Double> maxEntry = null;
							for(Entry<String,Double> entry : matchPer.entrySet()) {
								if (maxEntry == null || entry.getValue() < maxEntry.getValue()) {
									maxEntry = entry;
								}
							}
							lblOut.setText(maxEntry.getKey());
							System.out.println("Image Given is Most Likely: " + maxEntry.getKey());
							System.out.println("");
							System.out.println("---------------------------------------------------");
							System.out.println("");
						}
					};
					th.setPriority(Thread.MAX_PRIORITY);
					th.start();
				}
			}
		});
		btnFind.setBounds(10, 167, 424, 43);
		frmImageRecognizer.getContentPane().add(btnFind);

		new FileDrop(btnFileChoose, new FileDrop.Listener() {
			public void filesDropped(java.io.File[] files) {
				// handle file drop
				// ...
				if(files.length == 1){
					for (File file : files) {
						if(new ImageFilter().accept(file)) {
							lblPath.setText(file.getAbsolutePath());
						} else return;
					} // end for: through each dropped file
				} else return;
			} // end filesDropped
		}); // end FileDrop.Listener

		lblStaticText1 = new JLabel("The image given is...");
		lblStaticText1.setHorizontalAlignment(SwingConstants.CENTER);
		lblStaticText1.setBounds(10, 221, 424, 14);
		frmImageRecognizer.getContentPane().add(lblStaticText1);

		lblOut = new JLabel("Not Run Yet");
		lblOut.setHorizontalAlignment(SwingConstants.CENTER);
		lblOut.setBounds(10, 246, 424, 14);
		frmImageRecognizer.getContentPane().add(lblOut);

		method1 = new JRadioButton("Method 1 (More General Shape)");
		method1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				method1.setSelected(true);
				method2.setSelected(false);
			}
		});
		method1.setBounds(10, 99, 213, 23);
		frmImageRecognizer.getContentPane().add(method1);

		method2 = new JRadioButton("Method 2 (More Precise Shape)");
		method2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				method1.setSelected(false);
				method2.setSelected(true);
			}
		});
		method2.setSelected(true);
		method2.setBounds(10, 125, 213, 23);
		frmImageRecognizer.getContentPane().add(method2);

		lblStaticText2 = new JLabel("Comparison Quality");
		lblStaticText2.setHorizontalAlignment(SwingConstants.CENTER);
		lblStaticText2.setBounds(248, 103, 158, 14);
		frmImageRecognizer.getContentPane().add(lblStaticText2);

		lblQual = new JLabel("10 (10 is usually the best)");
		lblQual.setHorizontalAlignment(SwingConstants.CENTER);
		lblQual.setBounds(237, 140, 195, 16);
		frmImageRecognizer.getContentPane().add(lblQual);

		sldQual = new JSlider();
		sldQual.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				lblQual.setText(sldQual.getValue() + " (10 is usually the best)");
			}
		});
		sldQual.setMinimum(1);
		sldQual.setMaximum(30);
		sldQual.setValue(10);
		sldQual.setBounds(234, 125, 200, 16);
		frmImageRecognizer.getContentPane().add(sldQual);
	}
}
