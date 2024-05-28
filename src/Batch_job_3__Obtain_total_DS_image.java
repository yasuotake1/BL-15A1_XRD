import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.plugin.Concatenator;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;

public class Batch_job_3__Obtain_total_DS_image implements PlugIn {

	double globalMin2q = 0, globalMax2q = 0;
	double step2q;
	int h_g;

	public void run(String arg) {

		XRDProps prop = XRDCommon.ReadProps();

		DirectoryChooser dc = new DirectoryChooser("Choose directory for stitched images...");
		if(dc.getDirectory() == null) return;

		String dirImg = dc.getDirectory() + File.separator;

		File[] listAll = new File(dirImg).listFiles(new StitchFilter());

		if (listAll.length == 0) {
			IJ.error("No TIFF File is Selected.");
			return;
		}

		List<String> arrParamStr = new ArrayList<String>();
		try {
			arrParamStr = Files.readAllLines(new File(dirImg + "log_stitched.txt").toPath());
		} catch (IOException e) {
			e.printStackTrace();
			IJ.error("Failed to read file : "+dirImg + "log_stitched.txt");
			return;
		}

		int i_max = 0;
		while(! arrParamStr.get(i_max).startsWith("globalMax2q = ")){
			if(i_max+1 == arrParamStr.size()){
				IJ.error("Failed to load parameters!");
				return;
			}
			i_max++;
		}
		
		float globalMax2q = Float.parseFloat(arrParamStr.get(i_max).substring(("globalMax2q = ").length()));
		float globalMin2q = Float.parseFloat(arrParamStr.get(i_max+1).substring(("globalMin2q = ").length()));
		float globalStep2q = Float.parseFloat(arrParamStr.get(i_max+2).substring(("globalStep2q = ").length()));
		
		if(Float.isNaN(globalMax2q) || Float.isNaN(globalMax2q) || Float.isNaN(globalStep2q)){
			IJ.error("Failed to load parameters!");
			return;
		}

		File target = new File(dirImg + "total");
		if (!target.exists()) {
			if (!target.mkdir()) {
				IJ.error("Unable to create directory!");
				return;
			}
		}
				
		String filename = listAll[listAll.length-1].getName();
		String strPrefix = filename.substring(0, filename.lastIndexOf("_"));

		ImagePlus imp_total = new ImagePlus();
		ImagePlus[] consimps = new ImagePlus[ listAll.length ];
		for(int i=0; i<listAll.length; i++) {
			consimps[i] = new ImagePlus(listAll[i].getPath());
			IJ.showProgress((i+1), listAll.length);
			IJ.showStatus(listAll[i].getName());
		}
		
		imp_total = new Concatenator().concatenateHyperstacks(consimps, "total", true);

		new FileSaver(imp_total).saveAsTiff(dirImg + "total" + File.separator + strPrefix + "_stackDS.tif");
		
		ZProjector zpro = new ZProjector(imp_total);
		zpro.setMethod(ZProjector.SUM_METHOD);
		zpro.doProjection();
		imp_total = zpro.getProjection();

		(new ContrastEnhancer()).stretchHistogram(imp_total,0.1);

		new FileSaver(imp_total).saveAsTiff(dirImg + "total" + File.separator + strPrefix + "_total.tif");

		imp_total.setTitle(strPrefix + "_total.tif");
		imp_total.show();

		ImagePlus imp2q = XRDCommon.calc2q(imp_total, globalMax2q, globalStep2q, prop);
		XRDCommon.plot2q(imp2q, globalMin2q, globalStep2q, dirImg + "total" + File.separator, strPrefix + "_total", true);

		IJ.showStatus("Finished image integration.");

		File file = new File(dirImg + "total" + File.separator + "log_total.txt");
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			pw.println("globalMax2q = " + String.valueOf(globalMax2q));
			pw.println("globalMin2q = " + String.valueOf(globalMin2q));
			pw.println("globalStep2q = " + String.valueOf(globalStep2q));
			pw.println("Number of integrated images = " + String.valueOf(listAll.length));
			pw.println("Stacked DS image file: " + strPrefix + "_stackDS.tif");
			pw.println("Integrated image file: " + strPrefix + "_total.tif");
			pw.println("Integrated 2q file: " + strPrefix + "_total" + "_2q.txt");
			
			pw.close();
		} catch (IOException e) {
			IJ.error(e.getMessage());
			e.printStackTrace();
			return;
		}

	}
}
