import java.io.File;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;

public class Stitch_PILATUS_Images implements PlugIn {

	double globalMin2q = 0;
	double globalMax2q = 0;
	double step2q;
	int h_new;
	String prefix = "";

	public void run(String arg) {

		List<Integer> arrAngles;
		try {
			arrAngles = XRDCommon.getStitchAngles();
			if (arrAngles.size() == 0)
				return;
		} catch (Exception e) {
			e.printStackTrace();
			IJ.error(e.getMessage());
			return;
		}

		XRDProps prop = XRDCommon.ReadProps();

		Double[] arrMin2q = new Double[arrAngles.size()];
		Double[] arrMax2q = new Double[arrAngles.size()];
		ImagePlus[] arrImpIP = new ImagePlus[arrAngles.size()];

		String dir = "";
		int w;
		int h;
		for (int i = 0; i < arrAngles.size(); i++) {

			OpenDialog od = new OpenDialog(
					"Select image #" + String.valueOf(i + 1) + " (" + String.valueOf(arrAngles.get(i)) + " degree).",
					prop.defaultDir, "");

			dir = od.getDirectory();
			prop.defaultDir = dir;
			String filepath = od.getPath();

			if (filepath == null)
				return;

			ImagePlus imp = XRDCommon.CheckTiff32BPP(filepath);
			if (imp == null)
				return;

			if (prop.directionInt == 2)
				imp = new ImagePlus(imp.getTitle(), imp.getProcessor().rotateRight());

			double angle = arrAngles.get(i);

			w = imp.getProcessor().getWidth();
			h = imp.getProcessor().getHeight();
			h_new = h;

			arrMin2q[i] = XRDCommon.getMin2q(angle, w, prop);
			arrMax2q[i] = XRDCommon.getMax2q(angle, w, prop);

			step2q = (arrMax2q[i] - arrMin2q[i]) / w;

			arrImpIP[i] = XRDCommon.calcIP(imp, step2q, angle, prop);

			if (i == 0) {
				globalMin2q = arrMin2q[i];
				globalMax2q = arrMax2q[i];
				prefix = imp.getTitle().replace(".tif", "");
			} else {
				globalMin2q = arrMin2q[i] < globalMin2q ? arrMin2q[i] : globalMin2q;
				globalMax2q = arrMax2q[i] > globalMax2q ? arrMax2q[i] : globalMax2q;
				if (arrMin2q[i] > arrMax2q[i - 1]) {
					IJ.error("Missing angles in between images #" + String.valueOf(i - 1) + " and #" + String.valueOf(i)
							+ ".");
					return;
				}
			}
		}

		XRDCommon.WriteProps(prop);

		int w_new = (int) Math.round((globalMax2q - globalMin2q) / step2q);
		double globalStep2q = (globalMax2q - globalMin2q) / w_new;

		ImagePlus imp_sti = NewImage.createImage(prefix + "_Stitched", w_new, h_new, 1, 32, NewImage.FILL_BLACK);

		double x2q;
		double xi;
		int idxToUse;
		double blend;
		for (int i = 0; i < w_new; i++) {
			idxToUse = 0;
			blend = 0;
			if (prop.directionInt % 2 == 0) {
				x2q = globalMin2q + globalStep2q * i;
			} else {
				x2q = globalMax2q - globalStep2q * i;
			}
			while (idxToUse < arrAngles.size() - 1 && x2q > (arrMax2q[idxToUse] + arrMin2q[idxToUse + 1]) / 2) {
				idxToUse += 1;
			}
			if (idxToUse < arrAngles.size() - 1) {
				if (x2q > arrMin2q[idxToUse + 1]) {
					blend = (x2q - arrMin2q[idxToUse + 1]) / (arrMax2q[idxToUse] - arrMin2q[idxToUse + 1]);
				}
			}

			if (prop.directionInt % 2 == 0) {
				xi = (globalMin2q + globalStep2q * i - arrMin2q[idxToUse]) / step2q;
			} else {
				xi = (arrMax2q[idxToUse] - (globalMax2q - globalStep2q * i)) / step2q;
			}
			for (int j = 0; j < h_new; j++) {
				if (!prop.roundBool) {
					if (blend > 0) {

					}
					imp_sti.getProcessor().putPixelValue(i, j,
							arrImpIP[idxToUse].getProcessor().getInterpolatedValue(xi, j)); // [A]
				} else {
					imp_sti.getProcessor().putPixel(i, j,
							arrImpIP[idxToUse].getProcessor().getPixel((int) Math.round(xi), j));
					// imp_sti.getProcessor().putPixel(i,j,arrIdIP[idxToUse].getProcessor().getPixelInterpolated(xi,
					// j)); // [B]
				}
			}
		}

		FileSaver fs = new FileSaver(imp_sti);
		fs.saveAsTiff(dir + File.separator + prefix + "_Stitched.tif");
		(new ContrastEnhancer()).stretchHistogram(imp_sti, 0.1);
		imp_sti.show();
		ImagePlus imp_calc2q = XRDCommon.calc2q(imp_sti, globalMin2q, globalStep2q, prop);
		if (prop.debugBool)
			imp_calc2q.show();
		XRDCommon.plot2q(imp_calc2q, globalMin2q, globalStep2q, dir + File.separator, prefix + "_Stitched", true);
	}
}
