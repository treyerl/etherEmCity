package emcity;

import ch.fhnw.ether.scene.mesh.material.LineMaterial;
import ch.fhnw.util.color.RGBA;

/**@see MultiLineString
*
*/
class Roads extends MultiLineString{
	
	public LineMaterial getLineMaterial(){
		return new LineMaterial(RGBA.DARK_GRAY);
	}
}