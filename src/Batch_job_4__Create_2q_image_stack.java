import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.plugin.PlugIn;

public class Batch_job_4__Create_2q_image_stack implements PlugIn {

	double globalMin2q = 0, globalMax2q = 0;
	double step2q;
	int h_g;

	public void run(String arg) {

		DirectoryChooser dc = new DirectoryChooser("Choose directory for stitched images...");
		if (dc.getDirectory() == null)
			return;

		String dirImg = dc.getDirectory() + File.separator;

		File[] listAll = new File(dirImg).listFiles(new StitchFilter());

		if (listAll.length == 0) {
			IJ.error("No TIFF File is Selected.");
			return;
		}

		XRDProps prop = XRDCommon.ReadProps();

		List<String> arrParamStr = new ArrayList<String>();
		try {
			arrParamStr = Files.readAllLines(new File(dirImg + XRDCommon.fNameLogStitch).toPath());
		} catch (IOException e) {
			XRDCommon.errorHandling("Failed to read file : " + XRDCommon.fNameLogStitch, e);
			return;
		}

		int i_max = 0;
		while (!arrParamStr.get(i_max).startsWith("globalMax2q = ")) {
			i_max++;
			if (i_max == arrParamStr.size()) {
				IJ.error("Failed to load parameters!");
				return;
			}
		}

		String strHead = arrParamStr.get(0);
		int idxStart, idxEnd = 0;
		try {
			idxStart = Integer.parseInt(strHead.substring(("File index ").length(), strHead.indexOf("-")));
			idxEnd = Integer
					.parseInt(strHead.substring(strHead.indexOf("-") + 1, strHead.indexOf(" for camera angle")));
		} catch (NumberFormatException e) {
			XRDCommon.errorHandling("INVALID FORMAT : " + XRDCommon.fNameLogStitch, e);
			return;
		}

		float globalMax2q = Float.parseFloat(arrParamStr.get(i_max).substring(("globalMax2q = ").length()));
		float globalMin2q = Float.parseFloat(arrParamStr.get(i_max + 1).substring(("globalMin2q = ").length()));
		float globalStep2q = Float.parseFloat(arrParamStr.get(i_max + 2).substring(("globalStep2q = ").length()));

		if (Float.isNaN(globalMax2q) || Float.isNaN(globalMax2q) || Float.isNaN(globalStep2q)) {
			IJ.error("Failed to load parameters!");
			return;
		}

		String strGuide = String.valueOf(idxEnd - idxStart + 1) + " points are to be loaded.";

		GenericDialog gd1 = new GenericDialog("Mapping Info");
		gd1.addMessage(strGuide);
		gd1.addNumericField("Mapping width: ", 1, 0);
		gd1.addNumericField("Mapping height: ", 1, 0);
		gd1.showDialog();

		if (gd1.wasCanceled())
			return;

		int w = (int) gd1.getNextNumber();
		int h = (int) gd1.getNextNumber();

		if (w * h != idxEnd - idxStart + 1) {
			IJ.error("Invalid mapping size!");
			return;
		}

		File target = new File(dirImg + "stack2q");
		if (!target.exists()) {
			if (!target.mkdir()) {
				IJ.error("Unable to create directory!");
				return;
			}
		}
		String filename = listAll[listAll.length - 1].getName();
		String strPrefix = filename.substring(0, filename.lastIndexOf("_"));
		List<String> lines;
		ImagePlus imp_stack;
		int z;
		try {
			Stream<String> _lines = Files.lines(Paths.get(dirImg + filename.replace(".tif", "_vs2q.txt")));
			lines = _lines.filter(s -> !s.isEmpty()).collect(Collectors.toList());
			lines.remove(0);
			_lines.close();
			z = lines.size();
			imp_stack = NewImage.createImage(strPrefix + "_stack2q", w, h, z, 32, NewImage.FILL_WHITE);
			for (int i = 0; i < z; i++) {
				imp_stack.getStack().setSliceLabel(lines.get(i).split("[,\\s]+")[1] + "degree", i + 1);
			}
			for (int i = 0; i < h; i++) {
				for (int j = 0; j < w; j++) {
					String strIdx = String.format("%05d", (int) Math.floor(idxStart + w * i + j));
					_lines = Files.lines(Paths.get(dirImg + strPrefix + "_" + strIdx + "stitch_vs2q.txt"));
					lines = _lines.filter(s -> !s.isEmpty()).collect(Collectors.toList());
					lines.remove(0);
					_lines.close();
					for (int k = 0; k < z; k++) {
						imp_stack.setSlice(k + 1);
						imp_stack.getProcessor().putPixelValue(j, i,
								Double.parseDouble(lines.get(k).split("[,\\s]+")[2]));
					}
				}
				IJ.showProgress(-i, h);
			}
		} catch (IOException e) {
			XRDCommon.errorHandling("Failed to load \"_vs2q.txt\" files.", e);
			return;
		}

		if (prop.flipHor) {
			for (int i = 0; i < imp_stack.getNSlices(); i++) {
				imp_stack.setSlice(i + 1);
				imp_stack.getProcessor().flipHorizontal();
			}
		}
		if (prop.flipVer) {
			for (int i = 0; i < imp_stack.getNSlices(); i++) {
				imp_stack.setSlice(i + 1);
				imp_stack.getProcessor().flipVertical();
			}
		}

		new FileSaver(imp_stack).saveAsTiff(dirImg + "stack2q" + File.separator + strPrefix + "_stack2q.tif");

		imp_stack.setTitle(strPrefix + "_stack2q.tif");
		imp_stack.show();

		IJ.showStatus("Finished to create 2q image stack.");

		File file = new File(dirImg + "stack2q" + File.separator + "log_stack2q.txt");
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			pw.println("Dimension = " + String.valueOf(w) + "x" + String.valueOf(h) + "x" + String.valueOf(z));
			pw.println("globalMax2q = " + String.valueOf(globalMax2q));
			pw.println("globalMin2q = " + String.valueOf(globalMin2q));
			pw.println("globalStep2q = " + String.valueOf(globalStep2q));

			pw.close();
		} catch (IOException e) {
			IJ.error(e.getMessage());
			e.printStackTrace();
			return;
		}
	}
}
