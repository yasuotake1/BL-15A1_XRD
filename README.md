# BL-15A1_XRD
ImageJ plugin for XRD data analysis at KEK-PF BL-15A1.  
Using this plugin, Dectris PILATUS images of synchrotron X-ray diffraction patterns can be loaded and analyzed.  
These codes still require cleaning-up, but were anyway put on public repository to meet the requests to inspect them. 

# How to install
Create a folder "[ImageJ root]/plugins/BL-15A1_XRD" and place BL-15A1_XRD.jar and XRDProps.config in it. Then restart ImageJ.

# How to use

1. BL-15A1 XRD > Setup PILATUS...
Set necessary parameters before loading XRD patterns.
* Pixel size X and Pixel size Y
Pixel size of the detector (default 0.000172 m = 172 microns for PILATUS 100k).
* Direct spot position Y

# Reference
See [Y. Takeichi et al.](https://doi.org/10.2355/isijinternational.ISIJINT-2023-215 "Y. Takeichi et al., ISIJ Int. 63, 2017 (2023).") and its supplementary materials for how this plugin is used.