# BL-15A1_XRD
ImageJ plugin for XRD data analysis at KEK-PF BL-15A1.  
Using this plugin, Dectris PILATUS images of synchrotron X-ray diffraction patterns can be loaded and analyzed.  
These codes still require cleaning-up, but were anyway put on public repository to meet the requests to inspect them. 

# Installation
Create a folder "[ImageJ root]/plugins/BL-15A1_XRD" and place BL-15A1_XRD.jar and XRDProps.config in it. Then restart ImageJ.

# How to use

### BL-15A1 XRD > Setup PILATUS...
Set necessary parameters before loading XRD patterns.
- **Pixel size X** and **Pixel size Y** are the pixel size of the detector. Default is 0.000172 m = 172 microns for PILATUS 100k.
- **Direct spot position Y** specifies the position of the direct beam in pixels. Open a image with direct beam spot and measure the distance from the top in case two-theta increases to the left or right of the image.
- **Default camera angle** is the two-theta angle of the camera center (i.e. goniometer angle) in degree. It is asked every time you run **Load single PILATUS image...** and this option specifies the default value for the dialog.
- **Camera distance** is the distance from the camera center to the sample.
- **PILATUS direction** specifies the set up of PILATUS detector.
- When converting images, interpolated pixel values are used by default. **Use round() for getPixel()** forces to use nearest neighbour pixel values (no interpolation).
- **Cache file data** allows to store intermediate images for batch job operation.
- With checking **Debug mode**, intermediate images will be displayed. This might also be helpful to understand how this plugin works.

# Reference
See [Y. Takeichi et al.](https://doi.org/10.2355/isijinternational.ISIJINT-2023-215 "Y. Takeichi et al., ISIJ Int. 63, 2017 (2023).") and its supplementary materials for how this plugin is used.
