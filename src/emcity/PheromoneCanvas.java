package emcity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage;
import ch.fhnw.ether.platform.IImageSupport.FileFormat;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.ether.platform.Platform;

public class PheromoneCanvas {
	private int nx, ny, offX, offY;
	private float[][] values;
	
	public PheromoneCanvas(int width, int height){
		offX = (nx = width) / 2;
		offY = (ny = height) / 2;
		values = new float[width][height];
	}
	
	public void export(File f) throws FileNotFoundException, IOException{
		IHostImage img = IHostImage.create(nx, ny, IImage.ComponentType.FLOAT, IImage.ComponentFormat.RGBA);
		for (int x = 0; x < nx; x++){
			for (int y = 0; y < ny; y++){
				img.setPixel(x, y, new float[]{1,1,1,values[x][y]});
			}
		}
		String extension = "png";
		int i = f.getName().lastIndexOf(".");
		if (i > 0) extension = f.getName().substring(i);
		Platform.get().getImageSupport().write(img, new FileOutputStream(f), FileFormat.get(extension));
	}
	
	public void add(Vec3 v, float pheromone) {
		int x = ((int) v.x) + offX, y = ((int) v.y) + offY;
		if (x >= 0 && y >= 0 && x < nx && y < ny){
			values[x][y] = pheromone;
		}
	}

	public float read(Vec3 v) {
		int x = ((int) v.x) + offX, y = ((int) v.y) + offY;
		if (x >= 0 && y >= 0 && x < nx && y < ny){
			return values[x][y];
		}
		return 0;
	}

	public void decay(float mult) {
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++){
				values[i][j] *= mult;
			}
		}
	}

	public void reset() {
		decay(0);
	}
}
