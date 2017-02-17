package emcity;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import ch.fhnw.ether.render.gl.FloatArrayBuffer;
import ch.fhnw.ether.render.gl.GLObject;
import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMutableMesh;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.IMaterial;
import ch.fhnw.ether.scene.mesh.material.LineMaterial;
import ch.fhnw.util.BufferUtilities;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.LineString;

public class FiFoLineString extends LineString {
	private class DynLineStringMesh extends DefaultMesh implements IMutableMesh{
		
		
		public DynLineStringMesh(Primitive type, IMaterial material, IGeometry geometry) {
			super(type, material, geometry, Queue.DEPTH, Flag.DONT_CAST_SHADOW, Flag.DONT_CULL_FACE);
		}
		
		public DynLineStringMesh(Primitive type, IMaterial material, IGeometry geometry, Queue queue, Flag flag, Flag...flags) {
			super(type, material, geometry, queue, flag, flags);
		}

		@Override
		public float[][] getUpdatedTransformedGeometryData() {
			return new float[][]{Vec3.toArray(newPoints)};
		}

		@Override
		public FloatArrayBuffer getFloatArrayBuffer() {
			return new FloatArrayBuffer(){
				
				@Override
				public void load(FloatBuffer data){
					if (vbo == null){
						vbo = new GLObject(Type.BUFFER);
						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.getId());
						GL15.glBufferData(GL15.GL_ARRAY_BUFFER, ByteBuffer.allocateDirect(CAPACITY), GL15.GL_STATIC_DRAW);
					} else GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.getId());
					
					if (data != null && data.limit() != 0) {
						size = data.limit();
						data.rewind();
						GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, data);
					} else {
						size = 0;
						GL15.glBufferData(GL15.GL_ARRAY_BUFFER, BufferUtilities.EMPTY_FLOAT_BUFFER, GL15.GL_STATIC_DRAW);
					}
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
				}
			};
		}
		
		@Override
		public void clearUpdates() {
			newPoints.clear();
		}
		
		@Override
		public void draw(int mode, int numVertices){
			int s = START, e = END;
			if (END < START){
				GL11.glDrawArrays(mode, s, max);
				s = 0;
			}
			GL11.glDrawArrays(mode, s, e);
			clearUpdates();
		}
	}
	
	private IMutableMesh mesh;
	private LinkedList<Vec3> newPoints = new LinkedList<>();
	private static int max = 1000;
	private int floatToByte = 4 * 3;
	private int START = 0, END = points.size(), CAPACITY = max * floatToByte;

	public static int getMaxLength() {
		return max;
	}

	public FiFoLineString put(Vec3 p){
		points.add(p);
		newPoints.add(p);
		if (mesh != null) mesh.getUpdater().request();
		return this;
	}
	
	public Vec3 get(){
		if (mesh != null) mesh.getUpdater().request();
		return points.removeFirst();
	}
	
	@SuppressWarnings("unchecked")
	public LinkedList<Vec3> get(int n){
		if (n < points.size()){
			LinkedList<Vec3> l = new LinkedList<>();
			int c = 0, np = points.size() - newPoints.size();
			while (c++ < n) l.add(points.removeFirst());
			while (np++ < n) newPoints.removeFirst();
			if (mesh != null) mesh.getUpdater().request();
			return l;
		} 
		return (LinkedList<Vec3>) points.clone();
	}
	
	public IMutableMesh getMesh(){
		if (mesh == null){
			IGeometry g = DefaultGeometry.createV(Vec3.toArray(points));
			mesh = new DynLineStringMesh(Primitive.LINE_STRIP, new LineMaterial(RGBA.RED), g);
		} else if (newPoints.size() > 0){
			// TODO: 
		}
		return mesh;
	}

}
