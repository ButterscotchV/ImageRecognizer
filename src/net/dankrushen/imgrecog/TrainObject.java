package net.dankrushen.imgrecog;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

public class TrainObject {
	private HashMap<String, Double> percentages = new HashMap<String, Double>();
	private File image;
	private String category;
	
	public TrainObject(File image) {
		this.image = image;
		this.category = image.getParentFile().getName();
	}
	
	public String getCategory() {
		return category;
	}
	
	public HashMap<String, Double> getPercentages() {
		return this.percentages;
	}
	
	public TrainObject train(ImageRecog imageRecognizer) {
		percentages = imageRecognizer.getPercentages(image);
		return this;
	}
	
	public double compare(TrainObject object) {
		HashMap<String, Double> otherPercentage = object.getPercentages();
		double averageDifference = 0;
		int numberEntries = 0;
		
		for(Entry<String, Double> entry : percentages.entrySet()) {
			if(otherPercentage.containsKey(entry.getKey())) {
				averageDifference += Math.abs(otherPercentage.get(entry.getKey()).doubleValue() - entry.getValue());
				numberEntries++;
			}
		}
		
		return Math.round(averageDifference / numberEntries);
	}
}
