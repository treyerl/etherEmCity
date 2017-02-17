package emcity;
import java.util.Arrays;
import java.util.List;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec2;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.LineString;
import plethora.core.Ple_Agent;

class Agent extends Ple_Agent {

	public static final int PRIVATE = 1;
	public static final int CULTURE = 2;
	public static final int SQUARE = 3;
	
	public static final int numCategories = 4;
	
	public interface Type {
		default boolean is(int type){
			return type == 0;
		}
		
		default int getType(){
			return 0;
		}
	}
	
	public interface Private extends Type{
		default boolean is(int type){
			return type == PRIVATE;
		}
		
		default int getType(){
			return PRIVATE;
		}
	}
	
	public interface Culture extends Type{
		default boolean is(int type){
			return type == CULTURE;
		}
		
		default int getType(){
			return CULTURE;
		}
	}
	
	public interface Square extends Type{
		default boolean is(int type){
			return type == SQUARE;
		}
		
		default int getType(){
			return SQUARE;
		}
	}
	
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
	
	int participants;
	boolean is_active;
	int counter;

	float distance;
	
	public Vec3 lastCns;

	EmCity em;

	Vec3 coming_loc, target_c, lastLocation;
	private Parameters params;
	private Vec2 bounce;
	private PheromoneCanvas pheromones;

	Agent(EmCity em, Vec3 _loc, Parameters params, Vec2 bounceSpace, PheromoneCanvas phc) {
		super(em.getController(), _loc);
		this.em = em;
		this.participants = 500;
		this.is_active = true;
		this.counter = 0;
		this.bounce = bounceSpace;
		this.coming_loc = null;
		this.pheromones = phc;
		this.params = params;
	}
	
	/**
	 * @param max_distance
	 * @param max_angle
	 * @param attraction_factor
	 * @param type
	 */
	int attraction(float max_distance, float max_angle, float attraction_factor) {
		final Parameters settings = em.getParameters();
		int type = 0;
		for (Cluster c: em.getClusters()){
			if (c.isAttracting()){
				Vec3 att = c.getAttractionPoint();
				Vec3 loc = getLocation();
				if (Math.abs(loc.x - att.x) > max_distance && Math.abs(loc.y - att.y) > max_distance)
					continue;
				Vec3 target = new Vec3(att.x, att.y, 0);
				Vec3 direction = target.subtract(loc);
				float orientation_angle = direction.angle(this.getVelocity());
				float distance = target.distance(this.getLocation());
				if (distance < max_distance && orientation_angle < max_angle) {
					this.seek(target, (max_distance - distance) * attraction_factor);
					c.maxAgentDistance(distance);
				}
				// define type of volume to be added
				if (type == 0 && settings.isGeneratingVolumes() && counter >= settings.getApproach()) {
					type = c.getType();
				}
			}
		}
		return type;
	}

	// UPDATE NORMALIZED + INTERACT(test coming position)
	void interacting_update(boolean normalized) {
		// Update velocity
		Vec3 vel = getVelocity().add(getAcceleration()).normalize().scale(getMaxSpeed());
		counter++;

		// Test coming position
		coming_loc = getLocation().add(vel);
		Cell c = em.getCells().get(Cell.xy2long(coming_loc.x, coming_loc.y));
		if (c != null) c.cluster.agentInteraction(this);

		if (normalized)
			vel = vel.normalize();

		setLocation(getLocation().add(vel.scale(2)));
		setVelocity(vel);
		// Reset acceleration to 0 each cycle
		setAcceleration(Vec3.ZERO);
	}

	// COLLISION - change direction
	void collision() {
		Vec3 vel = getVelocity();
		double x = vel.x + MathUtilities.random(-10, 10);
		double y = vel.y + MathUtilities.random(-10, 10);
		setVelocity(new Vec3(x, y, vel.z));
		counter = 0;

		/// this.vel = this.vel.normalize();
		// TODO test new position until finds empty cell - correct direction
		/// (loc+vel to key -> vel)
	}
	
	private void bounceSpace(){
		if (bounce != null){
			bounceSpace(bounce.x/2, bounce.y/2, 0);
		}
	}
	
	int tick(List<Ple_Agent> allAgents, List<Vec3> antennas, LineString linestring){
		lastLocation = getLocation();
		if (params.isSwarmingOnOff()) {
			flock(allAgents, 80F, 65F, 35F, params.getCohesion(), params.getAlignment(), params.getSeparation()); 
		} else {
			wander2D(5, 0, (float) Math.PI);
		}

		// wander: inputs: circleSize, distance, variation in radians agent.wander2D(5, 0, PI); separation
		separationCall(allAgents, 10F, 10F); //[-0.03624, 0.03444, 0.00000]
		// define the boundries of the sagentce as bounce agent.bounceSpace(970,887.5, 0); 
		

		// attraction to clusters
		int newType = 0;
		newType = attraction(params.getAttractionDistance(), params.getAtt_angle(), params.getAtt_factor());
		
		//hack concerning the Tanjong Pagar site.
		//avoiding the roads as obstacles using the addForce function from Plethora library
		//changing the direction of agents
		//let's force the agents to stay within the borders of waterfront and an island
		//using this function, the agents no longer occupying CBD
//		Vec3 fLoc_ob = agent.futureLoc(5);
//		Vec3 cns_ob = agent.closestNormalToLineString(linestring, fLoc_ob);	
//		float obstacle_distance = cns_ob.distance(fLoc_ob);
//		float maxForce = (float) 1.2;
//
//		if (obstacle_distance > 0 && obstacle_distance < 5) { // distance
//			agent.setMaxforce(maxForce); // can be changed later on
//			agent.addForce(-agent.getLocation().x, -agent.getLocation().y, agent.getLocation().z);
//		}

		// STIGMERGY
		if (params.isStigmergy()) {

			// Vec3D fVec = new Vec3D(cos(ang)*fStrength,
			// sin(ang)*fStrength, 0);
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

			// spread
			pheromones.add(getLocation(), 80);// spread pheromone

			// agent.update(); //move agent to new position

			// update agents location based on agents calculations
			// normalized velocity vector (sum of vel + acc)(no acceleration)
//			agent.interacting_update(true);
		}
		
		if (params.isShowingDirection()) {
			// calculate future location of the agent
			Vec3 fLoc = futureLoc(15);
			antennas.add(getLocation());
			antennas.add(fLoc);
		}

		// path following attraction
		if (params.isFollowingPath()) {
			Vec3 fLoc = futureLoc(6);
			Vec3 cns = closestNormalToLineString(linestring, fLoc);
			seek(cns, params.getFollowPathFactor());
		}
		
		interacting_update(true);
		bounceSpace();
		
		return newType;
	}
	
	public void addStep(List<Vec3> steps){
		steps.add(lastLocation);
		steps.add(getLocation());
	}
}