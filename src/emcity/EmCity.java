package emcity;

/*
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 EmCity: case study AYE Singapore: AGENT-BASED SIMULATION MODEL OF COLONIAL GROWTH 12/2016
 Update 2016: Tanjong Pagar simulation model with interactive inputs
 
 Concept of simulation, partial methods programming, GUI, Path following/Attraction, Roads, Buildings2D, editing Agent Class  - (c) Peter Bus, MOLAB, FA CTU Prague, ETH Zurich 
 
 Programming Cells classes, Transcription, Clusters classes, Map and Typology class, editing Agent Class - (c) Lukas Kurilla, MOLAB, FA CTU Prague
 
 Stigmergy algorithm and Field2D class based on Stigmergy2D sketch by (c) Alessandro Zomparelli and (c) Alessio Eriolli, Co-de-iT
 and Pheromonics Processing workshop Prague 2014 organized by VSUP/RecodeNature.org by Martin Gsandtner and tutored by Alessio Eriolli (Co-de-iT), 06/2014
 Editing and adaptation by (c) Dr. Peter Bus, Lukas Kurilla, ETH Zurich, MOLAB, FA CTU Prague
 
 Plethora library by (c) Jose Sanchez (http://www.plethora-project.com/Plethora-0.3.0/index.html)
 
 
 contact: 
 
 Dr. Peter Bus
 Chair of Information Architecture
 DARCH ETH Zurich
 bus@arch.ethz.ch
 http://archa3d.com/

 (c)2011-2016
 
 //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.widgets.Display;
import org.lwjgl.glfw.GLFW;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.tool.NavigationTool;
import ch.fhnw.ether.controller.tool.PickTool;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.render.IRenderManager;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.ether.scene.mesh.material.IMaterial;
import ch.fhnw.ether.scene.mesh.material.LineMaterial;
import ch.fhnw.ether.scene.mesh.material.PointMaterial;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.ether.view.IView.Config;
import ch.fhnw.ether.view.IView.ViewType;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec2;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.LineString;
import emcity.luci.EmCityLuciService;
import plethora.core.Ple_Agent;

public class EmCity {
	public class EmCityController extends DefaultController {

		@Override
		public void viewDisposed(IView view){
			step.cancel();
			System.exit(0);
		}
		@Override
		public void viewResized(IView view){
			super.viewResized(view);
			updateParametersWindow(view);
			Vec2 size = view.getWindow().getSize();
			windowWidth = (int) size.x;
			windowHeight = (int) size.y;
		}
		
		private void updateParametersWindow(IView view) {
			IWindow window = view.getWindow();
			if (window != null && pv != null){
				Vec2 pos = window.getPosition();
				pv.setLocation((int) (pos.x + window.getSize().x), (int)pos.y);
			}
		}
		@Override
		public void viewRepositioned(IView view){
			super.viewRepositioned(view);
			updateParametersWindow(view);
		}
		
		/** ---- BUTTON ACTIONS ---- */
		
		public void toggleShowTrails(){
			run(time -> {
				synchronized(trails){
					if (params.isShowingTrails())
						trails.stream().forEach(t -> t.setVisible(true));
					else trails.stream().forEach(t -> t.setVisible(false));
				}
			});
		}
		
		public void resetScene() {
			run(time -> {
				IScene scene = getScene();
				scene.remove3DObjects(generatedOutlines);
				scene.remove3DObjects(generatedCells.stream().map(c -> c.occupationCube).collect(Collectors.toList()));
				scene.remove3DObjects(trails);
				scene.remove3DObjects(agentPoints);
			});
		}
		
		public void resetClusters(){
			resetScene();
			generatedCells.stream().forEach(cell -> {
				clusters.removeAll(cells.values().stream().map(c -> c.cluster).collect(Collectors.toSet()));
			});
			generatedCells.clear();
		}
	}
	private ArrayList<Ple_Agent> agents;
	private List<Typology> typologies;
	private Map<Long, Cell> cells;
	private List<Cluster> clusters;
	
	private Reader read;
	private LineString linestring;
	private Buildings2D buildings;
	private Roads roads;
	private int i = 0;
	private Parameters params;
	
	private EmCityLuciService luci;
	private EmCityController controller;
	
	// int pop = 10; //80 agents = 10*8
	boolean[] addVolume = new boolean[Agent.numCategories];
	boolean[] addVolumesForAllAgents = new boolean[Agent.numCategories];
	boolean[] drawBuildings = new boolean[Agent.numCategories];
	static final int cell_size_b = 10;
	static final int cell_size_park = 10;

	boolean drawExistingBuildings = true;
	boolean reset_button = false;
	boolean DRAW_ROADS = true;

	int res = 0;
	int iterT = 1;
	float distant;
	private int windowWidth = 1200, windowHeight = 900;
	private int sceneWidth = 3290, sceneHeight = 3016;
	private Timer timer;
	private int simulationFPS = 25;
	private int frameDuration = 1000 / simulationFPS;
	private IMesh sceneBoundary, mAntennas, agentPoints;
	private TimerTask step;
	
	private PheromoneCanvas pheromones;
	private ParametersView pv;
	private Random r = new Random();
	private ColorMaterial red = new ColorMaterial(RGBA.RED);
	
	private LinkedList<IMesh> trails;
	private List<Cell> generatedCells;
	private List<IMesh> generatedOutlines;
	
	public EmCity() {
		// fullScreen();
		timer = new Timer();
		params = new Parameters();
		trails = new LinkedList<>();
		generatedCells = new LinkedList<>();
		generatedOutlines = new LinkedList<>();
		Arrays.fill(drawBuildings, true);
		
		Platform.get().init();
		controller = new EmCityController();
		controller.run(time -> {
//			Config c = new Config(ViewType.INTERACTIVE_VIEW, 32, RGBA.GRAY, ViewFlag.SMOOTH_LINES);
			Config c = new Config(ViewType.INTERACTIVE_VIEW, 0, RGBA.GRAY);
			IView view = new DefaultView(controller, 100, 100, windowWidth, windowHeight, c, "EtherEmCity");
			IScene scene = new EmScene(controller);
			controller.setScene(scene);
			controller.setTool(getTool());
			Display.getDefault().asyncExec(() -> {
				pv = new ParametersView(params, windowWidth+100, 100, controller);
				pv.show(Display.getDefault());
			});
			
			// camera
			IRenderManager rm = controller.getRenderManager();
			ICamera cam = rm.getCamera(view);
			cam.setFov(70);
			cam.setPosition(new Vec3(0, 0, 2500));
			cam.setTarget(new Vec3(0,0,0));
			cam.setUp(new Vec3(0,1,0));
			
			// TrailBuffer
			pheromones = new PheromoneCanvas(sceneWidth, sceneHeight);
//			trailBuffer.update();
			
			startSimulationThread();
		});
		
		Platform.get().run();
	}
	
	private void startSimulationThread(){
		new Thread(() -> {
			setupAgents();
			loadGeometry();
			controller.run(time -> {
				drawGeoemtry();
			});
			// luci
			if (luci != null)
				if (luci.isConnected())
					luci.createScenario(ScID -> {
						luci.uploadClusters(clusters, null);
						timer.schedule((step = step()), frameDuration, frameDuration);
					});
				else {
					System.err.println("not connected to Luci!");
					System.exit(0);
				}
			else timer.schedule((step = step()), frameDuration, frameDuration);
		}).start();
	}
	
	private TimerTask step(){
		return new TimerTask(){
			@Override
			public void run() {
				tickAgents();
			}
		};
	}
	
	public IController getController(){
		return controller;
	}
	
	public Map<Long, Cell> getCells(){
		return cells;
	}
	
	public List<Cluster> getClusters(){
		return clusters;
	}
	
	public Parameters getParameters(){
		return params;
	}
	
	public EmCity setLuci(EmCityLuciService luci){
		this.luci = luci;
		return this;
	}

	private void loadGeometry(){
		read = new Reader();
		roads = new Roads();
		cells = new HashMap<>();
		clusters = new ArrayList<>();
		buildings = new Buildings2D();
		typologies = new ArrayList<>();
		try {
			read.cluster(read.lines("data/clusters.txt"), cell_size_b, cells, clusters, Agent.PRIVATE);
			read.cluster(read.lines("data/culture_clusters.txt"), cell_size_b, cells, clusters, Agent.CULTURE);
			read.cluster(read.lines("data/square_clusters.txt"), cell_size_park, cells, clusters, Agent.SQUARE);
			typologies.addAll(read.typologies(read.lines("data/typologies_test.txt")));
			linestring = read.ghSpline(read.lines("data/path_following_line.txt"));
			buildings.setLineStrings(read.lineStrings(
					read.points(read.lines("data/budovy_body.txt")), read.lines("data/budovy_zoznam.txt"), true));
			roads.setLineStrings(read.lineStrings(
					read.points(read.lines("data/roads_body.txt")), read.lines("data/roads_index.txt"), false));
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private IMesh getSceneBoundary(){
		if (sceneBoundary == null){
			List<Vec3> b = new LinkedList<>();
			int x2 = sceneWidth / 2, y2 = sceneHeight / 2;
			b.add(new Vec3(-x2, -y2, 0));
			b.add(new Vec3( x2, -y2, 0));
			
			b.add(new Vec3( x2, -y2, 0));
			b.add(new Vec3( x2,  y2, 0));
			
			b.add(new Vec3( x2,  y2, 0));
			b.add(new Vec3(-x2,  y2, 0));
			
			b.add(new Vec3(-x2,  y2, 0));
			b.add(new Vec3(-x2, -y2, 0));
			IGeometry g = DefaultGeometry.createV(Vec3.toArray(b));
			sceneBoundary = new DefaultMesh(Primitive.LINES, new LineMaterial(RGBA.BLACK), g, Queue.DEPTH, Flag.DONT_CAST_SHADOW);
		}
		return sceneBoundary;
	}
	
	private void drawGeoemtry() {
		IScene scene = controller.getScene();
		// boundary rectangle
		scene.add3DObject(getSceneBoundary());

		// draw center
		IMaterial m = new PointMaterial(RGBA.RED, 10);
		scene.add3DObject(MeshUtilities.createPoints(Arrays.asList(Vec3.ZERO), m, Queue.DEPTH, Flag.DONT_CAST_SHADOW));
		
		// agent origins
		scene.add3DObject(Agent.getOrigins(20));

		// building footprints of existing buildings
		if (params.isDraw2DBuildings()){
			scene.add3DObjects(buildings.getMesh());
		}
		
		// roads
		scene.add3DObject(roads.getMesh());
		
		// clusters capacity meshes
		scene.add3DObjects(Cluster.createOutlines(clusters));
		
		// cluster center points
		scene.add3DObjects(Cluster.createCenterPoints(clusters));
	}
	
	private void iteratePopulations(BiConsumer<Integer,Integer> f){
		for (int i = 0; i < Agent.populationSizes.length; i++){
			int size = Agent.populationSizes[i];
			for (int j = 0; j < size; j++){
				f.accept(i, size);
			}
		}
	}
	
	private void setupAgents(){
		agents = new ArrayList<>();
		final Vec2 bounce = new Vec2(sceneWidth, sceneHeight);
		iteratePopulations((i,size) -> {
			Agent agent = new Agent(this, Agent.initLocations[i], params, bounce, pheromones);
			agent.setVelocity(new Vec3(MathUtilities.random(-1, 0.5f), MathUtilities.random(-1, 0.5f), 0));
			// TODO - the velocity can be changed and can contribute to the weights as well
			agents.add(agent);
		});
	}
	
	public void tickAgents() {	
		clusters.stream().forEach(cl -> cl.resetMaxAgentDistance());
		List<Vec3> antennas = new LinkedList<>();
		List<Cluster> newVolumes = new ArrayList<>();
		List<Vec3> trailSteps = new LinkedList<>();
		synchronized(agents){
			for (Ple_Agent pAgent : agents) {
				Agent agent = (Agent) pAgent;
				if (!agent.is_active)
					continue;
				int newType = agent.tick(agents, antennas, linestring);
				for (int i = 1; i < Agent.numCategories; i++){
					if (newType == i || addVolumesForAllAgents[i]) {
						Cluster cl = typologies.get(r.nextInt(typologies.size()))
								.createVolume(agent.getLocation().x, agent.getLocation().y, cells, i);
						if (cl != null) {
							clusters.add(cl);
							newVolumes.add(cl);
						}
					}
				}
				agent.addStep(trailSteps);
			}
		}
		
		if (params.getTrailDecay() < 1) 
			pheromones.decay(params.getTrailDecay());
		
		// reset all addVolume values to false
		for (int i = 0; i < addVolumesForAllAgents.length; ++i){
			addVolumesForAllAgents[i] = false;
		}
		
		if (luci != null){
			if (newVolumes.size() > 0){
				luci.uploadClusters(newVolumes, null);
			}
//			luci.publishCamera(gui.getCam());
		}
		generatedCells.addAll(newVolumes.stream().flatMap(cl -> cl.cells.stream()).collect(Collectors.toList()));
		final List<IMesh> meshes = Cluster.createOutlines(newVolumes);
		final IGeometry g = DefaultGeometry.createV(Vec3.toArray(trailSteps));
		final IMesh newestTrailStep = new DefaultMesh(Primitive.LINES, red, g, Queue.TRANSPARENCY, Flag.DONT_CAST_SHADOW);
		generatedOutlines.addAll(meshes);
		newestTrailStep.setVisible(params.isShowingTrails());
		
		controller.run(time -> {
//			long t = System.nanoTime();
			IScene scene = controller.getScene();
			
			scene.add3DObjects(meshes);
			
			if (meshes.size() > 0) 
				scene.add3DObjects(meshes);
			
			cells.values().stream().forEach(cell -> cell.update(scene));
			
			if (mAntennas != null) scene.remove3DObject(mAntennas);
			scene.add3DObject((mAntennas = MeshUtilities.createLines(antennas, 1)));
			
			if (agentPoints != null) scene.remove3DObject(agentPoints);
//			synchronized(agents){
			scene.add3DObject((agentPoints = MeshUtilities.createPoints(agents.stream()
					.filter(agent -> ((Agent)agent).is_active)
					.map(agent -> agent.getLocation())
					.collect(Collectors.toList()), RGBA.WHITE, 5, Queue.OVERLAY, Flag.DONT_CAST_SHADOW)));

			synchronized(trails){
				trails.add(newestTrailStep);
				while (trails.size() > params.getMaxTrailLength()){
					scene.remove3DObject(trails.removeFirst());
				}
				scene.add3DObject(newestTrailStep);
			}
			
//			System.out.println((System.nanoTime() - t) / 1000000);
//			}
		});
	}
	
	private void updateTypologies(Stream<String> lines){
		updateTypologies(lines, new LinkedList<>());
	}
	
	public List<Integer> updateTypologies(Stream<String> lines, List<Cluster> updatedClusters){
		final List<Integer> deletedIDs = new LinkedList<>();
		i=0;
		lines.forEachOrdered(s -> {
			if (i<typologies.size())
				typologies.get(i++).setPoints(read.typologyPointsFromString(s), cells, updatedClusters);
			else typologies.add(new Typology(read.typologyPointsFromString(s)));
		});
		while (typologies.size() > i){
			Typology t = typologies.remove(i);
			for (Cluster cl: t.usingMe){
				for (Cell c: cl.cells){
					cells.remove(c.getLocationKey());
				}
				clusters.remove(cl);
				if (cl.getLuciID() != 0){
					deletedIDs.add(cl.getLuciID());
				}
			}
		}
		return deletedIDs;
	}
	
	public NavigationTool getTool() {
		return new NavigationTool(controller, new PickTool(controller)){
			@Override
			public void keyPressed(IKeyEvent e) {
				switch(e.getKey()){
				case GLFW.GLFW_KEY_UP:
				case GLFW.GLFW_KEY_RIGHT:
				case GLFW.GLFW_KEY_DOWN:
				case GLFW.GLFW_KEY_LEFT:
					break;
				case GLFW.GLFW_KEY_R:
					controller.resetScene();
					break;
				case GLFW.GLFW_KEY_T:
					try {
						updateTypologies(read.lines("data/typologies_test.txt"));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					break;
				case GLFW.GLFW_KEY_I:
					addVolumesForAllAgents[Agent.PRIVATE] = !addVolumesForAllAgents[Agent.PRIVATE];
					break;
				case GLFW.GLFW_KEY_C:
					addVolumesForAllAgents[Agent.CULTURE] = !addVolumesForAllAgents[Agent.CULTURE];
					break;
				case GLFW.GLFW_KEY_Q:
					addVolumesForAllAgents[Agent.SQUARE] = !addVolumesForAllAgents[Agent.SQUARE];
					break;
				case GLFW.GLFW_KEY_B:
					drawExistingBuildings = !drawExistingBuildings;
					break;
				case GLFW.GLFW_KEY_P:
					params.setShowPheromone(!params.isShowPheromone());
					break;
				case GLFW.GLFW_KEY_W:
					DRAW_ROADS = !DRAW_ROADS;
					break;
				case GLFW.GLFW_KEY_A:
					synchronized(agents){
						final Vec2 bounce = new Vec2(sceneWidth, sceneHeight);
						iteratePopulations((i,size)->{
							Agent agent = new Agent(EmCity.this, Agent.initLocations[i], params, bounce, pheromones);
							agent.setVelocity(new Vec3(MathUtilities.random(-1, 0.5f), MathUtilities.random(-1, 0.5f), 0));
							// TODO - the velocity can be changed and can contribute to the  weights as well
							agents.add(agent);
//							agent.update();
//							agent.interacting_update(true);
						});
					}
					break;
				case GLFW.GLFW_KEY_1:
					drawBuildings[1] = !drawBuildings[1]; // 1 = Agent.PRIVATE
					break;
				case GLFW.GLFW_KEY_2:
					drawBuildings[2] = !drawBuildings[2]; // 2 = Agent.CULTURE
					break;
				case GLFW.GLFW_KEY_3:
					drawBuildings[3] = !drawBuildings[3]; // 3 = Agent.SQUARE
					break;
				case GLFW.GLFW_KEY_F:
					// TODO: export camera view as hidden-lines vector drawing to PDF
					break;
				}
			}
		};
	}
	
	public static void main(String args[]) {
		new EmCity();
	}

}