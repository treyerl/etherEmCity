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
import java.util.Iterator;
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
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
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
import ch.fhnw.util.math.geometry.LineStrip;
import emcity.luci.EmCityLuciService;

public class EmCity {
	public static interface Typed {
		boolean is(TYPE type);
		TYPE getType();
	}
	
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
		
		private List<Integer> removeGeneratedGeometry(){
			List<Integer> idsToDelete = generatedClusters.stream()
					.map(cl -> cl.getLuciID())
					.filter(id -> id != 0)
					.collect(Collectors.toList());
			run(time -> {
				IScene scene = getScene();
				synchronized(generatedOutlines){
					synchronized(allMeshes){
						allMeshes.removeAll(generatedOutlines);
					}
					scene.remove3DObjects(generatedOutlines);
					generatedOutlines.clear();
				}
				synchronized(generatedCells){
					synchronized(cells){
						generatedCells.stream().forEach(c -> {
							if (c.getOccupationCube() != null) 
								scene.remove3DObject(c.getOccupationCube());
							cells.remove(c.getLocationKey());
						});
					}
					generatedCells.clear();
				}
				synchronized(generatedClusters){
					synchronized(clusters){
						clusters.removeAll(generatedClusters);
					}
					generatedClusters.clear();
				}
				
				synchronized(addedCenterPoints){
					scene.remove3DObjects(addedCenterPoints);
					addedCenterPoints.clear();
				}
			});
			return idsToDelete;
		}
		
		public void resetScene() {
			removeGeneratedGeometry();
			run(time -> {
				IScene scene = getScene();
				synchronized(trails){
					scene.remove3DObjects(trails);
					trails.clear();
				}
				synchronized(cells){
					cells.values().stream().forEach(c -> {
						c.setOccupation(0);
					});
				}
				synchronized(clusters){
					clusters.stream().forEach(c -> {
						c.setOccupation(0);
					});
				}
				synchronized(agentPoints){
					scene.remove3DObjects(agentPoints);
					agentPoints = null;
				}
				synchronized(mAntennas){
					scene.remove3DObject(mAntennas);
					mAntennas = null;
				}
				synchronized(agents){
					agents.forEach(a -> {
						if (a.getAttractionCircle() != null) 
							scene.remove3DObject(a.getAttractionCircle());
					});
					agents.clear();
				}
				
				addAgents();
			});
		}
		
		public void geometryVisibility(REPRESENTATION rep){
			controller.run(time -> {
				allMeshes.stream().filter(m -> rep.equals(m.getAttributes()
						.get(REPRESENTATION.key()))).forEach(m -> m.setVisible(params.isShowing(rep)));
				TYPE t = rep.asType();
				if (t != null){
					cells.values().stream().filter(c -> c.is(t)).forEach(c ->{
						IMesh cube = c.getOccupationCube();
						if (cube != null) cube.setVisible(params.isShowing(rep));
					});
				}
			});
		}
		public void showAttractionCircle(boolean att) {
			agents.forEach(agent -> agent.isShowingAttractionCircle(att));
		}
		public void toggleVolumes(int index) {
			addVolumesForAllAgents[index] = !addVolumesForAllAgents[index];
		}
		
		public void addAgents(){
			synchronized(agents){
				final Vec2 bounce = new Vec2(sceneWidth, sceneHeight);
				iteratePopulations((i,size) -> {
					Agent agent = new Agent(EmCity.this, Agent.initLocations[i], params, bounce, pheromones);
					agent.setVelocity(new Vec3(MathUtilities.random(-1, 0.5f), MathUtilities.random(-1, 0.5f), 0));
					agent.isShowingAttractionCircle(params.isShowingAttractionCircle());
					// TODO - the velocity can be changed and can contribute to the weights as well
					agents.add(agent);
				});
			}
		}
		
		public void updateTypologies() throws IOException{
			List<Cluster> updatedClusters = new LinkedList<>();
			List<Integer> deletedIDs = updateTypologies(read.lines("data/typologies_test.txt"), updatedClusters);
			if (luci != null) 
				luci.uploadClusters(updatedClusters, deletedIDs);
		}
		
		public List<Integer> updateTypologies(Stream<String> lines, List<Cluster> updatedClusters){
			final List<Integer> deletedIDs = new LinkedList<>();
			i=0;
			final boolean[] needToRemoveOutlines = new boolean[]{false};
			final List<IMesh> newOutlines = new LinkedList<>();
			List<Cluster> newClusters = new LinkedList<>();
			synchronized(cells){
				synchronized(typologies){
					lines.forEachOrdered(s -> {
						if (i<typologies.size()){
							Typology t = typologies.get(i++);
							if (t.setPoints(read.typologyPointsFromString(s), cells, updatedClusters)){
								needToRemoveOutlines[0] = true;
							}
							newClusters.addAll(t.usingMe);
						} else {
							typologies.add(new Typology(read.typologyPointsFromString(s)));
						}
					});
				}
			}

			synchronized(clusters){
				synchronized(cells){
					while (typologies.size() > i){
						needToRemoveOutlines[0] = true;
						typologies.remove(i);
					}
				}
			}

			if (needToRemoveOutlines[0]){
				deletedIDs.addAll(removeGeneratedGeometry());
				deletedIDs.removeAll(newClusters.stream()
						.map(cl -> cl.getLuciID())
						.filter(id -> id != 0)
						.collect(Collectors.toList()));
				newOutlines.addAll(Cluster.createOutlines(newClusters));
				List<IMesh> centerPoints = Cluster.createCenterPoints(newClusters);
				controller.run(time -> {
					synchronized(generatedOutlines){
						generatedOutlines.addAll(newOutlines);
						controller.getScene().add3DObjects(newOutlines);
					}
					synchronized(addedCenterPoints){
						controller.getScene().add3DObjects(centerPoints);
						addedCenterPoints.addAll(centerPoints);
					}
				});
			}
			updateStats();
			return deletedIDs;
		}
		public void moveOrigins() {
			System.out.println("TODO: move origins");
		}
	}
	
	public static enum REPRESENTATION {
		CULTURE("1", new RGBA(0xD2E87EFF), 0.5f), 
		SQUARE("2", new RGBA(0xA2B93AFF), 0.5f), 
		PRIVATE("3", RGBA.LIGHT_GRAY, 0.5f), 
		BUILDINGS2D("B", RGBA.LIGHT_GRAY, 0.5f), 
		ROADS("W", RGBA.DARK_GRAY, 0.5f);
		private String keyStroke;
		private RGBA color;
		private float lineWidth;
		REPRESENTATION(String stroke, RGBA color, float lineWidth){
			this.keyStroke = stroke;
			this.color = color;
			this.lineWidth = lineWidth;
		}
		String getKeyStroke(){
			return keyStroke;
		}
		public TYPE asType(){
			if (ordinal() < TYPE.values().length)
				return TYPE.values()[ordinal()];
			return null;
		}
		public RGBA getColor(){
			return color;
		}
		public float getLineWidth(){
			return lineWidth;
		}
		public static String key(){
			return "REPR";
		}
	}
	
	public static enum TYPE {
		CULTURE(1), SQUARE(1), PRIVATE(2);
		private int weight;
		private static Random rand = new Random();
		TYPE(int weight){
			this.weight = weight;
		}
		public RGBA getColor(){
			return represent().getColor();
		}
		public static int count(){
			return values().length;
		}
		private static int allWeights(){
			int aw = 0;
			for (TYPE t: values()) aw += t.weight;
			return aw;
		}
		public static TYPE getRandom(){
			int r = rand.nextInt(allWeights());
			int w = 0;
			for (TYPE t: values()){
				w += t.weight;
				if (r <= w) return t;
			}
			return mostProbableType();
		}
		public static TYPE mostProbableType(){
			TYPE t = values()[0];
			for (TYPE tt: values()) 
				if (tt.weight > t.weight) 
					t = tt;	
			return t;
		}
		public REPRESENTATION represent(){
			return REPRESENTATION.values()[ordinal()];
		}
	}
	
	public static final float LINEWIDTH = 0.5f;
	public static final RGBA ATTRACTION_COLOR = new RGBA(0xE8C36599);

	private final List<Agent> agents;
	private final List<Typology> typologies;
	private final LinkedList<IMesh> trails; 
	private final List<Cell> generatedCells;
	private final List<IMesh> allMeshes, generatedOutlines, addedCenterPoints;
	private final List<Cluster> clusters, generatedClusters;
	private final Map<Long, Cell> cells;
	
	private Reader read;
	private LineStrip linestring;
	private MultiLineString buildings, roads;
	private int i = 0;
	private Parameters params;
	
	private EmCityLuciService luci;
	private EmCityController controller;
	private boolean[] addVolumesForAllAgents = new boolean[TYPE.count()];
	static final int cell_size_b = 10;
	static final int cell_size_park = 10;
	
	private int windowWidth = 1200, windowHeight = 900,
				sceneWidth = 3290, sceneHeight = 3016,
				windowX = 100, windowY = 0;
	private Timer timer;
	private int simulationFPS = 25;
	private int frameDuration = 1000 / simulationFPS;
	private IMesh sceneBoundary, mAntennas, agentPoints;
	private TimerTask step;
	
	private PheromoneCanvas pheromones;
	private ParametersView pv;
	private Random r = new Random();
	private LineMaterial red = new LineMaterial(RGBA.RED).setWidth(0.5f);
	
	// TODO: IMutableMesh trails
	
	public EmCity(){
		this(null);
	}
	
	public EmCity(EmCityLuciService luciService) {
		luci = luciService;
		timer = new Timer();
		params = new Parameters();
		trails = new LinkedList<>();
		generatedCells = new ArrayList<>();
		allMeshes = new ArrayList<>();
		generatedOutlines = new ArrayList<>();
		addedCenterPoints = new ArrayList<>();
		agents = new ArrayList<>();
		cells = new HashMap<>();
		clusters = new ArrayList<>();
		typologies = new ArrayList<>();
		generatedClusters = new ArrayList<>();
		
		Platform.get().init();
		controller = new EmCityController();
		controller.run(time -> {
			Config c = new Config(ViewType.INTERACTIVE_VIEW, 32, RGBA.GRAY, IView.ViewFlag.SMOOTH_LINES);
//			Config c = new Config(ViewType.INTERACTIVE_VIEW, 0, RGBA.GRAY);
			IView view = new DefaultView(controller, windowX, windowY, windowWidth, windowHeight, c, "EtherEmCity");
			IScene scene = new EmScene(controller);
			controller.setScene(scene);
			controller.setTool(getTool());
			Display.getDefault().asyncExec(() -> {
				pv = new ParametersView(params, windowWidth+windowX, windowY, controller);
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
			
			startSimulationThread();
		});
		
		Platform.get().run();
	}
	
	private void startSimulationThread(){
		new Thread(() -> {
			controller.addAgents();
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
	
	public EmCityController getController(){
		return controller;
	}
	
	public Map<Long, Cell> getCells(){
		return cells;
	}
	
	public Parameters getParameters(){
		return params;
	}

	private void loadGeometry(){
		read = new Reader();
		roads = new MultiLineString();
		buildings = new MultiLineString();
		
		try {
			read.cluster(read.lines("data/clusters.txt"), cell_size_b, cells, clusters, TYPE.PRIVATE);
			read.cluster(read.lines("data/culture_clusters.txt"), cell_size_b, cells, clusters, TYPE.CULTURE);
			read.cluster(read.lines("data/square_clusters.txt"), cell_size_park, cells, clusters, TYPE.SQUARE);
			typologies.addAll(read.typologies(read.lines("data/typologies.txt")));
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
			sceneBoundary = MeshUtilities.createLines(b, 1);
		}
		return sceneBoundary;
	}
	
	private void drawGeoemtry() {
		IMesh mesh;
		IScene scene = controller.getScene();
		// boundary rectangle
		scene.add3DObject(getSceneBoundary());

		// draw center
		IMaterial m = new PointMaterial(RGBA.RED, 10);
		scene.add3DObject(MeshUtilities.createPoints(Arrays.asList(Vec3.ZERO), m, Queue.DEPTH, Flag.DONT_CAST_SHADOW));
		
		// agent origins
		scene.add3DObject(Agent.getOrigins(20));

		// building footprints of existing buildings
		mesh = buildings.getMesh(REPRESENTATION.BUILDINGS2D);
		scene.add3DObjects(mesh);
		allMeshes.add(mesh);
		
		// roads
		mesh = roads.getMesh(REPRESENTATION.ROADS);
		scene.add3DObject(mesh);
		allMeshes.add(mesh);
		
		// clusters capacity meshes
		List<IMesh> outlines = Cluster.createOutlines(clusters);
		scene.add3DObjects(outlines);
		allMeshes.addAll(outlines);
		
		// cluster center points
		List<IMesh> points = Cluster.createCenterPoints(clusters);
		scene.add3DObjects(points);
		allMeshes.addAll(points);
		
		for (REPRESENTATION re: REPRESENTATION.values()){
			allMeshes.stream().filter(g -> re.equals(g.getAttributes()
					.get(REPRESENTATION.key()))).forEach(g -> g.setVisible(params.isShowing(re)));
		}
	}
	
	private void iteratePopulations(BiConsumer<Integer,Integer> f){
		for (int i = 0; i < Agent.populationSizes.length; i++){
			int size = Agent.populationSizes[i];
			for (int j = 0; j < size; j++){
				f.accept(i, size);
			}
		}
	}
	
	public void tickAgents() {
		synchronized(clusters){
			clusters.stream().forEach(cl -> cl.resetMaxAgentDistance());
		}
		List<Vec3> antennas = new LinkedList<>();
		List<Cluster> newClusters = new LinkedList<>();
		List<Vec3> trailSteps = new LinkedList<>();
		final List<IMesh> remove = new LinkedList<>();
		boolean update = false;
		synchronized(agents){
			synchronized(cells){
				Iterator<Agent> it = agents.iterator();
				while(it.hasNext()){
					Agent agent = it.next();
					
					// needs update?
					if (!agent.isActive()){
						it.remove();
						IMesh c = agent.getAttractionCircle();
						if (c != null) remove.add(c);
						continue;
					}
					update = true;
					
					// for the trails
					Vec3 lastLocation = agent.getLocation();
					
					// plethora; affecting acceleration
					agent.plethoraMovement(agents);
					
					// attraction; affecting acceleration
					TYPE newType = agent.getAttracted(clusters);
					Cluster cl = null;
					for (TYPE type: TYPE.values()){
						if (type.equals(newType) || addVolumesForAllAgents[type.ordinal()]) {
							cl = typologies.get(r.nextInt(typologies.size()))
										.createVolume(agent.getLocation().x, agent.getLocation().y, cells, type);
							if (cl != null) {
								synchronized(clusters){
									clusters.add(cl);
								}
								newClusters.add(cl);
								break;
							}
						}
					}
					
					// stigmergy; affecting acceleration
					agent.stigmergy();
					
					// follow path; affecting acceleration
					agent.followPath(linestring);

					// doing the actual step
					// this applies all the acceleration changes to the 
					// velocity; make sure to call acceleration affecting
					// methods before
					agent.step(true, cl);
					
					// antennas; depending on velocity
					agent.addAntennas(antennas);
					
					// bounce off the boundary
					agent.bounceSpace();
					
					// add step to the trail
					trailSteps.add(lastLocation);
					trailSteps.add(agent.getLocation());
				}
			}
		}
		
		if (params.getTrailDecay() < 1) 
			pheromones.decay(params.getTrailDecay());
		
		// reset all addVolume values to false
		for (int i = 0; i < addVolumesForAllAgents.length; ++i){
			addVolumesForAllAgents[i] = false;
		}
		
		if (luci != null){
			if (newClusters.size() > 0){
				luci.uploadClusters(newClusters, null);
			}
		}
		synchronized(generatedCells){
			generatedCells.addAll(newClusters.stream().flatMap(cl -> cl.cells.stream()).collect(Collectors.toList()));
		}
		
		// outlines
		final List<IMesh> meshes = Cluster.createOutlines(newClusters);
		generatedOutlines.addAll(meshes);
		allMeshes.addAll(meshes);
		
		// trails
		final IGeometry g = DefaultGeometry.createV(Vec3.toArray(trailSteps));
		final IMesh newestTrailStep = new DefaultMesh(Primitive.LINES, red, g, Queue.TRANSPARENCY, Flag.DONT_CAST_SHADOW);
		newestTrailStep.setVisible(params.isShowingTrails());
		
		// agent positions
		final List<Vec3> agentPositions;
		synchronized(agents){
			agentPositions = agents.stream()
					.filter(agent -> ((Agent)agent).isActive())
					.map(agent -> agent.getLocation())
					.collect(Collectors.toList());
		}
		
		// cluster center points
		final List<IMesh> newCenterPoints = Cluster.createCenterPoints(newClusters);
		synchronized(allMeshes){
			allMeshes.addAll(newCenterPoints);
		}
		synchronized(addedCenterPoints){
			addedCenterPoints.addAll(newCenterPoints);
		}
		synchronized(generatedClusters){
			generatedClusters.addAll(newClusters);
		}
		
		updateStats();
				
		if (update){
			controller.run(time -> {
				IScene scene = controller.getScene();
				
				scene.remove3DObjects(remove);
				
				scene.add3DObjects(meshes);
				
				synchronized(cells){
					cells.values().stream().forEach(cell -> {
						if (cell.update(scene)) {
							cell.getOccupationCube().setVisible(params.isShowing(cell.getType().represent()));
						}
					});
				}
				
				if (mAntennas == null) 
					scene.add3DObject((mAntennas = MeshUtilities.createLines(antennas, new LineMaterial(RGBA.RED).setWidth(0.5f))));
				else 
					mAntennas.getGeometry().modify((attributes, data) -> {
						data[0] = Vec3.toArray(antennas);
					});
				
				
				if (agentPoints == null) 
					scene.add3DObject((agentPoints = MeshUtilities.createPoints(agentPositions, RGBA.WHITE, 5, Queue.OVERLAY, Flag.DONT_CAST_SHADOW)));
				else
					agentPoints.getGeometry().modify((attributes, data) -> {
						data[0] = Vec3.toArray(agentPositions);
					});
	
				synchronized(trails){
					trails.add(newestTrailStep);
					while (trails.size() > params.getMaxTrailLength()){
						scene.remove3DObject(trails.removeFirst());
					}
					scene.add3DObject(newestTrailStep);
				}
				
				scene.add3DObjects(newCenterPoints);
				
			});
		}
	}
	
	private void updateStats() {
		Display.getDefault().asyncExec(() -> {
			pv.updateAgents(agents);
			pv.updateClusters(generatedClusters);
		});
	}

	public NavigationTool getTool() {
		return new NavigationTool(controller, new PickTool(controller)){
			
			@Override
			public void pointerDragged(IPointerEvent e){
				super.pointerDragged(e);
				if (luci != null) {
					luci.publishCamera(getCamera(e.getView()));
				}
			}
			
			@Override
			public void pointerScrolled(IPointerEvent e){
				super.pointerScrolled(e);
				if (luci != null){
					luci.publishCamera(getCamera(e.getView()));
				}
			}
			
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
						controller.updateTypologies();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					break;
				case GLFW.GLFW_KEY_C:
					boolean att = !params.isShowingAttractionCircle();
					params.setShowingAttractionCircle(att);
					controller.showAttractionCircle(att);
					break;
				case GLFW.GLFW_KEY_D:
					params.setShowingDirection(!params.isShowingDirection());
					break;
				case GLFW.GLFW_KEY_I:
					controller.toggleVolumes(TYPE.PRIVATE.ordinal());
					break;
				case GLFW.GLFW_KEY_U:
					controller.toggleVolumes(TYPE.CULTURE.ordinal());
					break;
				case GLFW.GLFW_KEY_Q:
					controller.toggleVolumes(TYPE.SQUARE.ordinal());
					break;
				case GLFW.GLFW_KEY_H:
					params.setShowPheromone(!params.isShowPheromone());
					break;
				case GLFW.GLFW_KEY_O:
					controller.moveOrigins();
					break;
				case GLFW.GLFW_KEY_P:
					params.setFollowingPath(!params.isFollowingPath());
					break;
				case GLFW.GLFW_KEY_S:
					params.setSwarmingOnOff(!params.isSwarmingOnOff());
					break;
				case GLFW.GLFW_KEY_G:
					params.setGeneratingVolumes(!params.isGeneratingVolumes());
					break;
				case GLFW.GLFW_KEY_X:
					params.setShowingTrails(!params.isShowingTrails());
					controller.toggleShowTrails();
					break;
				case GLFW.GLFW_KEY_B:
					params.setShowing(REPRESENTATION.BUILDINGS2D, !params.isShowing(REPRESENTATION.BUILDINGS2D));
					controller.geometryVisibility(REPRESENTATION.BUILDINGS2D);
					break;
				case GLFW.GLFW_KEY_W:
					params.setShowing(REPRESENTATION.ROADS, !params.isShowing(REPRESENTATION.ROADS));
					controller.geometryVisibility(REPRESENTATION.ROADS);
					break;
				case GLFW.GLFW_KEY_A:
					controller.addAgents();
					break;
				case GLFW.GLFW_KEY_1:
					params.setShowing(REPRESENTATION.CULTURE, !params.isShowing(REPRESENTATION.CULTURE));
					controller.geometryVisibility(REPRESENTATION.CULTURE);
					break;
				case GLFW.GLFW_KEY_2:
					params.setShowing(REPRESENTATION.SQUARE, !params.isShowing(REPRESENTATION.SQUARE));
					controller.geometryVisibility(REPRESENTATION.SQUARE);
					break;
				case GLFW.GLFW_KEY_3:
					params.setShowing(REPRESENTATION.PRIVATE, !params.isShowing(REPRESENTATION.PRIVATE));
					controller.geometryVisibility(REPRESENTATION.PRIVATE);
					break;
				}
			}
		};
	}
	
	public static void main(String args[]) {
		new EmCity();
	}

}