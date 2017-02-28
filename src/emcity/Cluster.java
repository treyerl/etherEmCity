package emcity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.ether.scene.mesh.material.LineMaterial;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;
import emcity.EmCity.REPRESENTATION;
import emcity.EmCity.TYPE;
import emcity.EmCity.Typed;

public class Cluster implements Typed, Colonizeable {
	public final static GeometryFactory gf = new GeometryFactory();
	
	public static enum Colors {
		// make sure order is in sync with emcity.TYPE
		CULTURE(new RGBA(0xD2E87EFF)),
		SQUARE(new RGBA(0xA2B93AFF)),
		PRIVATE(new RGBA(0xFFC67CFF));
		public final RGBA color;
		Colors(RGBA color){
			this.color = color;
		}
		
		public static RGBA getColor(TYPE type){
			return values()[type.ordinal()].color;
		}

		public static String getName(int type) {
			return values()[type].name();
		}
	}
	
	public static List<IMesh> createMeshPerType(List<Cluster> clusters, BiFunction<TYPE, Stream<Cluster>,IMesh> perType){
		List<IMesh> allCenters = new ArrayList<>();
		for (TYPE t: TYPE.values())
			allCenters.add(perType.apply(t, clusters.stream().filter(c -> c.type.equals(t))));
		return allCenters;
	}
	
	public static List<IMesh> createMeshListPerType(List<Cluster> clusters, BiFunction<TYPE, Stream<Cluster>,List<IMesh>> perType){
		List<IMesh> allCenters = new ArrayList<>();
		for (TYPE t: TYPE.values())
			allCenters.addAll(perType.apply(t, clusters.stream().filter(c -> c.type.equals(t))));
		return allCenters;
	}
	
	
	public static List<IMesh> createCenterPoints(List<Cluster> clusters){
		return createMeshPerType(clusters, (t, typedClusters) -> {
			IMesh mesh = MeshUtilities.createPoints(typedClusters
					.map(c -> c.attractor)
					.collect(Collectors.toList()), t.getColor(), 5);
			mesh.setName(t.name());
			mesh.getAttributes().put(REPRESENTATION.key(), REPRESENTATION.values()[t.ordinal()]);
			return mesh;
		});
	}
	
	public static List<IMesh> createOutlines(List<Cluster> clusters){
		return createMeshListPerType(clusters, (type, typedClusters) -> {
			return MeshUtilities.mergeMeshes(typedClusters
					.map(cl -> cl.getCapacityMesh())
					.collect(Collectors.toList()) ).stream()
					.map(m -> {
						m.setName(type.name());
						m.getAttributes().put(REPRESENTATION.key(), REPRESENTATION.values()[type.ordinal()]);
						return m;
					})
					.collect(Collectors.toList());
		});
	}
	
	List<Cell> cells;
	TYPE type;
	private Vec3 attractor;
	private int capacity, maxHeight = 0, occupation, luciID = 0;
	private float maxAgentDistance = 0;
	private int[] center = new int[2];
	private IMesh disk;

	// TODO cluster_type = existing structure or extension
	// TODO activity_type

	Cluster(TYPE type) {
		this.type = type;
		occupation = 0;
		cells = new LinkedList<>();
	}
	
	public boolean is(TYPE t){
		return type.equals(t);
	}
	
	public TYPE getType(){
		return type;
	}
	
	public void addCell(Cell c){
		cells.add(c);
	}
	
	public int cellCount(){
		return cells.size();
	}
	
	public void setCenter(int x, int y){
		center[0] = x;
		center[1] = y;
	}
	
	public void setPoints(List<int[]> points, Map<Long, Cell> allCells){
		int x = center[0];
		int y = center[1];
		cells.clear();
		for (int[] point : points) {
			int capacity = point[2];
			int size = 10;
			if (type.equals(TYPE.SQUARE)){
				capacity = 1;
			}
			Cell c = new Cell(type, x + point[0], y + point[1], capacity, size, this);
			if (allCells.putIfAbsent(c.getLocationKey(), c) == null){
				addCell(c);
			}
		}
		// for reloading typologies
		colonize(occupation);
	}
	
	/** set Attraction and clusterCapacity;
	 * 
	 */
	void init() {
		this.capacity = 0;

		int sum_x = 0;
		int sum_y = 0;

		// loop for each cell in cluster
		for (Cell c: cells){
			sum_x += c.getX();
			sum_y += c.getY();
			capacity += c.getCapacity();
			if (c.getCapacity() > maxHeight){
				maxHeight = c.getCapacity();
			}
		}

		// centroid calculation - attraction point
		this.attractor = new Vec3(sum_x / cells.size(), sum_y / cells.size(), 0);
	}
	
	public IMesh getCapacityMesh(){
		List<Vec3> lines = new LinkedList<>();
		cells.stream()
			.map(cell -> cell.getRelativeOutline())
			.flatMap(List::stream)
			.collect(Collectors.toSet())
			.stream().forEach(pair -> {
				lines.add(pair.first);
				lines.add(pair.second);
		});
		return MeshUtilities.createLines(lines, new LineMaterial(RGBA.BLACK).setWidth(type.represent().getLineWidth()));
	}
	
	public Vec3 getAttractionPoint(){
		return attractor;
	}
 
	/**IMPORTANT: Y coordinates inverted because of Processing having 0,0 
	 * at top left corner
	 * @return
	 */
	public List<List<List<double[]>>> getSurfacePolygons(){
		List<List<List<double[]>>> faces = new LinkedList<>();
		List<double[]> footprint = getFootprint(); //.reduceToCorners();
		if (footprint == null) return null;
		List<double[]> roof = footprint.stream()
				.map(p -> new double[]{p[0], p[1], maxHeight})
				.collect(Collectors.toList()); 
		faces.add(wrap(footprint));
		for (int i = 1; i < footprint.size(); i++){
			List<double[]> side = new LinkedList<>();
			side.add(footprint.get(i - 1));
			side.add(footprint.get(i));
			side.add(roof.get(i));
			side.add(roof.get(i - 1));
			faces.add(wrap(side));
		}
		faces.add(wrap(roof));
		return faces;
	}
	
	/**Using JTS to unify the cell's footprints to a cluster footprint.
	 * IMPORTANT: invert Y coordinates due to processing having 0,0 at the top left corner.
	 * @return
	 */
	public List<double[]> getFootprint(){
		Geometry g = CascadedPolygonUnion.union(cells.stream()
				.map(c -> new Polygon(new LinearRing(new CoordinateArraySequence(
						c.getFootPrint().stream().map(i -> new Coordinate(i[0], -i[1])).toArray(Coordinate[]::new)), gf), null, gf))
				.collect(Collectors.toList()))
				.reverse();
		if (g != null && g.getGeometryType() == "Polygon") {
			Polygon p = (Polygon) g;
			return Arrays.stream(p.getExteriorRing().getCoordinates())
					.map(c -> new double[]{c.x, c.y, 0})
					.collect(Collectors.toList());
		} 
		return null;
	}
	
	private List<List<double[]>> wrap(List<double[]> o){
		List<List<double[]>> l = new LinkedList<>();
		l.add(o);
		return l;
	}

	boolean receive(Agent agent, Cell c) {
		if (!isFull()) {
			// colonize cluster's cells
			int remainingParticipants = colonize(agent.getParticipants());
			if (remainingParticipants > 0) {
				// now the cluster is full
				this.occupation = capacity;
				agent.setParticipants(remainingParticipants);
			} else {
				// increase cluster occupation
				this.occupation += agent.getParticipants();
				return true;
			}
		}
		return false;
	}

	public int colonize(int colonize_by) {
		int rest = colonize_by;
		for (Cell cell: cells) {
			rest = cell.colonize(rest);
			if (rest <= 0)
				break;
		}
		return rest;
	}

	public boolean isFull() {
		return occupation >= capacity;
	}

	public void empty() {
		for (int i = 0; i < cells.size(); i++) {
			cells.get(i).empty();
		}
		this.occupation = 0;
	}

	public void fill() {
		// fill empty cells gradually (if empty add agent energy until run out of it)
		for (int i = 0; i < cells.size(); i++) {
			// if cell is not full, colonize it
			cells.get(i).fill();
		}
		this.occupation = capacity;

		// occupancy ratio (if capcaity is full - ratio 1:1 - remove attraction)
		// if existing structure > and capacity is fulfilled > create new
		// extended cluster
	}

	public int getLuciID() {
		return luciID ;
	}
	
	public void setLuciID(int id){
		luciID = id;
	}

	public void maxAgentDistance(float distance) {
		if (distance > maxAgentDistance) maxAgentDistance = distance;
	}
	
	public void resetMaxAgentDistance(){
		maxAgentDistance = 0;
	}
	
	public IMesh getMaxAgentDistanceCircle(){
		if (disk == null)
			disk = MeshUtilities.createDisk(new ColorMaterial(new RGBA(0xE8C365FF)), 9);
		disk.setTransform(Mat4.scale(maxAgentDistance));
		return disk;
	}

	public void setOccupation(int o) {
		occupation = o;
	}
}