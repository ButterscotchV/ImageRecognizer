package net.dankrushen.imgrecog;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.awt.*;
import java.awt.image.*;

public class ImageCompare {

	protected BufferedImage img1 = null;
	protected BufferedImage img2 = null;
	protected BufferedImage oimg1 = null;
	protected BufferedImage oimg2 = null;
	protected BufferedImage imgc = null;
	protected int qual = 0;
	protected int factorA = 0;
	protected int factorD = 10;
	protected boolean colour = false;
	protected boolean match = false;
	protected double difference = 0;
	protected double difference2 = 0;
	protected int comparex;
	protected int comparey;
	protected int blocksx;
	protected int blocksy;
	protected double totalXaY;
	protected double multiply;
	protected boolean shouldCut;
	private JProgressBar progressBar;
	protected int debugMode = 0; // 1: textual indication of change, 2: difference of factors

	/* create a runable demo thing. */
	/**
	public static void main(String[] args) {
		// Create a compare object specifying the 2 images for comparison.
		ImageCompare ic = new ImageCompare("c:\\test1.jpg", "c:\\test2.jpg");
		// Set the comparison parameters. 
		//   (num vertical regions, num horizontal regions, sensitivity, stabilizer)
		ic.setParameters(8, 6, 5, 10);
		// Display some indication of the differences in the image.
		ic.setDebugMode(2);
		// Compare.
		ic.compare();
		// Display if these images are considered a match according to our parameters.
		System.out.println("Match: " + ic.match());
		// If its not a match then write a file to show changed regions.
		if (!ic.match()) {
			saveJPG(ic.getChangeIndicator(), "c:\\changes.jpg");
		}
	}
	 **/

	// constructor 1. use filenames
	public ImageCompare(String file1, String file2) {
		this(loadJPG(file1), loadJPG(file2));
	}

	// constructor 2. use awt images.
	public ImageCompare(Image img1, Image img2) {
		this(imageToBufferedImage(img1), imageToBufferedImage(img2));
	}

	// constructor 3. use buffered images. all roads lead to the same place. this place.
	public ImageCompare(BufferedImage img1, BufferedImage img2) {
		BufferedImage[] img = sizeImages(img1, img2);
		this.img1 = img[0];
		this.img2 = img[1];
		this.oimg1 = img[0];
		this.oimg2 = img[1];
		/*
		 * Un-Note this to auto resize images bigger than 1000 for the height
		 * Note: Currently only does this is the image's height is bigger than 1000, and always makes them into squares
		if(this.img1.getHeight() > 1000) {
			if(this.img1.getWidth() > 1000) {
				this.img1 = imageToBufferedImage(this.img1.getScaledInstance(1000, 1000, Image.SCALE_FAST));
				this.img2 = imageToBufferedImage(this.img2.getScaledInstance(1000, 1000, Image.SCALE_FAST));
			} else {
				this.img1 = imageToBufferedImage(this.img1.getScaledInstance(this.img1.getWidth(), 1000, Image.SCALE_FAST));
				this.img2 = imageToBufferedImage(this.img2.getScaledInstance(this.img1.getWidth(), 1000, Image.SCALE_FAST));
			}
		}
		 */
		autoSetParameters();
	}

	protected BufferedImage[] sizeImages(BufferedImage o1, BufferedImage o2) {
		int newWidth = (o1.getWidth() >= o2.getWidth() ? o1.getWidth() : o2.getWidth());
		int newHeight = (o1.getHeight() >= o2.getHeight() ? o1.getHeight() : o2.getHeight());
		if(o1.getHeight() != newHeight || o1.getWidth() != newWidth) o1 = imageToBufferedImage(o1.getScaledInstance(newWidth, newHeight, Image.SCALE_FAST));
		if(o2.getHeight() != newHeight || o2.getWidth() != newWidth) o2 = imageToBufferedImage(o2.getScaledInstance(newWidth, newHeight, Image.SCALE_FAST));

		return new BufferedImage[] {o1, o2};
	}

	// like this to perhaps be upgraded to something more heuristic in the future.
	protected void autoSetParameters() {
		qual = 20;
		factorA = 10;
		factorD = 10;
	}

	// set the parameters for use during change detection.
	public void setParameters(int qual, int factorA, int factorD, boolean colour, JProgressBar progressBar) {
		this.qual = qual;
		this.factorA = factorA;
		this.factorD = factorD;
		this.colour = colour;
		this.progressBar = progressBar;
	}

	// want to see some stuff in the console as the comparison is happening?
	public void setDebugMode(int m) {
		this.debugMode = m;
	}

	/*
	// compare the two images in this object.
	public void compare() {
		// setup change display image
		imgc = imageToBufferedImage(img2);
		Graphics2D gc = imgc.createGraphics();
		gc.setColor(Color.RED);
		// convert to gray images.
		img1 = imageToBufferedImage(GrayFilter.createDisabledImage(img1));
		img2 = imageToBufferedImage(GrayFilter.createDisabledImage(img2));
		// how big are each section

		int blocksx = ((int) (img1.getWidth() / comparex) == 0 ? 1 : (int) (img1.getWidth() / comparex));
		int blocksy = ((int) (img1.getHeight() / comparey) == 0 ? 1 : (int) (img1.getHeight() / comparey));
		// set to a match by default, if a change is found then flag non-match
		this.match = true;
		this.difference = 0;
		// loop through whole image and compare individual blocks of images
		for (int y = 0; y < comparey; y++) {
			if (debugMode > 0 && debugMode < 3) System.out.print("|");
			for (int x = 0; x < comparex; x++) {
				BufferedImage img1Sub = img1.getSubimage(x*blocksx, y*blocksy, blocksx, blocksy);
				BufferedImage img2Sub = img2.getSubimage(x*blocksx, y*blocksy, blocksx, blocksy);
				int b1 = getAverageBrightness(img1Sub);
				int b2 = getAverageBrightness(img2Sub);
				int diff = Math.abs(b1 - b2);
				if (diff >= factorA) { // the difference in a certain region has passed the threshold value of factorA
					// draw an indicator on the change image to show where change was detected.
					gc.drawRect(x*blocksx, y*blocksy, blocksx - 1, blocksy - 1);
					this.match = false;
					this.difference++;
				}
				if (debugMode == 1) System.out.print((diff > factorA ? "X" : " "));
				if (debugMode == 2) System.out.print(diff + (x < comparex - 1 ? "," : ""));
			}
			if (debugMode > 0 && debugMode < 3) System.out.println("|");
		}
		this.difference = (this.difference/(comparey*comparex)*100);
		if (debugMode == 3) System.out.println("Difference: " + this.difference);
		if(this.colour){
			this.difference2 = 0;
			int c1 = getAverageColour(this.oimg1);
			int c2 = getAverageColour(this.oimg2);
			//System.out.println(Integer.toHexString(c2));
			int[] argb = new int[] {(c1 >> 24) & 0xFF, (c1 >> 16) & 0xFF, (c1 >> 8) & 0xFF, (c1 >> 0) & 0xFF, (c2 >> 24) & 0xFF, (c2 >> 16) & 0xFF, (c2 >> 8) & 0xFF, (c2 >> 0) & 0xFF};
			int dif = argb.length/2;
			for(int e = 0; e < dif; e++) {
				this.difference2 += (((double) Math.abs(argb[e] - argb[e+dif]))/255)*(100+this.factorD);
			}
			this.difference = (this.difference + (this.difference2/dif))/2;
		}
		if (debugMode == 3) System.out.println("Difference2: " + this.difference2);
		this.difference2 = 0;
	}

	// return the image that indicates the regions where changes were detected.
	public BufferedImage getChangeIndicator() {
		return imgc;
	}
	 */

	private double getPixlPerBlock(double di, double imgdi) {
		while (true) {
			String[] decimal = Double.toString(di).split("\\.");
			if(decimal.length > 0) {
				if(decimal[1].equals("0")) break;
				else {
					multiply = (multiply < 1 ? multiply + 0.01 : 1);
					di = imgdi * multiply;
				}
			} else break;
		}
		return di;
	}

	/*
	private double[] getPixlPerBlock(double dix, double diy) {
		while (true) {
			shouldCut = true;
			String[] decimalx = Double.toString(dix).split("\\.");
			String[] decimaly = Double.toString(diy).split("\\.");
			if(decimalx.length == 0) decimalx[1] = "0";
			if(decimaly.length == 0) decimaly[1] = "0";
			if(!decimalx[1].equals("0") || !decimaly[1].equals("0")) {
				shouldCut = false;
				multiply = (multiply < 1 ? multiply + 0.01 : 1);
				dix = img1.getHeight() * multiply;
				diy = img1.getHeight() * multiply;
			}
			if(shouldCut) break;
		}
		System.out.println("diX : diY = " + dix + " : " + diy);
		return new double[] {dix, diy};
	}
	*/

	public void compare() {

		int origProgVal = progressBar.getValue();
		
		//Makes images gray
		img1 = imageToBufferedImage(GrayFilter.createDisabledImage(img1));
		img2 = imageToBufferedImage(GrayFilter.createDisabledImage(img2));

		//Calculates percentage of the full image height and width based on percentage given
		multiply = (100 - (double) qual)/100;
		double origMultiply = multiply;

		if(multiply != 0) {
			/*
			double[] d = getPixlPerBlock(img1.getWidth() * multiply, img1.getHeight() * multiply);

			comparex = (int) d[0];
			comparey = (int) d[1];
			*/
			
			comparex = (int) getPixlPerBlock(img1.getWidth() * multiply, (double) img1.getWidth());
			multiply = origMultiply;
			comparey = (int) getPixlPerBlock(img1.getHeight() * multiply, (double) img1.getHeight());
		} else {
			comparex = 1;
			comparey = 1;
		}

		if (debugMode == 4) System.out.println("Block Pixel Count X : Block Pixel Count Y = " + comparex + " : " + comparey);

		blocksx = img1.getWidth()/comparex;
		blocksy = img1.getHeight()/comparey;

		blocksx = (blocksx == 0 ? 1 : blocksx);
		blocksy = (blocksy == 0 ? 1 : blocksy);

		if (debugMode == 4) System.out.println("Blocks X : Blocks Y = " + blocksx + " : " + blocksy);
		
		double totalBlocks = blocksx * blocksy;
		totalXaY = 0;
		
		if (debugMode == 5) System.out.println(totalBlocks + " total blocks");
		
		this.match = true;
		this.difference = 0;

		if (debugMode == 4) System.out.println("Orig X : Orig Y = " + img1.getWidth() + " : " + img1.getHeight());
		
		for (int y = 0; y < blocksy; y++) {
			if (debugMode > 0 && debugMode < 3) System.out.print("|");
			for (int x = 0; x < blocksx; x++) {
				totalXaY++;
				if (debugMode == 5) System.out.println(totalXaY + " total X and Y");
				double totalPercent = (totalXaY / totalBlocks) * 50;
				if (debugMode == 5) System.out.println((int) Math.round(totalPercent) + "% done image");
				progressBar.setValue(origProgVal + (int) Math.round(totalPercent));
				int newx2 = x*comparex;
				int newy2 = y*comparey;
				//newx2 = (newx2 > img1.getWidth() ? img1.getWidth() - xLeft : newx2);
				//newy2 = (newy2 > img1.getHeight() ? img1.getHeight() - yLeft : newy2);
				if (debugMode == 4) System.out.println("Width : Height = " + comparex + " : " + comparey);
				if (debugMode == 4) System.out.println("X : Y = " + newx2 + " : " + newy2);
				BufferedImage img1Sub = img1.getSubimage(newx2, newy2, comparex, comparey);
				BufferedImage img2Sub = img2.getSubimage(newx2, newy2, comparex, comparey);
				double b1 = getAverageBrightness(img1Sub);
				double b2 = getAverageBrightness(img2Sub);
				if (debugMode == 6) System.out.println("Average brightness total: " + b1 + ", Average brightness total 2: " + b2);
				//int diff = Math.abs(b1 - b2);
				double diff = ((b1 > b2 ? b1 - b2 : b2 - b1)/255)*100;
				if (debugMode == 6) System.out.println("Brightness Difference: " + diff);
				this.difference += diff;
				/*if (diff >= factorA) { // the difference in a certain region has passed the threshold value of factorA
					this.match = false;
				}*/
				if (debugMode == 1) System.out.print((diff > factorA ? "X" : " "));
				if (debugMode == 2) System.out.print(diff + (x < blocksx - 1 ? "," : ""));
			}
			if (debugMode > 0 && debugMode < 3) System.out.println("|");
		}
		this.difference = (this.difference/(blocksx*blocksy));
		this.difference += this.difference*(this.factorD/100);
		if (debugMode == 3) System.out.println("Difference: " + this.difference);
		if(this.colour){
			this.difference2 = 0;
			int c1 = getAverageColour(this.oimg1);
			int c2 = getAverageColour(this.oimg2);
			//System.out.println(Integer.toHexString(c2));
			int[] argb = new int[] {(c1 >> 24) & 0xFF, (c1 >> 16) & 0xFF, (c1 >> 8) & 0xFF, (c1 >> 0) & 0xFF, (c2 >> 24) & 0xFF, (c2 >> 16) & 0xFF, (c2 >> 8) & 0xFF, (c2 >> 0) & 0xFF};
			int dif = argb.length/2;
			for(int e = 0; e < dif; e++) {
				double diff = (((double) Math.abs(argb[e] - argb[e+dif]))/255)*100;
				//diff += diff*(this.factorD/100);
				this.difference2 += diff;
			}
			this.difference = (this.difference + (this.difference2/dif))/2;
			this.difference2 = 0;
			
			if (debugMode == 3) System.out.println("Difference2: " + this.difference2);
		}
	}

	// returns a value specifying some kind of average brightness in the image.
	protected double getAverageBrightness(BufferedImage img) {
		/*
		Raster r = img.getData();
		double total = 0;
		for (int y = 0; y < r.getHeight(); y++) {
			for (int x = 0; x < r.getWidth(); x++) {
				total += r.getSampleDouble(r.getMinX() + x, r.getMinY() + y, 0);
			}
		}
		return total/(r.getWidth()*r.getHeight());
		*/
		
		//int a = 0;
		double r = 0;
		double g = 0;
		double b = 0;

		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int pixel = img.getRGB(x, y);

				//saveJPG(img, Integer.toHexString(pixel) + ".jpg");

				//System.out.println(Integer.toHexString(pixel));

				//a += (pixel >> 24) & 0xFF;
				r += (pixel >> 16) & 0xFF;
				g += (pixel >> 8) & 0xFF;
				b += (pixel >> 0) & 0xFF;
			}
		}

		double div = (img.getWidth()*img.getHeight());

		//int total = ((a / div) << 24) | ((r / div) << 16) | ((g / div) << 8) | (b / div);
		
		//if (debugMode == 6) System.out.println("Average brightness: " + (r+b+g)/(div*3));

		return (r+b+g)/(div*3);
	}

	protected int getAverageColour(BufferedImage img) {
		int a = 0;
		int r = 0;
		int g = 0;
		int b = 0;

		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int pixel = img.getRGB(x, y);

				//saveJPG(img, Integer.toHexString(pixel) + ".jpg");

				//System.out.println(Integer.toHexString(pixel));

				a += (pixel >> 24) & 0xFF;
				r += (pixel >> 16) & 0xFF;
				g += (pixel >> 8) & 0xFF;
				b += (pixel >> 0) & 0xFF;
			}
		}

		int div = (img.getWidth()*img.getHeight());

		return ((a / div) << 24) | ((r / div) << 16) | ((g / div) << 8) | (b / div);
	}


	// returns true if image pair is considered a match
	public boolean match() {
		return this.match;
	}

	// returns percentage difference
	public double difference() {
		return this.difference;
	}

	// buffered images are just better.
	protected static BufferedImage imageToBufferedImage(Image img) {
		BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		g2.drawImage(img, null, null);
		return bi;
	}

	// read a jpeg file into a buffered image
	@SuppressWarnings("deprecation")
	protected static Image loadJPG(String filename) {
		File file = new File(filename);
		BufferedImage bi;
		try {
			bi = ImageIO.read(file.toURL());
			return bi;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		};
		return null;
	}

}
