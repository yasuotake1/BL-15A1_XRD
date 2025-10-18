import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.plugin.PlugIn;

public class Batch_job_2__Stitch_images implements PlugIn {

	double globalMin2q = 0, globalMax2q = 0;
	double globalStep2q;
	double step2q;
	int h_new;

	public void run(String arg) {

		XRDProps prop = XRDCommon.ReadProps();

		DirectoryChooser dc = new DirectoryChooser("Choose directory for normalized images...");
		if (dc.getDirectory() == null)
			return;

		String dirImg = dc.getDirectory() + File.separator;

		File[] listAll = new File(dirImg).listFiles(new NormalizeFilter());

		if (listAll.length == 0) {
			IJ.error("No TIFF File is Selected.");
			return;
		}

		String filename = listAll[listAll.length - 1].getName();
		String strPrefix = filename.substring(0, filename.lastIndexOf("_"));
		Arrays.sort(listAll);

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

		int[] arrStartIdx = new int[arrAngles.size()];
		int[] arrEndIdx = new int[arrAngles.size()];

		int NumImg = (int) Math.floor(listAll.length / arrAngles.size());

		GenericDialog gd1 = new GenericDialog("Image index assignment");
		gd1.addStringField("File prefix: ", strPrefix);
		for (int i = 0; i < arrAngles.size(); i++) {
			gd1.addNumericField("First index of " + String.valueOf(arrAngles.get(i)) + " degree: ", i * NumImg, 0);
			gd1.addNumericField("Last index of " + String.valueOf(arrAngles.get(i)) + " degree: ", (i + 1) * NumImg - 1,
					0);
		}
		gd1.showDialog();
		if (gd1.wasCanceled())
			return;

		strPrefix = gd1.getNextString();
		for (int i = 0; i < arrAngles.size(); i++) {
			arrStartIdx[i] = (int) gd1.getNextNumber();
			arrEndIdx[i] = (int) gd1.getNextNumber();
			if (arrStartIdx[i] < 0 || arrStartIdx[i] >= arrEndIdx[i] || Math.round(arrStartIdx[i]) != arrStartIdx[i]
					|| Math.round(arrEndIdx[i]) != arrEndIdx[i]) {
				IJ.error("Invalid index assignment!");
				return;
			}
			if (i == 0) {
				NumImg = (int) Math.floor(arrEndIdx[i] - arrStartIdx[i] + 1);
			} else {
				if (NumImg != arrEndIdx[i] - arrStartIdx[i] + 1) {
					IJ.error("Invalid index assignment!");
					return;
				}
			}
		}

		File target = new File(dirImg + "stitched");
		if (!target.exists()) {
			if (!target.mkdir()) {
				IJ.error("Unable to create directory!");
				return;
			}
		}

		Double[] arrMin2q = new Double[arrAngles.size()];
		Double[] arrMax2q = new Double[arrAngles.size()];
		ImagePlus[] arrIdIP = new ImagePlus[arrAngles.size()];

		if (prop.cacheBool) {
			for (int n = 0; n < listAll.length; n++) {
				ImagePlus impWork = new ImagePlus(listAll[n].getPath());
				IJ.showProgress((n + 1), listAll.length);
				IJ.showStatus("Reading files... : " + impWork.getTitle());
			}
		}
		for (int i = 0; i < NumImg; i++) {

			String strIdx = String.format("%05d", (int) Math.floor(arrStartIdx[0]) + i);

			String strName0 = strPrefix + "_" + strIdx + "stitch";

			for (int j = 0; j < arrAngles.size(); j++) {
				strIdx = String.format("%05d", (int) Math.floor(arrStartIdx[j] + i));

				String path = dirImg + File.separator + strPrefix + "_" + strIdx + "norm.tif";

				double angle = arrAngles.get(j);

				ImagePlus impRaw = new ImagePlus(path);
				if (prop.directionInt == 2)
					impRaw = new ImagePlus(impRaw.getTitle(), impRaw.getProcessor().rotateRight());
				int w = impRaw.getProcessor().getWidth();
				int h = impRaw.getProcessor().getHeight();
				h_new = h;

				arrMin2q[j] = angle - Math.atan(prop.pX * w / 2 / prop.cDist) / Math.PI * 180;
				arrMax2q[j] = angle + Math.atan(prop.pX * w / 2 / prop.cDist) / Math.PI * 180;

				step2q = (arrMax2q[j] - arrMin2q[j]) / w;

				arrIdIP[j] = XRDCommon.calcIP(impRaw, step2q, angle, prop);

				if (j == 0) {
					globalMin2q = arrMin2q[j];
					globalMax2q = arrMax2q[j];
				} else {
					globalMin2q = arrMin2q[j] < globalMin2q ? arrMin2q[j] : globalMin2q;
					globalMax2q = arrMax2q[j] > globalMax2q ? arrMax2q[j] : globalMax2q;
				}
			}

			int w_new = (int) Math.round((globalMax2q - globalMin2q) / step2q);
			globalStep2q = (globalMax2q - globalMin2q) / w_new;
			ImagePlus imp_sti = NewImage.createImage(strName0, w_new, h_new, 1, 32, NewImage.FILL_BLACK);

			int idxToUse;
			double x2q, xj;
			for (int j = 0; j < w_new; j++) {
				idxToUse = 0;
				if (prop.directionInt % 2 == 0) {
					x2q = globalMin2q + globalStep2q * j;
				} else {
					x2q = globalMax2q - globalStep2q * j;
				}
				while (idxToUse < arrAngles.size() - 1 && x2q > (arrMax2q[idxToUse] + arrMin2q[idxToUse + 1]) / 2) {
					idxToUse += 1;
				}
				
				if (prop.directionInt % 2 == 0) {
					xj = (globalMin2q + globalStep2q * j - arrMin2q[idxToUse]) / step2q;
				} else {
					xj = (arrMax2q[idxToUse] - (globalMax2q - globalStep2q * j)) / step2q;
				}
				for (int k = 0; k < h_new; k++) {
					if (!prop.roundBool) {
						imp_sti.getProcessor().putPixelValue(j, k,
								arrIdIP[idxToUse].getProcessor().getInterpolatedValue(xj, k)); // [A]
					} else {
						imp_sti.getProcessor().putPixel(j, k,
								arrIdIP[idxToUse].getProcessor().getPixel((int) Math.round(xj), k));
						// imp_sti.getProcessor().putPixel(j,k,arrIdIP[idxToUse].getProcessor().getPixelInterpolated(xj,
						// k)); // [B]
					}
				}
			}
			new FileSaver(imp_sti).saveAsTiff(dirImg + "stitched" + File.separator + strName0 + ".tif");

			ImagePlus imp2q = XRDCommon.calc2q(imp_sti, globalMin2q, globalStep2q, prop);

			XRDCommon.plot2q(imp2q, globalMin2q, globalStep2q, dirImg + "stitched" + File.separator, strName0, false);

			IJ.showProgress((i + 1), NumImg);
			IJ.showStatus(strName0 + ".tif");
		}

		IJ.showStatus("Finished image stitching.");

		File file = new File(dirImg + "stitched" + File.separator + "log_stitched.txt");
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			for (int i = 0; i < arrAngles.size(); i++) {
				pw.println("File index " + String.valueOf(arrStartIdx[i]) + "-" + String.valueOf(arrEndIdx[i])
						+ " for camera angle " + String.valueOf(arrAngles.get(i)) + " degree.");
			}
			pw.println("globalMax2q = " + String.valueOf(globalMax2q));
			pw.println("globalMin2q = " + String.valueOf(globalMin2q));
			pw.println("globalStep2q = " + String.valueOf(globalStep2q));
			pw.println("---Do not edit this file as it is used in the following batch jobs.---");

			pw.close();
		} catch (IOException e) {
			IJ.error(e.getMessage());
			e.printStackTrace();
		}
	}
}
