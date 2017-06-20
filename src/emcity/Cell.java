package emcity;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.util.Pair;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.BoundingBox;
import emcity.EmCity.REPRESENTATION;
import emcity.EmCity.TYPE;
import emcity.EmCity.Typed;


class Cell implements Typed, Colonizeable{
	
	public static long xy2long(float x, float y){
		return (((long) ((int)x * 0.1)) << 32) + ((int) (y * 0.1));
	}
	
	public static IGeometry unitCube = DefaultGeometry.createV(MeshUtilities.UNIT_CUBE_TRIANGLES);
	private final int x, y;
	private int size, capacity, occupation;
	private RGBA activity = RGBA.RED;
	private Cluster cluster;
	private IMesh capacityCube, occupationCube;
	private Consumer<IScene> update;
	private TYPE type;

	Cell(TYPE type, int x, int y, int capacity, int size, Cluster c) {
		this.x = x;
		this.y = y;
		this.size = size;
		this.capacity = capacity;
		this.occupation = 0;
		this.cluster = c;
		this.activity = type.getColor();
		this.type = type;
		this.occupationCube = MeshUtilities.createCube(new ColorMaterial(activity), Queue.DEPTH, EnumSet.of(Flag.DONT_CAST_SHADOW));
		this.occupationCube.getAttributes().put(REPRESENTATION.key(), REPRESENTATION.values()[type.ordinal()]);
		this.occupationCube.setName(x+""+y);
	}
	
	public boolean is(TYPE t){
		return type.equals(t);
	}
	
	public TYPE getType(){
		return type;
	}
	
	public long getLocationKey() {
		return xy2long(x, y);
	}
	
	public IMesh getCapacityCube(boolean store){
		if (capacityCube == null){
			float s2 = ((float) size) / 2;
			List<Vec3> lines = boxDiagonalOutline(new Vec3(-s2,-s2,0), new Vec3(s2, s2, capacity));
			IMesh m = MeshUtilities.createLines(lines, EmCity.LINEWIDTH);
			m.setPosition(new Vec3(x,y,0));
			if (store) capacityCube = m;
			else return m;
		}
		return capacityCube;
	}

	public List<Pair<Vec3,Vec3>> getRelativeOutline(){
		float s2 = ((float) size) / 2;
		return boxDiagonalOutlinePairs(new Vec3(x-s2, y-s2, 0), new Vec3(x+s2, y+s2, capacity));
	}
	
	public static List<Vec3> boundingBoxOutline(IMesh cube) {
		BoundingBox box = cube.getBounds();
		return boxDiagonalOutline(box.getMin(), box.getMax());
	}
	
	public static List<Vec3> boxDiagonalOutline(Vec3 min, Vec3 max){
		float x = max.x - min.x;
		float y = max.y - min.y;
		float z = max.z - min.z;
		Vec3 up = new Vec3(0,0,z);
		Vec3 fwd = new Vec3(0,y,0);
		Vec3 side = new Vec3(x,0,0);
		Vec3 minUp = min.add(up);
		Vec3 minRight = min.add(side);
		Vec3 minFwd = min.add(fwd);
		Vec3 maxDown = max.subtract(up);
		Vec3 maxLeft = max.subtract(side);
		Vec3 maxBack = max.subtract(fwd);
		return Arrays.asList(
			min, minUp,
			min, minRight,
			min, minFwd,
			max, maxDown,
			max, maxLeft,
			max, maxBack,
			maxBack, minUp,
			maxBack, minRight,
			maxDown, minFwd,
			maxDown, minRight,
			maxLeft, minUp,
			maxLeft, minFwd
		);
	}
	
	public static List<Pair<Vec3,Vec3>> boxDiagonalOutlinePairs(Vec3 min, Vec3 max){
		float x = max.x - min.x;
		float y = max.y - min.y;
		float z = max.z - min.z;
		Vec3 up = new Vec3(0,0,z);
		Vec3 fwd = new Vec3(0,y,0);
		Vec3 side = new Vec3(x,0,0);
		Vec3 minUp = min.add(up);
		Vec3 minRight = min.add(side);
		Vec3 minFwd = min.add(fwd);
		Vec3 maxDown = max.subtract(up);
		Vec3 maxLeft = max.subtract(side);
		Vec3 maxBack = max.subtract(fwd);
		return Arrays.asList(
			new Pair<>(min, minUp),
			new Pair<>(min, minRight),
			new Pair<>(min, minFwd),
			new Pair<>(max, maxDown),
			new Pair<>(max, maxLeft),
			new Pair<>(max, maxBack),
			new Pair<>(maxBack, minUp),
			new Pair<>(maxBack, minRight),
			new Pair<>(maxDown, minFwd),
			new Pair<>(maxDown, minRight),
			new Pair<>(maxLeft, minUp),
			new Pair<>(maxLeft, minFwd)
		);
	}
	
	public IMesh getOccupationCube(){
		return occupationCube;
	}
	
	public void removeOccupationCube(){
		occupationCube = null;
	}
	
	// @return: remaining agents
	public int colonize(int newAgents){
		if (isFull())
			return newAgents; // return all initial agents as remaining agents
		int available = capacity - occupation;
		int taken = Math.min(available, newAgents);
		occupation += taken;
		if (isFull()){
			occupationCube.setTransform(getOccupationTransformation());
			cluster.addOccupationCube(occupationCube);
			update = scene -> {
				scene.remove3DObject(occupationCube);
			};
		}
		else if (occupation > 0){
			// first colonization
			if (occupation == taken){
				update = (IScene scene) -> {
					scene.add3DObject(occupationCube);
					occupationCube.setTransform(getOccupationTransformation());
				};
			} else {
				update = (IScene scene) -> {
					occupationCube.setTransform(getOccupationTransformation());
				};
			}
			
		}
		return newAgents - taken;
	}
	
	private Mat4 getOccupationTransformation(){
		Vec3 pos = new Vec3(x,y,(float)occupation/2);
		Vec3 scale = new Vec3(size, size, occupation);
		return Mat4.multiply(Mat4.translate(pos), Mat4.scale(scale));
	}
	
	public boolean update(IScene scene){
		if (update != null) {
			update.accept(scene);
			update = null;
			return true;
		}
		return false;
	}
	
	public boolean needsUpdate(){
		return update != null;
	}
	
	public boolean isFull() {
		return occupation >= capacity;
	}

	public void empty() {
		this.occupation = 0;
	}

	public void fill() {
		this.occupation = capacity;
	}
	
	public String toString(){
		return "Cell: "+x+", "+y+", "+size+", "+capacity;
	}
	
	public List<int[]> getFootPrint(){
		List<int[]> fp = new LinkedList<>();
		int s = size / 2;
		fp.add(new int[]{x-s, y+s}); 
		fp.add(new int[]{x+s, y+s});
		fp.add(new int[]{x+s, y-s});
		fp.add(new int[]{x-s, y-s});
		fp.add(new int[]{x-s, y+s});
		return fp;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public Vec3 getPosition(){
		return new Vec3(x,y,0);
	}

	public int getSize() {
		return size;
	}

	public int getCapacity() {
		return capacity;
	}

	public int getOccupation() {
		return occupation;
	}
	public void setOccupation(int o){
		occupation = o;
		if (occupationCube != null)
			occupationCube.setVisible(false);
	}
	public Cluster getCluster(){
		return cluster;
	}
}