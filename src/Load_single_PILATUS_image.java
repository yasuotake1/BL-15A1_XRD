import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;

public class Load_single_PILATUS_image implements PlugIn {

	double step2q;
	int h_g;

	public void run(String arg) {
		XRDProps prop = XRDCommon.ReadProps();
		OpenDialog od = new OpenDialog("Select a PILATUS image.", prop.defaultDir, "");
		if (od.getDirectory() == null)
			return;

		String dir = od.getDirectory();
		String filepath = od.getPath();
		String name = od.getFileName();

		ImagePlus imp = XRDCommon.CheckTiff32BPP(filepath);
		if (imp == null)
			return;

		prop.defaultDir = dir;
		XRDCommon.WriteProps(prop);

		if (prop.directionInt == 2)
			imp = new ImagePlus("RotR_" + imp.getTitle(), imp.getProcessor().rotateRight());

		imp.show();
		imp.setRoi(0, 0, imp.getWidth(), imp.getHeight());
		(new ContrastEnhancer()).stretchHistogram(imp, 0.1);
		imp.updateAndDraw();

		double angle;
		try {
			angle = XRDCommon.getAngle();
		} catch (Exception e) {
			IJ.error(e.getMessage());
			return;
		}
		int w = imp.getProcessor().getWidth();
		double min2q = XRDCommon.getMin2q(angle, w, prop);
		double max2q = XRDCommon.getMax2q(angle, w, prop);
		double step2q = (max2q - min2q) / w;

		ImagePlus img_IP = XRDCommon.calcIP(imp, step2q, angle, prop);
		if (prop.debugBool)
			img_IP.show();

		ImagePlus img_2q = XRDCommon.calc2q(img_IP, min2q, step2q, prop);
		if (prop.debugBool)
			img_2q.show();

		XRDCommon.plot2q(img_2q, min2q, step2q, dir + File.separator, name.replace(".tif", ""), true);

	}
}
