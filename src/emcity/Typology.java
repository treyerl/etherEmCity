package emcity;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class Typology {
	
	List<int[]> points;
	List<Cluster> usingMe = new LinkedList<>();
	
	public Typology(List<int[]> points){
		this.points = points;
	}
	
	public boolean listsOfArraysEqual(List<int[]>l1, List<int[]> l2){
		if (l1 == null || l2 == null) return false;
		if (l1.size() != l2.size()) return false;
		for (int i = 0; i < l1.size(); i++){
			int[] i1 = l1.get(i), i2 = l2.get(i);
			if (i1.length != i2.length) return false;
			for (int j = 0; j < i1.length; j++)
				if (i1[j] != i2[j]) return false;
		}
		return true;
	}
	
	public boolean setPoints(List<int[]> p, Map<Long, Cell> cells, List<Cluster> updated) {
		if (!listsOfArraysEqual(p, points)){
			for (Cluster cl: usingMe){
				for (Cell cell: cl.cells) cells.remove(cell);
				cl.setPoints(p, cells);
				updated.add(cl);
			}
			points = p;
			return true;
		}
		return false;
	}

	public Cluster createVolume(float x, float y, Map<Long, Cell> cells, int type) {
		// round agent position (/10 + 5)
		int a_x = (int) Math.floor(x * 0.1) * 10 + 5;
		int a_y = (int) Math.floor(y * 0.1) * 10 + 5;
		

		if (points.size() > 0){
			Cluster cluster = Cluster.create(type);
			cluster.setCenter(a_x, a_y);
			cluster.setPoints(points, cells);
			if (cluster.cellCount() > 0){
				cluster.init();
				usingMe.add(cluster);
				return cluster;
			}
		}
		return null;
	}
}