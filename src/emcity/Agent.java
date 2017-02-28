package emcity;
import static ch.fhnw.util.math.MathUtilities.random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec2;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.LineStrip;
import emcity.EmCity.TYPE;
import plethora.core.Ple_Agent;

class Agent extends Ple_Agent {
	
	private static final IGeometry g = MeshUtilities.createDiskGeometry(64);
	
	public static final int[] populationSizes = new int[]{
		10, // pop 1
		16, // pop 2
		18, // pop 3
		18, // pop 4
		15, // pop 5
		10, // pop 6
		12, // pop 7
		14  // pop 8
	};
	
	public static final Vec3[] initLocations = new Vec3[]{
			new Vec3(-1019, -180.95f, 0),	//pop1
			new Vec3(1414, -60, 0),			//pop2
			new Vec3(131, 844, 0),			//pop3
			new Vec3(-230, -452, 0),		//pop4
			new Vec3(-1085, 754, 0),		//pop5
			new Vec3(-460, 452, 0),			//pop6
			new Vec3(1381, -663, 0),		//pop7
			new Vec3(954, -482, 0)			//pop8
	};

	public static IMesh getOrigins(int radius){
		return MeshUtilities.createPoints(Arrays.asList(Agent.initLocations), new RGBA(255, 132, 0, 90), radius);
	}
	
	public static Map<Integer, List<Agent>> perType(List<Agent> agents){
		return agents.stream().collect(Collectors.groupingBy(Agent::getCurrentInterest));
	}
	
	public static List<IMesh> pointMeshPerType(List<Agent> agents){
		List<IMesh> points = new ArrayList<>(TYPE.count());
		perType(agents).forEach((i, l) -> {
			points.add(i, MeshUtilities.createPoints(l.stream()
				.map(a -> a.getLocation())
				.collect(Collectors.toList()), TYPE.values()[i].getColor(), radius, Queue.OVERLAY, Flag.DONT_CAST_SHADOW));
		});
		return points;
	}
	
	public static List<List<Vec3>> pointsPerType(List<Agent> agents){
		List<List<Vec3>> points = new ArrayList<>(TYPE.count());
		perType(agents).forEach((i,l) -> {
			points.add(i, l.stream().map(a -> a.getLocation()).collect(Collectors.toList()));
		});
		return points;
	}
	
	private static int radius = 5;
	
	private int counter, participants, currentInterestIndex;
	private EmCity em;
	private Vec3 coming_loc, lastLocation;
	private Parameters params;
	private Vec2 bounce;
	private PheromoneCanvas pheromones;
	private boolean isActive = true, isShown = true;
	private IMesh attractionCircle;
	private static ColorMaterial m = new ColorMaterial(EmCity.ATTRACTION_COLOR);
	private int[] interests;
	private final TYPE type;

	Agent(EmCity em, Vec3 _loc, Parameters params, Vec2 bounceSpace, PheromoneCanvas phc) {
		super(em.getController(), _loc);
		this.em = em;
		this.counter = 0;
		this.bounce = bounceSpace;
		this.coming_loc = null;
		this.pheromones = phc;
		this.params = params;
		type = TYPE.getRandom();
		if (type == TYPE.SQUARE)
			participants = 50;
		else participants = 500;
//		System.out.println(_loc +" "+ Arrays.toString(interests));
	}
	
	public TYPE getType(){
		return type;
	}
	
	public int getParticipants(){
		return participants;
	}
	
	public void setParticipants(int participants){
		this.participants = participants;
	}
	
	public int getCurrentInterest(){
		return interests[currentInterestIndex];
	}
	
	public boolean isActive(){
		return isActive;
	}
	
	public void setActive(boolean active){
		this.isActive = active;
		if (attractionCircle != null) 
			attractionCircle.setVisible(false);
	}
	
	public boolean isShowingAttractionCircle(){
		return isShown;
	}
	
	public void isShowingAttractionCircle(boolean is){
		if (attractionCircle != null){
			attractionCircle.setVisible(is);
		}
		isShown = is;
	}
	
	/**Agents get attracted by buildings of the type defined upon agent construction.
	 * If a requested building type is close (max_distance) the agent tries to reach it.
	 * Otherwise it builds a building itself. 
	 * @param max_distance
	 * @param max_angle
	 * @param attraction_factor
	 */
	public TYPE getAttracted(final List<Cluster> clusters) {
		final Vec3 loc = getLocation();
		final Cluster cl;
		final float max_distance = params.getAttractionDistance(),
					max_angle = params.getAtt_angle(),
					attraction_factor = params.getAtt_factor();
		
		synchronized(clusters){
			// find closest cluster that we do not have occupied yet
			cl = clusters.stream()
				.filter(c -> {
					Vec3 d = c.getAttractionPoint().subtract(loc);
					return !c.isFull() &&
							c.getType() == type &&
							d.x < max_distance && 
							d.y < max_distance;
				})
				.sorted((c1, c2) -> {
					Vec3 d1 = c1.getAttractionPoint().subtract(loc);
					Vec3 d2 = c2.getAttractionPoint().subtract(loc);
					return new Float(d1.squaredLength()).compareTo(d2.squaredLength());
				})
				.findFirst().orElse(null);
		}
//		System.out.println(isShown);
		if (cl != null && counter < params.getApproach()){
			Vec3 d = cl.getAttractionPoint().subtract(loc);
			float orientation_angle = MathUtilities.DEGREES_TO_RADIANS * d.angle(getVelocity());
			float di = d.length();
			if (di < max_distance && orientation_angle < max_angle){
				seek(cl.getAttractionPoint(), (max_distance - di) * attraction_factor);
				// define type of volume to be added
				if (isShown){
					if (attractionCircle == null){
						attractionCircle = new DefaultMesh(Primitive.TRIANGLES, m, g, Queue.TRANSPARENCY, EnumSet.of(Flag.SHADER_TRANSFORMATION));
						em.getController().run(time -> {
							em.getController().getScene().add3DObject(attractionCircle);
							attractionCircle.setVisible(isShown);
						});
					} else attractionCircle.setVisible(isShown);
					attractionCircle.setTransform(new Mat4(di, 0,  0,  loc.x,
														   0,  di, 0,  loc.y,
														   0,  0,  di, loc.z,
														   0,  0,  0,  1).transpose());
				} else if (attractionCircle != null) 
					attractionCircle.setVisible(false);
			} else if (attractionCircle != null) 
				attractionCircle.setVisible(false);
		} else if (params.isGeneratingVolumes()){
			return type;
		} else if (attractionCircle != null) {
			attractionCircle.setVisible(false);
		}
		return null;
	}

	
	/**Advance the agents location
	 * @param normalize whether or not to normalize the velocity vector
	 * @param cl The newly built cluster that needs to be colonized or null if there is no new cluster
	 */
	void step(boolean normalize, Cluster cl) {
		// Update velocity
		Vec3 vel = getVelocity().add(getAcceleration()).normalize().scale(getMaxSpeed());
		counter++;
		
		// Test coming position
		Cell c = null;
		if (cl == null){
			coming_loc = getLocation().add(vel);
			c = em.getCells().get(Cell.xy2long(coming_loc.x, coming_loc.y));
			if (c != null){
				cl = c.getCluster();
			}
		} 
		if (cl != null && cl.getType() == type){
			if (cl.receive(this, c)){
				setParticipants(0);
				setActive(false);
			} else collision(c);
		} else collision(c);
		
		if (normalize)
			setVelocity(vel.normalize());
		else setVelocity(vel);

		setLocation(getLocation().add(getVelocity().scale(2)));
		
		// Reset acceleration to 0 each cycle
		setAcceleration(Vec3.ZERO);
	}

	
	/** r=d−2(d⋅n)n wher<br>e 
	 * 	r = reflection, <br>
	 * 	d = incoming direction,<br> 
	 * 	n = surface normal, <br>
	 * 	⋅ = dot product<br>
	 * 
	 *  If the agent is located "on" a cell already, it does not collide (otherwise it would shiver around)
	 * @param c Cell
	 */
	void collision(Cell c) {
		if (c == null) return;
		if (em.getCells().get(Cell.xy2long(getLocation().x, getLocation().y)) != null){
			counter = 0;
			return;
		}
		Vec3 reflection, newPos;
		// derive the normal: since out cell is a square rectangle we check for 45° degree quarters
		Vec3 dir = c.getPosition().subtract(getLocation());
		Vec3 normal;
		float x = dir.x, y = dir.y;
		if (x < y){
			// TOP
			if (Math.abs(x) < y){
				normal = new Vec3(0,-1,0);
			}
			// LEFT
			else {
				normal = new Vec3(1,0,0);
			}
		} else {
			// BOTTOM
			if (Math.abs(x) < y){
				normal = new Vec3(0,1,0);
			}
			// RIGHT
			else {
				normal = new Vec3(-1,0,0);
			}
		}
		Vec3 v = getVelocity();
		reflection = v.subtract(normal.scale(MathUtilities.dot(v.x, v.y, normal.x, normal.y)*2));
		newPos = getLocation().add(reflection);
		if (em.getCells().get(Cell.xy2long(newPos.x, newPos.y)) != null){
			setVelocity(new Vec3(v.x + random(-10f, 10f), v.y + random(-10f,10f),0));
		} else setVelocity(reflection);
		counter = 0;
	}
	
	public void bounceSpace(){
		if (bounce != null){
			bounceSpace(bounce.x/2, bounce.y/2, 0);
		}
	}
	
	public void plethoraMovement(List<Agent> allAgents){
		if (params.isSwarmingOnOff()) {
			flock(allAgents, 80F, 65F, 35F, params.getCohesion(), params.getAlignment(), params.getSeparation()); 
		} else {
			wander2D(5, 0, (float) Math.PI);
		}

		separationCall(allAgents, 10F, 10F);
	}
	
	public void stigmergy(){
		if (params.isStigmergy()) {

			// unify actual velocity vector for other calculation
			Vec3 vel = getVelocity().normalize(); 
			// scatter as distance of future localization
			Vec3 futLoc = futureLoc(params.getScatter()); 
			Vec3 bestLoc = Vec3.ZERO; // default position

			// FIND MAX VALUE AROUND
			float val = -1; // default min value
			for (int i = 0; i < params.getNSamples(); i++) {
				// scatter = distance radius
				float addX = MathUtilities.random(-params.getScatter(), params.getScatter());
				float addY = MathUtilities.random(-params.getScatter(), params.getScatter());
				Vec3 v = futLoc.add(new Vec3(addX, addY, 0));
				float sampleVal = pheromones.read(v);
				// float sampleVal = 10000000;
				if (sampleVal > val) {
					val = sampleVal;
					bestLoc = v;
				}
			}
			// stigmergy vector with scale
			Vec3 stigVec = bestLoc.subtract(getLocation()).normalize().scale(params.getStigmergyStrength());
			//  orient agent by stigmergy vector  (add velocity)
			// normalize for constant speed
			setVelocity(vel.add(stigVec).normalize());

			setMaxspeed(3);

			// spread pheromones
			pheromones.add(getLocation(), 80);
		}
	}
	
	public void addAntennas(List<Vec3> antennas){
		if (params.isShowingDirection()) {
			// calculate future location of the agent
			Vec3 fLoc = futureLoc(15);
			antennas.add(getLocation());
			antennas.add(fLoc);
		}
	}
	
	public void followPath(LineStrip linestring){
		// path following attraction
		if (params.isFollowingPath()) {
			Vec3 fLoc = futureLoc(6);
			Vec3 cns = closestNormalToLineString(linestring, fLoc);
			seek(cns, params.getFollowPathFactor());
		}
	}
	
	public void addStep(List<Vec3> steps){
		steps.add(lastLocation);
		steps.add(getLocation());
	}

	public IMesh getAttractionCircle() {
		return attractionCircle;
	}
}