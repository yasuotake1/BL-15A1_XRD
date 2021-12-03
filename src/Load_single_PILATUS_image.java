import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;

public class Load_single_PILATUS_image implements PlugIn {

	double step2q;
	int h_g;
	
	public void run(String arg){
		double angle;
		XRDProps prop = Common.ReadProps();

		OpenDialog od = new OpenDialog("Select a PILATUS image.", prop.defaultDir, "");
		if(od.getDirectory() == null) return;

		String dir = od.getDirectory();
		String filepath = od.getPath();
		String name = od.getFileName();
		
		ImagePlus imp = Common.CheckTiff32BPP(filepath);
		if(imp == null )return;

		// �ݒ�l���擾
		prop.defaultDir = dir;
		Common.WriteProps(prop);
		
		if(prop.directionInt == 2)
			imp = new ImagePlus("RotR_" + imp.getTitle(), imp.getProcessor().rotateRight());
		
		imp.show();
		imp.setRoi(0,0,imp.getWidth(), imp.getHeight());
		(new ContrastEnhancer()).stretchHistogram(imp,0.1);

		imp.updateAndDraw();

		int w = imp.getProcessor().getWidth();

		try{
			angle = Common.getAngle();
		} catch (Exception e) {
			e.printStackTrace();
			IJ.error(e.getMessage());
			return;
		}
		
		double min2q = Common.getMin2q(angle, w, prop);
		double max2q = Common.getMax2q(angle, w, prop);

		double step2q = (max2q - min2q) / w;

		ImagePlus img_IP = Common.calcIP(imp, step2q, angle, prop);
		if(prop.debugBool)
			img_IP.show();
		
		ImagePlus img_2q = Common.calc2q(img_IP, min2q, step2q, prop);
		if(prop.debugBool)
			img_2q.show();

		Common.plot2q(img_2q, min2q, step2q, dir + File.separator, name.replace(".tif", ""), true);

	}
}
