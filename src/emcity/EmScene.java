package emcity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.render.IRenderManager;
import ch.fhnw.ether.scene.I3DObject;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.mesh.IMesh;

public class EmScene implements IScene {
	
	private final IController controller;

	private final Set<I3DObject> objects = new HashSet<>();

	public EmScene(IController controller) {
		this.controller = controller;
	}
	
	@Override
	public void add3DObject(I3DObject object) {
		if(object == null) return;
		/* contains test is performed in DefaultRenderManager */
		if (objects.contains(object))
			return;
//			throw new IllegalArgumentException("object already in scene: " + object); 

		IRenderManager rm = controller.getRenderManager();
		if (object instanceof ILight)
			rm.addLight((ILight) object);
		else if (object instanceof IMesh)
			rm.addMesh((IMesh) object);
		objects.add(object);
	}

	@Override
	public void remove3DObject(I3DObject object) {
		if (!objects.contains(object))
//			throw new IllegalArgumentException("object not in scene: " + object);
			return;

		IRenderManager rm = controller.getRenderManager();
		if (object instanceof ILight)
			rm.removeLight((ILight) object);
		else if (object instanceof IMesh)
			rm.removeMesh((IMesh) object);
		objects.remove(object);
	}


	@Override
	public Collection<I3DObject> get3DObjects() {
		return objects;
	}

}
