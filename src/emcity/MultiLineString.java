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
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.LineString;

/**A class holding a list of toxi.geom.Spline3D which it is able to draw to a PApplet canvas
 * by connecting their respective points with a PApplet.line()
 * @author Lukas Treyer
 *
 */
public class MultiLineString {
	
	List<LineString> lineStrings;
	
	/**Set the splines
	 * @param lineStrings List&lt;LineString&gt;
	 */
	public void setLineStrings(List<LineString> lineStrings){
		this.lineStrings = lineStrings;
	}
	
	/**Draw the connecting straight lines between spline points.
	 * @param p PApplet - Processing Applet
	 */
	public List<Vec3> getLines(){
		List<Vec3> lines = new LinkedList<>();
		for (LineString ls: lineStrings) 
			lines.addAll(ls.getLines());
		return lines;
	}
	
	public LineMaterial getLineMaterial(){
		return new LineMaterial(RGBA.BLACK).setWidth(1);
	}
	
	public IMesh getMesh(){
		IGeometry g = DefaultGeometry.createV(Vec3.toArray(getLines()));
		return new DefaultMesh(Primitive.LINES, getLineMaterial(), g, Queue.DEPTH, Flag.DONT_CAST_SHADOW);
	}
}
