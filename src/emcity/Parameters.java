package emcity;

public class Parameters {
	// general settings
	private boolean 
			swarmingOnOff = false, 
			showingTrails = true, 
			stigmergy = true, 
			showingDirection = true, 
			followingPath = true, 
			generatingVolumes = false, 
			show_att_distance = false,
			showPheromone = true,
			draw2DBuildings = true;
	private int 
			approach = 100,
			NSamples = 70, //200 - how many samples it takes (precise)
			attractionDistance = 100,
			maxTrailLength = 100;
	private float 	
			followPathFactor = 0.05f, 
			scatter = 10,  // stigmergy influence raduis
			trailDecay = 1.5f,
			att_angle = 1.4f,
			att_factor = 0.1f, 
			stigmergyStrength = 0.8f, 
			cohesion = 0f, 
			alignment = 1.5f, 
			separation = 1.5f;
	
	private String pathOBJ, pathPDF, pathJPG;
	
	
	public boolean isSwarmingOnOff() {
		return swarmingOnOff;
	}
	public void setSwarmingOnOff(boolean swarmingOnOff) {
		this.swarmingOnOff = swarmingOnOff;
	}
	public boolean isShowingTrails() {
		return showingTrails;
	}
	public void setShowingTrails(boolean showTrails) {
		this.showingTrails = showTrails;
	}
	public boolean isStigmergy() {
		return stigmergy;
	}
	public void setStigmergy(boolean stigmergy) {
		this.stigmergy = stigmergy;
	}
	
	public boolean isShowingDirection() {
		return showingDirection;
	}
	public void setShowingDirection(boolean showingDirection) {
		this.showingDirection = showingDirection;
	}
	public boolean isFollowingPath() {
		return followingPath;
	}
	public void setFollowingPath(boolean attract) {
		this.followingPath = attract;
	}
	public boolean isGeneratingVolumes() {
		return generatingVolumes;
	}
	public void setGeneratingVolumes(boolean generate) {
		this.generatingVolumes = generate;
	}
	public boolean isShow_att_distance() {
		return show_att_distance;
	}
	public void setShow_att_distance(boolean show_att_distance) {
		this.show_att_distance = show_att_distance;
	}
	public boolean isShowPheromone() {
		return showPheromone;
	}
	public void setShowPheromone(boolean showPheromone) {
		this.showPheromone = showPheromone;
	}
	public int getApproach() {
		return approach;
	}
	public void setApproach(int approach) {
		this.approach = approach;
	}
	public int getNSamples() {
		return NSamples;
	}
	public void setNSamples(int nSamples) {
		this.NSamples = nSamples;
	}
	public float getFollowPathFactor() {
		return followPathFactor;
	}
	public void setFollowPathFactor(float factor) {
		this.followPathFactor = factor;
	}
	public float getScatter() {
		return scatter;
	}
	public void setScatter(float scatter) {
		this.scatter = scatter;
	}
	public float getTrailDecay() {
		return trailDecay;
	}
	public void setTrailDecay(float decay) {
		this.trailDecay = decay;
	}
	public int getAttractionDistance() {
		return attractionDistance;
	}
	public void setAttractionDistance(int att_distance) {
		this.attractionDistance = att_distance;
	}
	public float getAtt_angle() {
		return att_angle;
	}
	public void setAtt_angle(float att_angle) {
		this.att_angle = att_angle;
	}
	public float getAtt_factor() {
		return att_factor;
	}
	public void setAtt_factor(float att_factor) {
		this.att_factor = att_factor;
	}
	public float getStigmergyStrength() {
		return stigmergyStrength;
	}
	public void setStigmergyStrength(float stigmergyStrength) {
		this.stigmergyStrength = stigmergyStrength;
	}
	public float getCohesion() {
		return cohesion;
	}
	public void setCohesion(float cohesion) {
		this.cohesion = cohesion;
	}
	public float getAlignment() {
		return alignment;
	}
	public void setAlignment(float alignment) {
		this.alignment = alignment;
	}
	public float getSeparation() {
		return separation;
	}
	public void setSeparation(float separation) {
		this.separation = separation;
	}
	public String getPathOBJ() {
		return pathOBJ;
	}
	public void setPathOBJ(String pathOBJ) {
		this.pathOBJ = pathOBJ;
	}
	public String getPathPDF() {
		return pathPDF;
	}
	public void setPathPDF(String pathPDF) {
		this.pathPDF = pathPDF;
	}
	public String getPathJPG() {
		return pathJPG;
	}
	public void setPathJPG(String pathJPG) {
		this.pathJPG = pathJPG;
	}
	public int getMaxTrailLength() {
		return maxTrailLength;
	}
	public void setMaxTrailLength(int maxTrailLength) {
		this.maxTrailLength = maxTrailLength;
	}
	public boolean isDraw2DBuildings() {
		return draw2DBuildings;
	}
	public void setDraw2DBuildings(boolean draw2dBuildings) {
		draw2DBuildings = draw2dBuildings;
	}
}
