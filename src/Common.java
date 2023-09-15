import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.ProfilePlot;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Common implements PlugIn {

	public static float pX = 0.000172F;
	public static float pY = 0.000172F;
	public static float y0 = 82F;
	public static float cAngle = 20F;
	public static float cDist = 0.201F;
	public static int directionInt = 0;
	public static boolean roundBool = false;
	public static boolean cacheBool = true;
	public static boolean debugBool = false;
	public static List<Integer> arrAngles = new ArrayList<Integer>();
	public static String defaultDir = "";

	public static String PropPath = "plugins/BL-15A1_XRD/XRDProps.config";

	public void run(String arg) {
		// plugins ���j���[�ɂ͕\������Ȃ��N���X
	}

	/**
	 * �ݒ�t�@�C���ǂݏo��
	 * 
	 * @return
	 */
	public static XRDProps ReadProps() {
		Properties prop = new Properties();

		InputStream is;
		XRDProps target = new XRDProps();
		try {
			is = new FileInputStream(new File(PropPath));
			prop.load(is);
			is.close();

			target.pX = Float.parseFloat(prop.getProperty("pX"));
			target.pY = Float.parseFloat(prop.getProperty("pY"));
			target.y0 = Float.parseFloat(prop.getProperty("y0"));
			target.cAngle = Float.parseFloat(prop.getProperty("cAngle"));
			target.cDist = Float.parseFloat(prop.getProperty("cDist"));
			target.directionInt = Integer.parseInt(prop.getProperty("directionInt"));
			target.roundBool = Boolean.parseBoolean(prop.getProperty("roundBool"));
			target.cacheBool = Boolean.parseBoolean(prop.getProperty("cacheBool"));
			target.debugBool = Boolean.parseBoolean(prop.getProperty("debugBool"));
			target.arrAngles = Arrays.asList(prop.getProperty("arrAngles").replaceAll("[ \\[\\]]", "").split(","))
					.stream().mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
			target.defaultDir = prop.getProperty("defaultDir");

		} catch (FileNotFoundException e) {
			// �f�t�H���g�l���g�p����
			target.pX = pX;
			target.pY = pY;
			target.y0 = y0;
			target.cAngle = cAngle;
			target.cDist = cDist;
			target.directionInt = directionInt;
			target.roundBool = roundBool;
			target.cacheBool = true;
			target.debugBool = false;
			target.arrAngles = arrAngles;
			target.defaultDir = defaultDir;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return target;

	}

	/**
	 * �ݒ�t�@�C�������o��
	 * 
	 * @param target
	 */
	public static void WriteProps(XRDProps target) {
		Properties prop = new Properties();

		prop.setProperty("pX", String.valueOf(target.pX));
		prop.setProperty("pY", String.valueOf(target.pY));
		prop.setProperty("y0", String.valueOf(target.y0));
		prop.setProperty("cAngle", String.valueOf(target.cAngle));
		prop.setProperty("cDist", String.valueOf(target.cDist));
		prop.setProperty("directionInt", String.valueOf(target.directionInt));
		prop.setProperty("roundBool", Boolean.toString(target.roundBool));
		prop.setProperty("cacheBool", Boolean.toString(target.cacheBool));
		prop.setProperty("debugBool", Boolean.toString(target.debugBool));
		prop.setProperty("arrAngles", Arrays.toString(target.arrAngles.toArray()));
		prop.setProperty("defaultDir", target.defaultDir);
		try {
			Prefs.savePrefs(prop, Common.PropPath);
		} catch (IOException e) {
			IJ.error("Failed to write properties.");
		}
	}

	// PILATUS�C���[�W���C���[�W���O�v���[�g(IP)���ɕϊ�
	/**
	 * [calcIP] �J�����p�_���ƃJ�����p���w�肵�A������PILATUS�C���[�W
	 * (�J������X-�J������Y) ���Ȃ��ăf�o�C�E�V�F���[�z�u�C���[�W���O�v���[�g (IP) ��
	 * (2��-�J������Y) �ɕϊ�
	 * 
	 * @param imp
	 * @param max2q
	 * @param step2q
	 * @return
	 */
	public static ImagePlus calcIP(ImagePlus imp, double step2q, double angle, XRDProps prop) {

		ImagePlus imp_new = imp.duplicate();
		int w = imp.getProcessor().getWidth();
		int h = imp.getProcessor().getHeight();

		for (int i = 0; i < w; i++) {

			double xi;
			if (prop.directionInt % 2 == 0) {
				xi = w / 2 + prop.cDist * Math.tan((getMin2q(angle, w, prop) - angle + i * step2q) / 180 * Math.PI)
						/ prop.pX;
			} else {
				xi = w / 2 - prop.cDist * Math.tan((getMax2q(angle, w, prop) - angle - i * step2q) / 180 * Math.PI)
						/ prop.pX;
			}

			for (int j = 0; j < h; j++) {

				if (!prop.roundBool) {
					// @@@@@<���}�l�g�p>��������
					imp_new.getProcessor().putPixelValue(i, j, imp.getProcessor().getInterpolatedValue(xi, j));// [A]
					// @@@@@<���}�l�g�p>�����܂�
				} else {
					// @@@@@<round()�g�p>��������
					imp_new.getProcessor().putPixel(i, j, imp.getProcessor().getPixel((int) Math.round(xi), j));
					// imp_new.getProcessor().putPixel(i, j,
					// imp.getProcessor().getPixelInterpolated(xi, j));// [B]==round
					// @@@@@<round()�g�p>�����܂�
				}

			}
		}
		return imp_new;
	}

	// IP������^��2�Ƒ��ɕϊ�
	/**
	 * [calc2q] IP�� (2��-�J������Y) ����^��2�Ƒ� (2��-�J������Y)�ɕϊ�
	 * 
	 * @param imp
	 * @param max2q
	 * @param step2q
	 * @return
	 */
	public static ImagePlus calc2q(ImagePlus imp, double min2q, double step2q, XRDProps prop) {

		ImagePlus imp_new = imp.duplicate();

		int w = imp_new.getProcessor().getWidth();
		int h = imp_new.getProcessor().getHeight();

//		ExecutorService threadPool = Executors.newSingleThreadExecutor();
//		ExecutorService threadPool = Executors.newCachedThreadPool();
//		ExecutorService threadPool = Executors.newFixedThreadPool (8);

		// Common.debugTiff(imp, "calc2q_pre");
		double y;
		double j2qRad;
		double xj;
		for (int i = 0; i < h; i++) {
//			threadPool.submit(new calc2q_Sub(i, w, max2q, step2q, imp_new, imp));
			y = (i - prop.y0) * prop.pY;

			for (int j = 0; j < w; j++) {
				j2qRad = (min2q + j * step2q) / 180 * Math.PI;
				if (Math.abs(y) > Math.abs(j2qRad * prop.cDist)) {
					imp_new.getProcessor().putPixelValue(j, i, Double.NaN);
				} else {
					if (prop.directionInt % 2 == 0) {
						xj = j - prop.cDist
								* (j2qRad - Math.signum(j2qRad) * Math.acos(
										Math.sqrt(prop.cDist * prop.cDist + y * y) * Math.cos(j2qRad) / prop.cDist))
								/ prop.pX;
					} else {
						xj = w - j
								+ prop.cDist * (j2qRad - Math.signum(j2qRad) * Math.acos(
										Math.sqrt(prop.cDist * prop.cDist + y * y) * Math.cos(j2qRad) / prop.cDist))
										/ prop.pX;
					}
					if (xj < 0) {
						imp_new.getProcessor().putPixelValue(j, i, Double.NaN);
					} else {
						if (!prop.roundBool) {
							// @@@@@<���}�l�g�p>��������
							imp_new.getProcessor().putPixelValue(j, i, imp.getProcessor().getInterpolatedValue(xj, i)); // [A]
							// @@@@@<���}�l�g�p>�����܂�
						} else {
							// @@@@@<round()�g�p>��������
							imp_new.getProcessor().putPixel(j, i,
									imp.getProcessor().getPixel((int) (Math.round(xj)), i));
							// imp_new.getProcessor().putPixel(j, i,
							// imp.getProcessor().getPixelInterpolated(xj, i)); //
							// [B]==round
							// @@@@@<round()�g�p>�����܂�
						}
					}
				}
				// Common.debug(""+j+","+i+","+imp_new.getProcessor().getInterpolatedValue(j,
				// i));
			} // for(j)

		}
//		threadPool.shutdown();
		// Common.debugTiff(imp_new, "calc2q");

//		for (int i = 0; i < h; i++) {
//			for (int j = 0; j < w; j++) {
//				Common.debug(""+j+","+i+","+Float.intBitsToFloat(imp_new.getProcessor().getPixel(j, i)));
//			}
//		}
		return imp_new;
	}

	/**
	 * 
	 * @param imp
	 * @param min2q
	 * @param step2q
	 * @param dir
	 * @param nameStrip
	 * @param bShow
	 */
	public static void plot2q(ImagePlus imp, double min2q, double step2q, String dir, String nameStrip, boolean bShow) {
		ResultsTable rt = ResultsTable.getResultsTable();
		rt.reset();
		imp.setRoi(0, 0, imp.getWidth(), imp.getHeight());
		ProfilePlot pp = new ProfilePlot(imp);
		double[] profileY = pp.getProfile();
		double[] profileX = new double[profileY.length];

		for (int i = 0; i < profileY.length; i++) {
			profileX[i] = min2q + i * step2q;
			rt.setValue("2q", i, profileX[i]);
			rt.setValue("Intensity", i, profileY[i]);
		}
		if (bShow) {
			Plot pl = new Plot(nameStrip + "_vs2q", "2q (deg.)", "Intensity");
			pl.addPoints(profileX, profileY, Plot.LINE);
			pl.show();
		}

		try {
			rt.saveAs(dir + nameStrip + "_vs2q.txt");
		} catch (IOException e) {
			e.printStackTrace();
			IJ.error("Failed to write file. : " + dir + nameStrip + "_vs2q.txt");
		}
		rt.reset();
		return;
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public static double getAngle() throws Exception {
		XRDProps prop = ReadProps();
		GenericDialog gd0 = new GenericDialog("Camera Angle");
		gd0.addNumericField("Camera angle: ", prop.cAngle, 2, 5, "degree");
		gd0.showDialog();

		if (gd0.wasCanceled())
			throw new Exception("Image loading was canceled.");

		return (double) gd0.getNextNumber();
	}

	public static List<Integer> getStitchAngles() throws Exception {
		XRDProps prop = ReadProps();
		GenericDialog gd1 = new GenericDialog("Stitch PILATUS Images");
		gd1.addNumericField("# of camera angles: ", prop.arrAngles.size(), 0);
		gd1.showDialog();

		if (gd1.wasCanceled())
			return new ArrayList<Integer>();

		int numImages;
		numImages = (int) gd1.getNextNumber();

		if (numImages < 1 || Math.round(numImages) != numImages) {
			throw new Exception("Invalid number of images!");
		}

		String strGuide = "Note: Angles must be assigned in ascending order.";

		GenericDialog gd2 = new GenericDialog("Stitch PILATUS Images");

		for (int i = 0; i < numImages; i++) {
			String strLabel = "Camera angle of image #" + String.valueOf(i + 1) + ": ";
			if (i < prop.arrAngles.size())
				gd2.addNumericField(strLabel, prop.arrAngles.get(i), 2, 5, "degree");
			else
				gd2.addNumericField(strLabel, prop.cAngle, 2, 5, "degree");

		}
		gd2.addMessage(strGuide);
		gd2.showDialog();

		if (gd2.wasCanceled())
			return new ArrayList<Integer>();

		List<Integer> arrAngles = new ArrayList<Integer>(numImages);
		for (int i = 0; i < numImages; i++) {
			arrAngles.add((int) gd2.getNextNumber());
		}

		for (int i = 0; i < numImages - 1; i++) {
			if (arrAngles.get(i) > arrAngles.get(i + 1)) {
				throw new Exception("Invalid angle assignment!");
			}
		}
		prop.arrAngles = arrAngles;
		WriteProps(prop);

		return arrAngles;
	}

	/**
	 * <h1>�P�����z��̗v�f�𔽓]����</h1>
	 * <p>
	 * </p>
	 * 
	 * @param arr �F �Ώ۔z��
	 */
	public static final void reverse(double[] arr) {
		final int len = arr.length;
		double tmp;
		for (int i = 0; i < len / 2; i++) {
			tmp = arr[i];
			arr[i] = arr[len - 1 - i];
			arr[len - 1 - i] = tmp;
		}
	}

	public static double[] getColumnAverageProfile(Rectangle rect, ImageProcessor ip) {
		double[] profile = new double[rect.width];
		int[] counts = new int[rect.width];
		double[] aLine;
		ip.setInterpolate(false);
		for (int y = rect.y; y < rect.y + rect.height; y++) {
			aLine = ip.getLine(rect.x, y, rect.x + rect.width - 1, y);
			for (int i = 0; i < rect.width; i++) {
				if (!Double.isNaN(aLine[i])) {
					profile[i] += aLine[i];
					counts[i]++;
				}
			}
		}
		for (int i = 0; i < rect.width; i++)
			profile[i] /= counts[i];
		return profile;
	}

	public static double getMin2q(double cameraAngle, int width, XRDProps prop) {
		return cameraAngle - Math.atan(prop.pX * (width - 1) / 2 / prop.cDist) / Math.PI * 180;
	}

	public static double getMax2q(double cameraAngle, int width, XRDProps prop) {
		return cameraAngle + Math.atan(prop.pX * (width - 1) / 2 / prop.cDist) / Math.PI * 180;
	}

	/**
	 * ������z�񂩂琔�l�z����쐬
	 * 
	 * @param s
	 * @return
	 */
	public static int[] parseInts(String[] s) {

		int[] x = new int[s.length];
		for (int i = 0; i < s.length; i++) {
			x[i] = Integer.parseInt(s[i]);
		}
		return x;
	}

	/**
	 * �f�o�b�O���O���o��
	 * 
	 * @param s
	 */
	public static void debug(String s) {
		XRDProps prop = ReadProps();
		if (prop.debugBool) {
			IJ.log(s);
		}
	}

	/**
	 * 
	 * @param str
	 */
	public static void log(String str) {
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		Date now = new Date();
		DateFormat df = new SimpleDateFormat("HH:mm:ss");

		IJ.log(str + " : [" + ste.getClassName() + "] " + df.format(now));
	}

	/**
	 * �t�@�C���`�F�b�N�iTIFF/32bpp)
	 * 
	 * @param filepath
	 * @return
	 */
	public static ImagePlus CheckTiff32BPP(String filepath) {

		Pattern p = Pattern.compile(".*[.]tif$");
		Matcher m = p.matcher(filepath);

		// �I�������t�@�C���̊g���q��tif�łȂ���΃G���[�ƕ\�����ďI��
		if (!m.find()) {
			IJ.error("Invalid file type.");
			return null;
		}

		File f = new File(filepath);
		// �I�������t�@�C�������݂��Ȃ���΃G���[�ƕ\�����ďI��
		if (!f.exists()) {
			IJ.error("File[" + filepath + "] does not exist.");
			return null;
		}

		// �摜�t�@�C����ImagePlus�ɕϊ�
		ImagePlus imp = new ImagePlus(filepath);
		// �I�������t�@�C����BPP(�F�[�x�FBit Per Pixel)��32�łȂ���΃G���[�ƕ\�����ďI��
		if (imp.getBitDepth() != 32) {
			IJ.error("Invalid file type.");
			return null;
		}
		return imp;
	}
}

/**
 * �t�B���^�����O�N���X(_nnnnn.tif)
 */
class TifFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {

		Pattern p = Pattern.compile(".*[_][0-9]{5}[.]tif$");
		Matcher m = p.matcher(name);
		if (m.find())
			return true;

		return false;
	}
}

/**
 * �t�B���^�����O�N���X(_nnnnnnorm.tif)
 */
class NormalizeFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {

		Pattern p = Pattern.compile(".*[_][0-9]{5}norm[.]tif$");
		Matcher m = p.matcher(name);
		if (m.find())
			return true;

		return false;
	}
}

/**
 * �t�B���^�����O�N���X(_nnnnnstitch.tif)
 */
class StitchFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {

		Pattern p = Pattern.compile(".*[_][0-9]{5}stitch[.]tif$");
		Matcher m = p.matcher(name);
		if (m.find())
			return true;

		return false;
	}
}

/* 2016.08.12 ������s�s�̂��߁A�g�p�֎~ */
/*
 * class calc2q_Sub implements Callable<String> {
 * 
 * private int i; private double w, max2q; private double step2q; private
 * ImagePlus imp_new; private ImagePlus imp;
 * 
 * private XRDProps prop;
 * 
 * public calc2q_Sub(int _i, double _w, double _max2q, double _step2q, ImagePlus
 * _imp_new, ImagePlus _imp) {
 * 
 * this.i = _i; this.w = _w; this.max2q = _max2q; this.step2q = _step2q;
 * this.imp_new = _imp_new; this.imp = _imp;
 * 
 * this.prop = Common.ReadProps(); }
 * 
 * public String call() { double yL2 = (i - prop.y0) * prop.pY * (i - prop.y0) *
 * prop.pY / prop.cDist / prop.cDist;
 * 
 * for (int j = 0; j < w; j++) { double xj = (max2q - Math.acos(Math.cos((max2q
 * - j * step2q) / 180 * Math.PI) * Math.sqrt(1 + yL2)) / Math.PI * 180) /
 * step2q;
 * 
 * if (!prop.roundBool) { // @@@@@<���}�l�g�p>��������
 * imp_new.getProcessor().putPixelValue(j, i,
 * imp.getProcessor().getInterpolatedValue(xj, i)); // [A]
 * // @@@@@<���}�l�g�p>�����܂�
 * 
 * } else { // @@@@@<round()�g�p>�������� imp_new.getProcessor().putPixel(j, i,
 * imp.getProcessor().getPixel((int) (Math.round(xj)), i)); //
 * imp_new.getProcessor().putPixel(j, i, //
 * imp.getProcessor().getPixelInterpolated(xj, i)); // // [B]==round
 * // @@@@@<round()�g�p>�����܂� }
 * //Common.debug(""+j+","+i+","+imp_new.getProcessor().getInterpolatedValue(j,
 * i)); } // for(j) //imp_new.getProcessor().putPixelValue(1, 62,
 * imp.getProcessor().getInterpolatedValue(1, 69)); // [A]
 * //imp_new.getProcessor().putPixelValue(1, 62, 1915.5434208759418); // [A]
 * return "";
 * 
 * } }
 */
