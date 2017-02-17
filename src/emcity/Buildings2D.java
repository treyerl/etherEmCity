package emcity;
import ch.fhnw.ether.scene.mesh.material.LineMaterial;
import ch.fhnw.util.color.RGBA;

/**@see MultiLineString
 *
 */
class Buildings2D extends MultiLineString{
	
	public LineMaterial getLineMaterial(){
		return new LineMaterial(RGBA.GRAY);
	}
}