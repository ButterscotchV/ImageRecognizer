package net.dankrushen.imgrecog;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import javax.swing.JFrame;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

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
import java.util.Map.Entry;
import java.awt.event.ActionEvent;
import javax.swing.JRadioButton;
import java.awt.Toolkit;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JTabbedPane;
import java.awt.Component;
import javax.swing.Box;

public class ImageRecog {

	private static double splitFraction = 0.05;
	private static double minimumSideFraction = 0.1;
	private String lastBrowseLoc = System.getProperty("user.home") + File.separator + "Pictures";
	private File[] imgComp;
	private HashMap<String, Double> matchPer = new HashMap<String, Double>();
	BufferedImage in;
	BufferedImage in2;
	BufferedImage oin = null;
	BufferedImage oin2 = null;
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
	private JSlider sldDif;
	private JLabel lblDif;
	private JRadioButton method3;
	private JLabel lblDe;
	private JSlider sldDebug;
	private JLabel lblDebugMode;
	private JProgressBar progressBar;
	private JPanel diffPan;
	private JPanel debugPan;
	private JPanel optionPan;
	private JPanel settingsPan;
	private JLabel lblDone;
	private JTabbedPane tabbedPane;
	private JPanel Main;
	private Component verticalStrut;

	//TODO Add a way to automatically sort the images. This can be done by finding similar images (using ImageCompare), and grouping them into folders. You can name the folders afterwards or I might add a way to also automatically name the folders using the Internet.

	/*
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

		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(in, null, ImageFloat32.class);

		BufferedImage displayImage = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);

		// Finds edges inside the image
		CannyEdge<ImageFloat32,ImageFloat32> canny =
				FactoryEdgeDetectors.canny(2, true, true, ImageFloat32.class, ImageFloat32.class);

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
				try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());} catch (Exception e) {}
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
		frmImageRecognizer.setBounds(100, 100, 400, 230);
		frmImageRecognizer.setMinimumSize(new Dimension(400, 230));
		frmImageRecognizer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmImageRecognizer.setLocationRelativeTo(null);
		frmImageRecognizer.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmImageRecognizer.getContentPane().add(tabbedPane);

		Main = new JPanel();
		tabbedPane.addTab("Main", null, Main, null);
		Main.setLayout(new GridLayout(0, 1, 0, 0));

		ConsoleOutput.createGui();

		JButton btnFileChoose = new JButton("Choose Image");
		Main.add(btnFileChoose);
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

		lblPath = new JLabel("Nothing Selected");
		Main.add(lblPath);
		lblPath.setHorizontalAlignment(SwingConstants.CENTER);

		verticalStrut = Box.createVerticalStrut(20);
		Main.add(verticalStrut);

		progressBar = new JProgressBar();
		Main.add(progressBar);

		lblDone = new JLabel("0/0 images compared");
		Main.add(lblDone);
		lblDone.setHorizontalAlignment(SwingConstants.CENTER);

		btnFind = new JButton("What is this?");
		Main.add(btnFind);
		btnFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(lblPath.getText() != "Nothing Selected") {
					Thread th = new Thread() {
						public void run() {
							File fol = new File("C:" + File.separator + "ImageRecognizer");
							if(fol.exists()){
								if(fol.isDirectory()){
									matchPer.clear();
									imgComp = null;
									imgComp = fol.listFiles();
									if(imgComp.length != 0){
										lblOut.setText("Processing...");
										oin = null;
										oin2 = null;
										if(method1.isSelected()){
											try {
												in2 = fitBinaryImage(ImageIO.read(new File(lblPath.getText())));
											} catch (IOException e) {
												e.printStackTrace();
											}
										} else if(method2.isSelected()) {
											try {
												in2 = fitCannyEdges(ImageIO.read(new File(lblPath.getText())));
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
										try {
											oin2 = ImageIO.read(new File(lblPath.getText()));
										} catch (IOException e) {
											e.printStackTrace();
										}

										progressBar.setMinimum(0);
										progressBar.setMaximum(0);

										for(File f : imgComp) {
											int s = f.listFiles().length;
											if(s != 0) progressBar.setMaximum(progressBar.getMaximum() + s);
										}

										progressBar.setValue(0);

										lblDone.setText("0/" + progressBar.getMaximum() + " images compared");

										progressBar.setMaximum(progressBar.getMaximum() * 100);

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
													} else if(method2.isSelected()) {
														try {
															in = fitCannyEdges(ImageIO.read(img));
														} catch (IOException e) {
															e.printStackTrace();
														}
													}
													try {
														oin = ImageIO.read(img);
													} catch (IOException e) {
														e.printStackTrace();
													}
													ImageCompare ic = new ImageCompare(in, in2);
													ic.setDebugMode(sldDebug.getValue());
													ic.setParameters(sldQual.getValue(), sldDif.getValue(), 50, false, progressBar);
													ic.compare();
													ImageCompare ic2 = new ImageCompare(oin, oin2);
													ic2.setDebugMode(sldDebug.getValue());
													ic2.setParameters(sldQual.getValue(), sldDif.getValue(), 50, method3.isSelected(), progressBar);
													ic2.compare();
													if (sldDebug.getValue() == 7) System.out.println("ImageCompare 1 Diff: " + ic.difference() + ", ImageCompare 2 Diff: " + ic2.difference());
													double diff = Math.round((ic.difference() + ic2.difference() + ic2.difference() + ic2.difference())/4);
													System.out.println(img.getName() + " " + diff + "%");
													matchPer.replace(folder.getName(), matchPer.get(folder.getName()) + diff);
													String[] a = lblDone.getText().split("/");
													int b = Integer.parseInt(a[0]);
													b++;
													lblDone.setText(b + "/" + a[1]);
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
									} else {
										System.out.println("Error! No pictures found!");
										System.out.println("");
										System.out.println("---------------------------------------------------");
										System.out.println("");
									}
								} else {
									System.out.println("Error! Folder needed, not file!");
									System.out.println("");
									System.out.println("---------------------------------------------------");
									System.out.println("");
								}
							} else {
								System.out.println("Error! Folder not found!");
								System.out.println("");
								System.out.println("---------------------------------------------------");
								System.out.println("");
							}
						}
					};
					th.setPriority(Thread.MAX_PRIORITY);
					th.start();
				}
			}
		});

		lblStaticText1 = new JLabel("The image given is most likely...");
		Main.add(lblStaticText1);
		lblStaticText1.setHorizontalAlignment(SwingConstants.CENTER);

		lblOut = new JLabel("Not Run Yet");
		Main.add(lblOut);
		lblOut.setHorizontalAlignment(SwingConstants.CENTER);

		settingsPan = new JPanel();
		tabbedPane.addTab("Settings", null, settingsPan, null);
		settingsPan.setLayout(new GridLayout(0, 2, 0, 0));

		optionPan = new JPanel();
		settingsPan.add(optionPan);
		optionPan.setLayout(new BorderLayout(0, 0));

		method1 = new JRadioButton("Method 1 (More General Shape)");
		optionPan.add(method1, BorderLayout.NORTH);

		method2 = new JRadioButton("Method 2 (More Precise Shape)");
		optionPan.add(method2, BorderLayout.CENTER);
		method2.setSelected(true);

		method3 = new JRadioButton("Compare Colour");
		optionPan.add(method3, BorderLayout.SOUTH);
		method3.setSelected(true);

		JPanel comparePan = new JPanel();
		settingsPan.add(comparePan);
		comparePan.setLayout(new BorderLayout(0, 0));

		lblStaticText2 = new JLabel("Comparison Quality");
		comparePan.add(lblStaticText2, BorderLayout.NORTH);
		lblStaticText2.setHorizontalAlignment(SwingConstants.CENTER);

		lblQual = new JLabel("80% (80% is usually the best)");
		comparePan.add(lblQual, BorderLayout.SOUTH);
		lblQual.setHorizontalAlignment(SwingConstants.CENTER);

		sldQual = new JSlider();
		comparePan.add(sldQual, BorderLayout.CENTER);
		sldQual.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				lblQual.setText(sldQual.getValue() + "% (80% is usually the best)");
			}
		});
		sldQual.setValue(80);

		diffPan = new JPanel();
		settingsPan.add(diffPan);
		diffPan.setLayout(new BorderLayout(0, 0));

		JLabel lblDifferenceThreshold = new JLabel("Difference Threshold");
		diffPan.add(lblDifferenceThreshold, BorderLayout.NORTH);
		lblDifferenceThreshold.setHorizontalAlignment(SwingConstants.CENTER);

		lblDif = new JLabel("1 (1 is usually the best)");
		diffPan.add(lblDif, BorderLayout.SOUTH);
		lblDif.setHorizontalAlignment(SwingConstants.CENTER);

		sldDif = new JSlider();
		diffPan.add(sldDif, BorderLayout.CENTER);
		sldDif.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				lblDif.setText(sldDif.getValue() + " (1 is usually the best)");
			}
		});
		sldDif.setValue(1);
		sldDif.setMinimum(1);
		sldDif.setMaximum(1000);

		debugPan = new JPanel();
		settingsPan.add(debugPan);
		debugPan.setLayout(new BorderLayout(0, 0));

		lblDe = new JLabel("0 (0 is off)");
		debugPan.add(lblDe, BorderLayout.SOUTH);
		lblDe.setHorizontalAlignment(SwingConstants.CENTER);

		sldDebug = new JSlider();
		debugPan.add(sldDebug, BorderLayout.CENTER);
		sldDebug.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				int d = sldDebug.getValue();
				lblDe.setText(d + " (0 is off)");
				ConsoleOutput.setGuiVisible(d == 0 ? false : true);
			}
		});
		sldDebug.setValue(0);
		sldDebug.setMaximum(7);

		lblDebugMode = new JLabel("Debug Mode");
		debugPan.add(lblDebugMode, BorderLayout.NORTH);
		lblDebugMode.setHorizontalAlignment(SwingConstants.CENTER);
		method2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				method1.setSelected(false);
				method2.setSelected(true);
			}
		});
		method1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				method1.setSelected(true);
				method2.setSelected(false);
			}
		});
	}
}
