import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

public class Batch_job_1__I0_normalization implements PlugIn {

	double globalMin2q = 0, globalMax2q = 0;
	double step2q;
	int h_g;

	public void run(String arg) {

		String dirImg = "";

		DirectoryChooser dc = new DirectoryChooser("Choose directory for PILATUS images...");
		if (dc.getDirectory() == null)
			return;

		dirImg = dc.getDirectory() + File.separator;

		File[] listAll = new File(dirImg).listFiles(new TifFilter());

		if (listAll.length == 0) {
			IJ.error("No TIFF File is Selected.");
			return;
		}
		Arrays.sort(listAll);

		OpenDialog od = new OpenDialog("Choose Mapping Data File...");

		if (od.getPath() == null) {
			IJ.error("No File is Selected.");
			return;
		}

		String[] headerData = null;
		List<int[]> mapData = new ArrayList<int[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(od.getPath())));
			String line;
			int i_line = 0;
			while ((line = br.readLine()) != null) {
				if (i_line == 0) {
					headerData = line.replaceAll(" ", "").split(",");
				} else {
					int[] data_str = XRDCommon.parseInts(line.replaceAll(" ", "").split(","));
					mapData.add(data_str);
				}
				i_line++;
			}
			br.close();

		} catch (NumberFormatException ex) {
			XRDCommon.errorHandling("Invalid File Format. : " + od.getPath(), ex);
		} catch (IOException ex) {
			XRDCommon.errorHandling("Failed to read file", ex);
		}

		GenericDialog gd1 = new GenericDialog("I_0 Normalization...");
		gd1.addChoice("Select a column: ", headerData, "");
		gd1.showDialog();

		if (gd1.wasCanceled())
			return;

		int selectedIndex = gd1.getNextChoiceIndex();

		ResultsTable rt = ResultsTable.getResultsTable();
		rt.reset();

		for (int i = 0; i < mapData.size(); i++) {
			rt.setValue("I_0", i, mapData.get(i)[selectedIndex]);
		}
		double intNorm = rt.getValue("I_0", 0);

		File target = new File(dirImg + "normalized");
		if (!target.exists()) {
			if (!target.mkdir()) {
				IJ.error("Unable to create directory!");
				return;
			}
		}

		XRDProps prop = XRDCommon.ReadProps();
		if (prop.cacheBool) {
			for (int n = 0; n < listAll.length; n++) {
				ImagePlus work = new ImagePlus(listAll[n].getPath());
				IJ.showProgress((n + 1), listAll.length);
				IJ.showStatus("Reading files... : " + work.getTitle());
			}
		}

		for (int i = 0; i < listAll.length; i++) {
			String fname = listAll[i].getName();
			int fileIdx = Integer.parseInt(fname.substring(fname.lastIndexOf("_") + 1, fname.lastIndexOf(".tif")));

			ImagePlus imp = new ImagePlus(dirImg + fname);

			double val = intNorm / rt.getValue("I_0", fileIdx);

			imp.getProcessor().multiply(val);

			new FileSaver(imp).saveAsTiff(dirImg + "normalized" + File.separator + fname.replace(".tif", "norm.tif"));

			IJ.showProgress((i + 1), listAll.length);
			IJ.showStatus(fname);
		}

		IJ.showStatus("Finished I_0 normalization.");

		String resultFileName = dirImg + "normalized" + File.separator + XRDCommon.fNameLogNorm;
		try {
			rt.saveAs(resultFileName);
		} catch (IOException e) {
			XRDCommon.errorHandling("Failed to write file. : " + resultFileName, e);
		}
		rt.reset();

		try {
			Path src = Paths.get(resultFileName + ".new");
			Path srcRename = Paths.get(resultFileName);
			BufferedWriter out = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(resultFileName + ".new")));
			out.write("I_0 normalization to " + String.valueOf(intNorm) + "\n");
			for (String s : Files.readAllLines(srcRename)) {
				out.write(s + "\n");
			}
			out.close();
			Files.move(src, srcRename, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			XRDCommon.errorHandling("Failed to write file. : " + resultFileName, e);
		}
	}
}
