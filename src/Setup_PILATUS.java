import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Setup_PILATUS implements PlugIn {

	public void run(String arg) {
		
		XRDProps readProps = Common.ReadProps();
		
		GenericDialog gd = new GenericDialog("PILATUS Setup");
		
		String[] choiceDirections = {"Portrait (2q increase to right)", "Portrait (2q increase to left)", "Landscape (2q increase to top)"};

		// Dialog.create("PILATUS Setup");
		String strGuide = "Select \"Image\" -> \"Show Info\" to show PILATUS image description.";
		gd.addMessage(strGuide);

		gd.addNumericField("Pixel size X: ", readProps.pX, 6, 7, "m");
		gd.addNumericField("Pixel size Y: ", readProps.pY, 6, 7, "m");
		gd.addNumericField("Direct spot position Y: ", readProps.y0, 0, 3, "pixel");
		gd.addNumericField("Default camera angle: ", readProps.cAngle, 2, 5, "degree");
		gd.addNumericField("Camera distance: ", readProps.cDist, 4, 5, "m");
		gd.addChoice("PILATUS direction: ", choiceDirections, choiceDirections[readProps.directionInt]);
		gd.addMessage("Choose \"increase to right\" for data acquired at BL-15A1 since 2021,\n\"increase to left\" up to 2020.");
		gd.addCheckbox("Use Round() for getPixel(): ", readProps.roundBool);
		gd.addCheckbox("Cache file data: ", readProps.cacheBool);
		gd.addCheckbox("Debug Mode: ", readProps.debugBool);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		XRDProps target = new XRDProps();
		target.pX = (float) gd.getNextNumber();
		target.pY = (float) gd.getNextNumber();
		target.y0 = (float) gd.getNextNumber();
		target.cAngle = (float) gd.getNextNumber();
		target.cDist = (float) gd.getNextNumber();
		target.directionInt = gd.getNextChoiceIndex();
		target.roundBool = gd.getNextBoolean();
		target.cacheBool = gd.getNextBoolean();
		target.debugBool = gd.getNextBoolean();
		target.arrAngles = readProps.arrAngles;
		target.defaultDir = readProps.defaultDir;
		
		Common.WriteProps(target);
	}

}
