package emcity.luci;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONObject;

import com.esotericsoftware.minlog.Log;

import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.util.math.Vec3;
import emcity.Cluster;
import emcity.EmCity;
import emcity.MultiLineString;
import emcity.Reader;
import luci.connect.AttachmentAsArray;
import luci.connect.JSON;
import luci.connect.LcRemoteService;
import luci.connect.Message;

public class EmCityLuciService extends LcRemoteService {
	public final String scenarioName = "EmCity"; 
	private int ScID = 0, cameraID;
	boolean didReceiveCameraID = false;
	private EmCity emc;
	public EmCityLuciService(DefaultArgsProcessor ap) {
		super(ap);
	}
	
	public void setEmCity(EmCity emc){
		this.emc = emc;
	}
	
	private void start(){
		emc.start();
	}

	@Override
	public String getDescription() {
		return "Expects an optional typology file (for now; might accept settings in the future).";
	}

	@Override
	protected ResponseHandler newResponseHandler() {
		// TODO Auto-generated method stub
		return new RemoteServiceResponseHandler() {
			Reader r = new Reader();
			@Override
			public void processResult(Message m) {
//				System.out.println(m);
			}
			
			@Override
			public Message implementation(Message input) {
				JSONObject h = input.getHeader();
				
				final CountDownLatch cdl = new CountDownLatch(1);
				final int[] numbers = new int[2];
				JSONObject result = new JSONObject();
				
				if (h.has("typology")){
					AttachmentAsArray t = (AttachmentAsArray) h.get("typology");
					emc.getController().updateTypologies(r.lines(t.getByteBuffer()), (updatedClusters, deletedIDs) -> {
						uploadClusters(updatedClusters, deletedIDs);
						numbers[0] = updatedClusters.size();
						numbers[1] = deletedIDs.size();
						cdl.countDown();
					});
					try {
						cdl.await();
						 result.put("updatedClusters", numbers[0]).put("deletedClusters", numbers[1]);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return new Message(new JSONObject().put("error", e.getMessage()));
					}
				}
				if (h.has("network")){
					JSONObject network = h.getJSONObject("network");
					JSONObject j = network.getJSONObject("streets");
					AttachmentAsArray p = (AttachmentAsArray) j.get("points");
					AttachmentAsArray l = (AttachmentAsArray) j.get("indices");
					emc.getController().updateStreetNetwork(readIndexedPoints(a2l(p),a2l(l),false));
					j = network.getJSONObject("parcels");
					p = (AttachmentAsArray) j.get("points");
					l = (AttachmentAsArray) j.get("indices");
					emc.getController().updateParcels(readIndexedPoints(a2l(p),a2l(l),false));
				}
				return new Message(new JSONObject().put("result", result));
			}
			
			public MultiLineString readIndexedPoints(Stream<String> pointLines, Stream<String> indexLines, boolean closed){
				return new MultiLineString().setLineStrings(r.lineStrings(r.points(pointLines), indexLines, closed));
			}
			
			private Stream<String> a2l(AttachmentAsArray a){
				return r.lines(a.getByteBuffer());
			}
		};
	}
	
	

	@Override
	protected JSONObject exampleCall() {
		return new JSONObject("{'run':'EmCity'}");
	}

	@Override
	protected JSONObject specifyInputs() {
		return new JSONObject("{'run':'EmCity','OPT typology':'attachment',"
				+ "'OPT network':{"
				+ "		'streets':{'points':'attachment','indices':'attachment'},"
				+ "		'parcels':{'points':'attachment','indices':'attachment'},"
				+ "}}");
	}

	@Override
	protected JSONObject specifyOutputs() {
		return new JSONObject("{'XOR result':{'success':'string'}, 'XOR error':'string'}");
	}
	
	public static void main( String[] args ){
		Log.set(Log.LEVEL_TRACE);
    	DefaultArgsProcessor asp = new DefaultArgsProcessor(args);
		System.out.println("EmCity registering at "+asp.getHostname()+":"+asp.getPort());
		EmCityLuciService em = new EmCityLuciService(asp);
		new Thread(em).start();
		em.connect(asp.getHostname(), asp.getPort());
		em.setEmCity((new EmCity(em)));
		em.start();
    }
	
	public void createScenario(Consumer<Integer> onScenarioCreated){
		sendAndReceive(
				new Message(new JSONObject().put("run", "scenario.GetList")), 
				new ResponseHandler(){
			@Override
			public void processResult(Message m) {
				JSONObject r = m.getHeader().getJSONObject("result");
				List<Integer> ScIDs = StreamSupport.stream(r.getJSONArray("scenarios").spliterator(), false)
						.filter(o -> ((JSONObject) o).getString("name").equals(scenarioName))
						.map(o -> ((JSONObject) o).getInt("ScID"))
						.collect(Collectors.toList());
				if (ScIDs.size() > 0){
					System.out.printf("deleting %d", ScID);
					sendAndReceive(
						new Message(new JSONObject()
							.put("run", "scenario.Delete")
							.put("ScIDs", ScIDs)
						),
						new ResponseHandler() {
							@Override
							public void processResult(Message m) {
								_createScenario(onScenarioCreated);
							}
							public void processError(Message m) {
								System.err.println(m.getHeader().getString("error"));
								_createScenario(onScenarioCreated);
							}
						}
					);
				} else _createScenario(onScenarioCreated);
			}
		});
	}
	
	private void _createScenario(Consumer<Integer> onScenarioCreated){
		JSONObject request = new JSONObject()
				.put("run", "scenario.Create")
				.put("name", scenarioName);
		sendAndReceive(new Message(request), new ResponseHandler(){
			@Override
			public void processResult(Message m) {
				JSONObject h = m.getHeader();
				ScID = h.getJSONObject("result").getInt("ScID");
				onScenarioCreated.accept(ScID);
			}
		});
	}

	/**Generates GeoJSON geometry
	 * @param clusters
	 * @param deletedIDs
	 */
	public void uploadClusters(List<Cluster> clusters, List<Integer> deletedIDs) {
		final JSONArray features = new JSONArray();
		final JSONObject geojson = new JSONObject()
				.put("type", "FeatureCollection")
				.put("features", features);
		clusters.stream().forEach(cl -> features.put(new JSONObject()
				.put("type", "Feature")
				.put("properties", new JSONObject().put("geomID", cl.getLuciID()))
				.put("geometry", new JSONObject()
						.put("type", "MultiPolygon")
						.put("coordinates", cl.getSurfacePolygons()))
				));
		if (deletedIDs != null){
			features.put(new JSONObject()
					.put("type", "Feature")
					.put("properties", new JSONObject().put("deleted_geomIDs", deletedIDs)));
		}
		Message m = new Message(new JSONObject()
				.put("run", "scenario.geojson.Update")
				.put("ScID", ScID)
				.put("geometry_input", new JSONObject()
						.put("format", "geojson")
						.put("geometry", geojson))
				);
		sendAndReceive(m, new ResponseHandler() {
			@Override
			public void processResult(Message m) {
				List<Integer> newIDs = JSON.ArrayToIntList(m.getHeader()
						.getJSONObject("result").getJSONArray("newIDs"));
				for (int i = 0; i < newIDs.size(); i++){
					clusters.get(i).setLuciID(newIDs.get(i));
				}
			}
		});
	}
	
	public void publishCamera(ICamera cam){
		JSONObject jCam = camToJSONObject(cam);
		if (!didReceiveCameraID){
			sendAndReceive(new Message(new JSONObject().put("run", "scenario.camera.List")), 
				new ResponseHandler() {
				@Override
				public void processResult(Message m) {
					List<Integer> ids = JSON.ArrayToIntList(m.getHeader().getJSONObject("result")
							.getJSONArray("cameraIDs"));
					if (ids.size() > 0){
						cameraID = ids.get(0);
						didReceiveCameraID = true;
						send(new Message(jCam
								.put("run", "scenario.camera.Update")
								.put("cameraID", cameraID)));
					} else {
						sendAndReceive(new Message(jCam
								.put("run", "scenario.camera.Create")
								.put("scale", 1)),
								new ResponseHandler() {
									@Override
									public void processResult(Message m) {
										cameraID = m.getHeader().getJSONObject("result").getInt("cameraID");
									}
								});
					}
				}
			});
		} else {
			send(new Message(jCam
					.put("run", "scenario.camera.Update")
					.put("cameraID", cameraID)));
		}
	}
	
	public JSONObject camToJSONObject(ICamera cam){
		Vec3 lookAt = cam.getTarget();
		Vec3 camUp = cam.getUp();
		Vec3 pos = cam.getPosition();
		return new JSONObject()
				.put("lookAt", new JSONObject()
						.put("x",  f(lookAt.x))
						.put("y",  f(lookAt.y))
						.put("z",  f(lookAt.z)))
				.put("cameraUp", new JSONObject()
						.put("x",  f(camUp.x))
						.put("y",  f(camUp.y))
						.put("z",  f(camUp.z)))
				.put("location", new JSONObject()
						.put("x",  f(pos.x))
						.put("y",  f(pos.y))
						.put("z",  f(pos.z)))
				.put("fov", cam.getFov());
	}
	
	private float f(float f){
		if (f == -0) return 0.0f;
		return f;
	}
}
