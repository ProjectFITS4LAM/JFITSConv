# JFITSConv
JFITSConv is a TIFF to FITS image converter compliant with the [UNI 11845:2022](https://store.uni.com/p/UNI1606754/uni-118452022-316668/UNI1606754_EIT) standard.<br>
JFITSConv handles several original TIFF tags by mapping them with specific FITS keywords as described in the Appendix of the UNI 11845:2022 document.<br>

## License
JFITSConv is released under the GNU General Public License version 3.0. See LICENSE file for details.

## Requirements
JFITSConv uses the <b>nom.tam.fits</b> library available [here](https://github.com/nom-tam-fits/nom-tam-fits/releases).
The library must be modified because of the fixed position of the EXTEND keyword in the FITS header.
The UNI 11845:2022 standard establishes that the EXTEND keyword must be placed after the UNIKEY keyword and the UNIKEY keyword must be placed immediately after the last NAXISn keyword.<br>
To compile the source codes you need to use Java 14 or above versions.

### Notes
Since the FITS standard version 3.0 the EXTEND keyword is no longer in fixed position after NAXISn.<br>
It seems that the latest nom.tam.fits version (1.16.1) has a misoutput when managing zero float values (the library writes a zero value as "0.E0" instead of "0.0") so we recommend to use the 1.15.2 version and correct handling of the EXTEND keyword as described below.<br>
The converter supports 24 bit color images only.

### The EXTEND keyword fix

To free the positioning of EXTEND keyword you need to modify the class <b>HeaderOrder</b> by commenting the source code from line 116 as follows (nom.tam.fits version 1.15.2):

```java
        /*
        if (c1.equals(EXTEND.key())) {
            return -1;
        } else if (c2.equals(EXTEND.key())) {
            return 1;
        } else
        */
```

After making this fix, compile and create a new jar file that you will use in conjunction with JFITSConv.

## Usage

To show the command-line usage just start JFITSConv without parameters.<br>
The current version of JFITSConv uses two parameters:
the original TIFF image filename and the output FITS filename.

```sh
JFITSConv tiff_filename fits_filename
```

Example:

```sh
java -cp "nom-tam-fits.jar;JFITSConv.jar" org.dbseret.fits.JFITSConv image.tif image.fit
```

## See also

The UNI 11845:2022 standard documentation is available at [uni.com](https://store.uni.com/p/UNI1606754/uni-11845-2022/UNI1606754).
