package net.dankrushen.imgrecog;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class FileUtils {

	public static <T> T readFile(File file, Type type) {
		try (Reader reader = new FileReader(file.getPath())) {
			Gson gson = new GsonBuilder().create();
			
			return gson.fromJson(reader, type);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void writeFile(File file, Object object) {
		try (Writer writer = new FileWriter(file.getPath())) {
			Gson gson = new GsonBuilder().create();
			gson.toJson(object, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Type getTrainObjectListType() {
		return new TypeToken<List<TrainObject>>() {}.getType();
	}
}