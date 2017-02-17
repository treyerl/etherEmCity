package emcity;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.render.forward.ForwardRenderer;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.FrameCameraControl;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMapMaterial;
import ch.fhnw.ether.scene.mesh.material.IMaterial;
import ch.fhnw.ether.scene.mesh.material.LineMaterial;
import ch.fhnw.ether.view.IView.Config;
import ch.fhnw.ether.view.IView.ViewType;
import ch.fhnw.ether.view.OffscreenView;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public final class TrailBuffer {

	private int nx, ny, offX, offY;
	private OffscreenView view;
	private IMesh mesh;
	private ColorMapMaterial m;
	private List<Vec3> newLines;
	private IScene scene;
	private IController controller;
	private float trailStrength = 0.3f;
	private ICamera cam;
	private CountDownLatch initiated;

	public TrailBuffer(int width, int height){
		controller = new DefaultController(new ForwardRenderer(false));
		offX = (nx = width) / 2;
		offY = (ny = height) / 2;
		newLines = new LinkedList<>();
		initiated = new CountDownLatch(1);
		controller.run(time -> {
			view = new OffscreenView(controller, width, height, new Config(ViewType.RENDER_VIEW, 4, RGBA.CLEAR));
			scene = new DefaultScene(controller);
			cam = new Camera(new Vec3(0,0,10), Vec3.ZERO);
			controller.setCamera(view, cam);
			initiated.countDown();
		});
		try {
			initiated.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public float getTrailStrength() {
		return trailStrength;
	}

	public void setTrailStrength(float trailStrength) {
		this.trailStrength = trailStrength;
	}

	public void addValue(Vec3 v) {
		int x = ((int) v.x) + offX, y = ((int) v.y) + offY;
		if (x >= 0 && y >= 0 && x < nx && y < ny){
			float[] p = new float[4];
			view.getImage().getPixel(x, y, p);
			view.getImage().setPixel(x, y, new float[]{1,0,0,(p[3] += trailStrength)});
		}
	}
	
	public void dropTrail(Vec3 from, Vec3 to){
		newLines.add(from);
		newLines.add(to);
	}

	public float readValue(Vec3 v) {
		int x = ((int) v.x) + offX, y = ((int) v.y) + offY;
		if (x >= 0 && y >= 0 && x < nx && y < ny){
			IHostImage img = view.getImage();
			float[] p = new float[4];
			img.getPixel(x, y, p);
			return p[3];
		}
		return 0;
	}

	public void decay(float mult) {
		IHostImage img = view.getImage();
		float[] p = new float[4];
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++){
				img.getPixel(i, j, p);
				float val = p[3];
				if (val > 0){
					img.setPixel(i, j, new float[]{1,0,0, val * mult});
				}
			}
		}
	}

	public void reset() {
		view.getImage().clear();
	}

	public IMesh getCanvas() {
		if (mesh == null){
			CountDownLatch meshCreated = new CountDownLatch(1);
			controller.run(time -> {
				float[] vertices = { 0, 0, 0, 0.5f, 0, 0.5f, 0, 0, 0.5f };
				float[] colors = { 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1 };
				float[] texCoords = { 0, 0, 1, 1, 0, 1 };
				IHostImage img = view.getImage();
				m = new ColorMapMaterial(RGBA.WHITE, img.createGPUImage(), true);
				IGeometry g = DefaultGeometry.createVCM(vertices, colors, texCoords);
				mesh = new DefaultMesh(Primitive.TRIANGLES, m, g);
				mesh.setTransform(Mat4.scale(img.getWidth(), img.getHeight(), 1));
				FrameCameraControl fcc = new FrameCameraControl(cam, Arrays.asList(mesh));
				fcc.frame();
				meshCreated.countDown();
			});
			try {
				meshCreated.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return mesh;
	}
	
	public void update(){
		controller.run(time -> {
			IMaterial lm = new LineMaterial(new RGBA(1,0,0,trailStrength));
			IGeometry g = DefaultGeometry.createV(Vec3.toArray(newLines));
			IMesh lines = new DefaultMesh(Primitive.LINES, lm, g);
			synchronized(view){
				scene.add3DObject(lines);
				controller.getRenderManager().update();
			}
			newLines.clear();
		});
	}
	
	public void drawUpdate(){
		synchronized(view){
			if (m != null) 
				m.setColorMap(view.getImage().createGPUImage());
		}
	}

}
