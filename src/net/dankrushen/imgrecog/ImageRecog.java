package net.dankrushen.imgrecog;

import java.awt.Dimension;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import net.iharder.dnd.FileDrop;
import samson.stream.Console;

import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.awt.event.ActionEvent;
import javax.swing.JRadioButton;
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
	private JProgressBar progressBarTest;
	private JPanel diffPan;
	private JPanel debugPan;
	private JPanel optionPan;
	private JPanel settingsPanel;
	private JLabel lblDone;
	private JTabbedPane tabbedPane;
	private JPanel testPanel;
	private Component verticalStrut;
	private JPanel trainPanel;
	private JProgressBar progressBarTrain;
	private JButton btnTrain;
	private ImageRecog imgRecog = this;
	public File trainDataLoc = new File(System.getProperty("user.home") + File.separator + "Pictures" + File.separator + "ImageRecognizer Training Data.json");

	/*
	 * Methods
	 */

	public HashMap<String, Double> getPercentages(File image) {
		HashMap<String, Double> percentages = new HashMap<String, Double>();

		File fol = new File(System.getProperty("user.home") + File.separator + "Pictures" + File.separator + "ImageRecognizer");
		if(fol.exists()){
			if(fol.isDirectory()){
				imgComp = null;
				imgComp = fol.listFiles();
				if(imgComp.length != 0){
					oin = null;
					oin2 = null;
					if(method1.isSelected()){
						try {
							in2 = OutlineMethods.fitBinaryImage(ImageIO.read(image));
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else if(method2.isSelected()) {
						try {
							in2 = OutlineMethods.fitCannyEdges(ImageIO.read(image));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					try {
						oin2 = ImageIO.read(image);
					} catch (IOException e) {
						e.printStackTrace();
					}

					for(File folder : imgComp){
						if(folder.listFiles().length != 0){
							for(File img : folder.listFiles()){
								if(!img.equals(image)) {
									if(method1.isSelected()){
										try {
											in = OutlineMethods.fitBinaryImage(ImageIO.read(img));
										} catch (IOException e) {
											e.printStackTrace();
										}
									} else if(method2.isSelected()) {
										try {
											in = OutlineMethods.fitCannyEdges(ImageIO.read(img));
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
									
									// TODO Connect progressBar to get more accurate progress bars
									ic.setParameters(sldQual.getValue(), sldDif.getValue(), 50, false, new JProgressBar());
									ic.compare();
									ImageCompare ic2 = new ImageCompare(oin, oin2);
									ic2.setDebugMode(sldDebug.getValue());
									ic2.setParameters(sldQual.getValue(), sldDif.getValue(), 50, method3.isSelected(), new JProgressBar());
									ic2.compare();
									double diff = Math.round((ic.difference() + ic2.difference() + ic2.difference() + ic2.difference())/4);
									percentages.put(img.getName(), diff);
								}
							}
						}
					}

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

		return percentages;
	}

	public TrainObject getLowest(HashMap<TrainObject, Double> results) {
		Entry<TrainObject, Double> lowestEntry = null;

		for (Entry<TrainObject, Double> entry : results.entrySet()) {
			if (lowestEntry == null) lowestEntry = entry;
			else if (lowestEntry.getValue() > entry.getValue()) lowestEntry = entry;
		} 

		return lowestEntry.getKey();
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
		frmImageRecognizer.setTitle("Image Recognizer");
		frmImageRecognizer.setBounds(100, 100, 400, 230);
		frmImageRecognizer.setMinimumSize(new Dimension(400, 230));
		frmImageRecognizer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmImageRecognizer.setLocationRelativeTo(null);
		frmImageRecognizer.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmImageRecognizer.getContentPane().add(tabbedPane);

		testPanel = new JPanel();
		tabbedPane.addTab("Test", null, testPanel, null);
		testPanel.setLayout(new GridLayout(0, 1, 0, 0));

		Console.initialize();
		Console.hide();

		JButton btnFileChoose = new JButton("Choose Image");
		testPanel.add(btnFileChoose);
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
		testPanel.add(lblPath);
		lblPath.setHorizontalAlignment(SwingConstants.CENTER);

		verticalStrut = Box.createVerticalStrut(20);
		testPanel.add(verticalStrut);

		progressBarTest = new JProgressBar();
		testPanel.add(progressBarTest);

		lblDone = new JLabel("0/0 images compared");
		testPanel.add(lblDone);
		lblDone.setHorizontalAlignment(SwingConstants.CENTER);

		btnFind = new JButton("What is this?");
		testPanel.add(btnFind);
		btnFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Thread th = new Thread() {
					public void run() {
						if(lblPath.getText() != "Nothing Selected") {
							lblOut.setText("Processing...");
							
							TrainObject selected = new TrainObject(new File(lblPath.getText())).train(imgRecog);

							List<TrainObject> files = FileUtils.readFile(trainDataLoc, FileUtils.getTrainObjectListType());
							HashMap<TrainObject, Double> results = new HashMap<TrainObject, Double>();

							lblDone.setText("0/" + files.size() + " images compared");
							progressBarTest.setValue(0);
							progressBarTest.setMaximum(files.size());
							int compared = 0;

							for (TrainObject trainFile : files) {
								results.put(trainFile, selected.compare(trainFile));

								compared += 1;
								lblDone.setText(compared + "/" + files.size() + " images compared");
								progressBarTest.setValue(compared);
							}

							lblOut.setText(getLowest(results).getCategory());
						}
					}
				};
				th.setPriority(Thread.MAX_PRIORITY);
				th.start();
			}
		});

		lblStaticText1 = new JLabel("The image given is most likely...");
		testPanel.add(lblStaticText1);
		lblStaticText1.setHorizontalAlignment(SwingConstants.CENTER);

		lblOut = new JLabel("Not Run Yet");
		testPanel.add(lblOut);
		lblOut.setHorizontalAlignment(SwingConstants.CENTER);

		trainPanel = new JPanel();
		tabbedPane.addTab("Train", null, trainPanel, null);
		trainPanel.setLayout(new GridLayout(0, 1, 0, 0));

		btnTrain = new JButton("Train");
		btnTrain.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Thread th = new Thread() {
					public void run() {
						File fol = new File(System.getProperty("user.home") + File.separator + "Pictures" + File.separator + "ImageRecognizer");
						if(fol.exists()){
							if(fol.isDirectory()){
								imgComp = null;
								imgComp = fol.listFiles();
								if(imgComp.length != 0) {
									int length = 0;
									for(File folder : imgComp){
										length += folder.listFiles().length;
									}

									progressBarTrain.setMaximum(length);
									progressBarTrain.setValue(0);

									List<TrainObject> files = new ArrayList<TrainObject>();

									for(File folder : imgComp){
										if(folder.listFiles().length != 0){
											for(File img : folder.listFiles()){
												files.add(new TrainObject(img).train(imgRecog));
												progressBarTrain.setValue(progressBarTrain.getValue() + 1);
											}
										}
									}

									FileUtils.writeFile(trainDataLoc, files);

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
		});
		trainPanel.add(btnTrain);

		progressBarTrain = new JProgressBar();
		trainPanel.add(progressBarTrain);

		settingsPanel = new JPanel();
		tabbedPane.addTab("Settings", null, settingsPanel, null);
		settingsPanel.setLayout(new GridLayout(0, 2, 0, 0));

		optionPan = new JPanel();
		settingsPanel.add(optionPan);
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
		settingsPanel.add(comparePan);
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
		settingsPanel.add(diffPan);
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
		settingsPanel.add(debugPan);
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
				Console.setVisible(d != 0);
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
