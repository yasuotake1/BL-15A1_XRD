import ij.plugin.PlugIn;
import java.util.List;

public class XRDProps implements PlugIn{
	public float pX;
	public float pY;
	public float y0;
	public float cAngle;
	public float cDist;
	public int directionInt;
	public boolean roundBool;
	public boolean debugBool;
	public boolean cacheBool;
	public boolean flipHor;
	public boolean flipVer;
	public List<Integer> arrAngles;
	public String defaultDir;

	public void run(String arg){
	}
}
