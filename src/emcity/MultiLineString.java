package emcity;
import java.util.LinkedList;
import java.util.List;

import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.LineMaterial;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.LineStrip;
import emcity.EmCity.REPRESENTATION;

/**A class holding a list of toxi.geom.Spline3D which it is able to draw to a PApplet canvas
 * by connecting their respective points with a PApplet.line()
 * @author Lukas Treyer
 *
 */
public class MultiLineString {
	
	List<LineStrip> lineStrings;
	
	/**Set the splines
	 * @param lineStrings List&lt;LineString&gt;
	 */
	public void setLineStrings(List<LineStrip> lineStrings){
		this.lineStrings = lineStrings;
	}
	
	/**Draw the connecting straight lines between spline points.
	 * @param p PApplet - Processing Applet
	 */
	public List<Vec3> getLines(){
		List<Vec3> lines = new LinkedList<>();
		for (LineStrip ls: lineStrings) 
			lines.addAll(ls.getLines());
		return lines;
	}
	
	public IMesh getMesh(REPRESENTATION r){
		IGeometry g = DefaultGeometry.createV(Vec3.toArray(getLines()));
		IMesh m = new DefaultMesh(Primitive.LINES, new LineMaterial(r.getColor()).setWidth(r.getLineWidth()), g, Queue.TRANSPARENCY, Flag.DONT_CAST_SHADOW, Flag.DONT_CULL_FACE);
		m.getAttributes().put(REPRESENTATION.key(), r);
		m.setName(r.name());
		return m;
	}
}
