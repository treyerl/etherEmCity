package emcity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Display;

import emcity.EmCity.REPRESENTATION;

public class Parameters {
	// general settings
	private boolean 
			swarmingOnOff = false, 
			showingTrails = true, 
			stigmergy = true, 
			showingDirection = false, 
			followingPath = true, 
			generatingVolumes = false, 
			showingAttractionCircle = false,
			showPheromone = true;
	private boolean[] showing = new boolean[REPRESENTATION.values().length];
	private int 
			approach = 100,
			NSamples = 70, //200 - how many samples it takes (precise)
			attractionDistance = 100,
			maxTrailLength = 300,
			accessDistance = 100;
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
	
	private Set<IView> views = new HashSet<>();
	
	Parameters(){
		Arrays.fill(showing, true);
	}
	
	public void register(IView view){
		views.add(view);
	}
	
	public void notifyViews(){
		Display.getDefault().asyncExec(() -> {
			views.stream().forEach(v -> v.update());
		});
	}
	
	
	public boolean isSwarmingOnOff() {
		return swarmingOnOff;
	}
	public void setSwarmingOnOff(boolean swarmingOnOff) {
		this.swarmingOnOff = swarmingOnOff;
		notifyViews();
	}
	public boolean isShowingTrails() {
		return showingTrails;
	}
	public void setShowingTrails(boolean showTrails) {
		this.showingTrails = showTrails;
		notifyViews();
	}
	public boolean isStigmergy() {
		return stigmergy;
	}
	public void setStigmergy(boolean stigmergy) {
		this.stigmergy = stigmergy;
		notifyViews();
	}
	
	public boolean isShowingDirection() {
		return showingDirection;
	}
	public void setShowingDirection(boolean showingDirection) {
		this.showingDirection = showingDirection;
		notifyViews();
	}
	public boolean isFollowingPath() {
		return followingPath;
	}
	public void setFollowingPath(boolean attract) {
		this.followingPath = attract;
		notifyViews();
	}
	public boolean isGeneratingVolumes() {
		return generatingVolumes;
	}
	public void setGeneratingVolumes(boolean generate) {
		this.generatingVolumes = generate;
		notifyViews();
	}
	public boolean isShowingAttractionCircle() {
		return showingAttractionCircle;
	}
	public void setShowingAttractionCircle(boolean showingAttractionCircle) {
		this.showingAttractionCircle = showingAttractionCircle;
		notifyViews();
	}
	public boolean isShowPheromone() {
		return showPheromone;
	}
	public void setShowPheromone(boolean showPheromone) {
		this.showPheromone = showPheromone;
		notifyViews();
	}
	public int getApproach() {
		return approach;
	}
	public void setApproach(int approach) {
		this.approach = approach;
		notifyViews();
	}
	public int getNSamples() {
		return NSamples;
	}
	public void setNSamples(int nSamples) {
		this.NSamples = nSamples;
		notifyViews();
	}
	public float getFollowPathFactor() {
		return followPathFactor;
	}
	public void setFollowPathFactor(float factor) {
		this.followPathFactor = factor;
		notifyViews();
	}
	public float getScatter() {
		return scatter;
	}
	public void setScatter(float scatter) {
		this.scatter = scatter;
		notifyViews();
	}
	public float getTrailDecay() {
		return trailDecay;
	}
	public void setTrailDecay(float decay) {
		this.trailDecay = decay;
		notifyViews();
	}
	public int getAttractionDistance() {
		return attractionDistance;
	}
	public void setAttractionDistance(int att_distance) {
		this.attractionDistance = att_distance;
		notifyViews();
	}
	public float getAtt_angle() {
		return att_angle;
	}
	public void setAtt_angle(float att_angle) {
		this.att_angle = att_angle;
		notifyViews();
	}
	public float getAtt_factor() {
		return att_factor;
	}
	public void setAtt_factor(float att_factor) {
		this.att_factor = att_factor;
		notifyViews();
	}
	public float getStigmergyStrength() {
		return stigmergyStrength;
	}
	public void setStigmergyStrength(float stigmergyStrength) {
		this.stigmergyStrength = stigmergyStrength;
		notifyViews();
	}
	public float getCohesion() {
		return cohesion;
	}
	public void setCohesion(float cohesion) {
		this.cohesion = cohesion;
		notifyViews();
	}
	public float getAlignment() {
		return alignment;
	}
	public void setAlignment(float alignment) {
		this.alignment = alignment;
		notifyViews();
	}
	public float getSeparation() {
		return separation;
	}
	public void setSeparation(float separation) {
		this.separation = separation;
		notifyViews();
	}
	public String getPathOBJ() {
		return pathOBJ;
	}
	public void setPathOBJ(String pathOBJ) {
		this.pathOBJ = pathOBJ;
		notifyViews();
	}
	public String getPathPDF() {
		return pathPDF;
	}
	public void setPathPDF(String pathPDF) {
		this.pathPDF = pathPDF;
		notifyViews();
	}
	public String getPathJPG() {
		return pathJPG;
	}
	public void setPathJPG(String pathJPG) {
		this.pathJPG = pathJPG;
		notifyViews();
	}
	public int getMaxTrailLength() {
		return maxTrailLength;
	}
	public void setMaxTrailLength(int maxTrailLength) {
		this.maxTrailLength = maxTrailLength;
		notifyViews();
	}
	public boolean isShowing(REPRESENTATION r){
		return showing[r.ordinal()];
	}
	public void setShowing(REPRESENTATION r, boolean show){
		showing[r.ordinal()] = show;
		notifyViews();
	}
	public int getAccessDistance() {
		return accessDistance;
	}
	public void setAccessDistance(int accessDistance) {
		this.accessDistance = accessDistance;
		notifyViews();
	}
	
}
