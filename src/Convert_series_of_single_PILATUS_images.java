import java.io.File;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;

public class Convert_series_of_single_PILATUS_images implements PlugIn {
	public void run(String arg) {
		XRDProps prop = XRDCommon.ReadProps();
		String dirImg = "";
		ImagePlus imp, imp_IP, imp_2q;

		DirectoryChooser dc = new DirectoryChooser("Choose directory for PILATUS images...");
		if (dc.getDirectory() == null)
			return;

		dirImg = dc.getDirectory() + File.separator;
		prop.defaultDir = dirImg;
		XRDCommon.WriteProps(prop);

		File[] listAll = new File(dirImg).listFiles(new TifFilter());

		if (listAll.length == 0) {
			IJ.error("No TIFF File is Selected.");
			return;
		}
		Arrays.sort(listAll);

		double angle;
		try {
			angle = XRDCommon.getAngle();
		} catch (Exception e) {
			IJ.error(e.getMessage());
			return;
		}
		for (int i = 0; i < listAll.length; i++) {
			imp = XRDCommon.CheckTiff32BPP(listAll[i].toString());
			if (imp != null) {
				if (prop.directionInt == 2)
					imp = new ImagePlus("RotR_" + imp.getTitle(), imp.getProcessor().rotateRight());

				int w = imp.getProcessor().getWidth();
				double min2q = XRDCommon.getMin2q(angle, w, prop);
				double max2q = XRDCommon.getMax2q(angle, w, prop);
				double step2q = (max2q - min2q) / w;

				imp_IP = XRDCommon.calcIP(imp, step2q, angle, prop);
				imp_2q = XRDCommon.calc2q(imp_IP, min2q, step2q, prop);
				XRDCommon.plot2q(imp_2q, min2q, step2q, dirImg, imp.getTitle().replace(".tif", ""), false);
			}
		}
	}
}
