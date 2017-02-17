package emcity;
public interface Colonizeable {
	public boolean isFull();
	public void empty();
	public void fill();
	public int colonize(int agent);
}