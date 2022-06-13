
package org.dbseret.fits;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.plugins.tiff.TIFFDirectory;
//import javax.imageio.plugins.tiff.TIFFField;

import nom.tam.fits.Fits;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.ImageHDU;


/**
 * JFITSConv
 * TIFF to FITS converter compliant with UNI 11845:2022
 * 
 * @author Francesco Gambino
 * @version 1.0.0
 * @see <a href="https://github.com/ProjectFITS4LAM/JFITSConv">JFITSConv Project</a>
 * @see <a href="https://www.dbseret.com/">DB SERET</a>
 */
public class JFITSConv {

	private final static String VERSION = "1.0.0";
	private final static String FITS_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private final static String TIFF_DATETIME_FORMAT = "yyyy:MM:dd HH:mm:ss";
	
	private String tiffilename = null;
	private String fitsfilename = null;
	
	
	public JFITSConv(String tiffilename, String fitsfilename) {
		
		FitsFactory.setUseHierarch(false);
		FitsFactory.setUseAsciiTables(false);
		FitsFactory.setCheckAsciiStrings(true);
		FitsFactory.setLongStringsEnabled(true);
		
		this.tiffilename = tiffilename;
		this.fitsfilename = fitsfilename;
	}

	
	public static void main(String[] args) {

		int exitcode = 0;
		
		log("JFITSConv v" + VERSION);
		log("TIFF to FITS UNI 11845:2022 compliant converter");
		log("");
		
		if (args.length == 0) {
			log("Usage:");
			log("\tJFITSConv tiff_filename fits_filename");
		}
		
		try {
			
			if (args.length == 2) {
				
				JFITSConv jfitsconv = new JFITSConv(args[0], args[1]);
				jfitsconv.execute();
			
			} else {
				throw new Exception("Wrong number of arguments");
			}
		
		} catch (Exception e) {
			
			exitcode = 1;
			log("");
			log("Error: " + e.getMessage());
		}
		
		System.exit(exitcode);
	}
	
	
	public static void log(String text) {
		System.out.println(text);
	}
	
	
	private void execute() throws Exception {
		
		Fits fits = new Fits();
		
		try {
			
			File infile = new File(this.tiffilename);
			File outfile = new File(this.fitsfilename);
			Date creationdate = new Date();
			
			log("Input:\t" + infile.getName());
			log("Output:\t" + outfile.getName());
			
			ImageReader reader = ImageIO.getImageReadersByFormatName("TIF").next();
			reader.setInput(ImageIO.createImageInputStream(infile), true);
			BufferedImage image = reader.read(0);
			float size = image.getWidth() * image.getHeight() / 1000000f;
			
			log("Size:\t" + image.getWidth() + "x" + image.getHeight() + " pixels [" + String.format("%.2f", size) + " MP]");
			
			TIFFDirectory dir = TIFFDirectory.createFromMetadata(reader.getImageMetadata(0));
			
			if ((dir.getTIFFField(TIFFTagName.BITS_PER_SAMPLE).getAsInt(0) != 8) ||
			    (dir.getTIFFField(TIFFTagName.SAMPLES_PER_PIXEL).getAsInt(0) != 3))
				throw new Exception("Image format not supported");
			
			int xres = dir.getTIFFField(TIFFTagName.XRESOLUTION).getAsInt(0);
			int yres = dir.getTIFFField(TIFFTagName.YRESOLUTION).getAsInt(0);
			int ures = dir.getTIFFField(TIFFTagName.RESOLUTION_UNIT).getAsInt(0);
			
			String device = "";
			byte[][][] data = getRGBMatrix(image);
			
			image.flush();
			fits.addHDU(FitsFactory.hduFactory(data));
			ImageHDU hdu = (ImageHDU)fits.getHDU(0);
			
			hdu.addValue("UNIKEY", true, "Compliant with UNI 11845:2022");
			hdu.addValue("EXTEND", true, "");
			hdu.addValue("LONGSTRN", "OGIP 1.0", "The OGIP long string convention may be used");
			hdu.addValue("CTYPE1", "", "");
			hdu.addValue("CTYPE2", "", "");
			hdu.addValue("CTYPE3", "RGB", "");
			hdu.addValue("CRPIX1", 0.0, "");
			hdu.addValue("CRPIX2", 0.0, "");
			hdu.addValue("CRPIX3", 0.0, "");
			hdu.addValue("CRVAL1", 0.0, "");
			hdu.addValue("CRVAL2", 0.0, "");
			hdu.addValue("CRVAL3", 0.0, "");
			/*
			hdu.addValue("PC1_1", 1.0, "");
			hdu.addValue("PC1_2", 1.0, "");
			hdu.addValue("PC2_1", 1.0, "");
			hdu.addValue("PC2_2", 1.0, "");
			*/
			hdu.addValue("CUNIT1", "mm", "");
			hdu.addValue("CUNIT2", "mm", "");
			hdu.addValue("CUNIT3", "", "");
			hdu.addValue("CDELT1", 25.4/xres, "");
			hdu.addValue("CDELT2", 25.4/yres, "");
			hdu.addValue("CDELT3", 1.0, "");
			
			hdu.addValue("COLORMAP", "RGB", "Colors mapping");
			hdu.addValue("IMGURESL", ures == 2 ? "INCH" : "MM", "Resolution unit");
			hdu.addValue("IMGXRESL", xres, "Horizontal resolution");
			hdu.addValue("IMGYRESL", yres, "Vertical resolution");
			
			hdu.addValue("OBJECT", infile.getName().replaceFirst("[.][^.]+$", ""), "Item identification");
			hdu.addValue("CREATOR", "JFITSConv v" + VERSION, "Software that created this FITS file");
			hdu.addValue("DATE", getFormattedDateTime(creationdate), "Date and time of FITS file creation");
			
			if (dir.getTIFFField(TIFFTagName.DATE_TIME) != null) {
				SimpleDateFormat tiffdateformat = new SimpleDateFormat(TIFF_DATETIME_FORMAT);
				Date date = tiffdateformat.parse(dir.getTIFFField(TIFFTagName.DATE_TIME).getAsString(0));
				hdu.addValue("DATE-OBS", getFormattedDateTime(date), "Date and time of acquisition");
			}
			
			if (dir.getTIFFField(TIFFTagName.COPYRIGHT) != null)
				hdu.addValue("ORIGIN", dir.getTIFFField(TIFFTagName.COPYRIGHT).getAsString(0), "Copyright notice");
			
			if (dir.getTIFFField(TIFFTagName.ARTIST) != null)
				hdu.addValue("AUTHOR", dir.getTIFFField(TIFFTagName.ARTIST).getAsString(0), "Author of the image");
			
			if (dir.getTIFFField(TIFFTagName.MAKE) != null)
				device = dir.getTIFFField(TIFFTagName.MAKE).getAsString(0);
			
			if (dir.getTIFFField(TIFFTagName.MODEL) != null) {
				String val = dir.getTIFFField(TIFFTagName.MODEL).getAsString(0);
				if ((device.length() > 0) && (val.startsWith(device)))
					device = val;
				else
					device += (device.length() > 0 ? " " : "") + val;
			}
			
			if (device.length() > 0)
				hdu.addValue("INSTRUME", device, "Maker and model of the device");
			
			if (dir.getTIFFField(TIFFTagName.SOFTWARE) != null)
				hdu.addValue("PROGRAM", dir.getTIFFField(TIFFTagName.SOFTWARE).getAsString(0), "Software that created the image");
			
			/*
			TIFFField[] fields = dir.getTIFFFields();
			log("Tags:");
			for(TIFFField f: fields) {
				log("\t" + f.getTagNumber() + "\t" + f.getTag().getName() + "=" + f.getValueAsString(0));
			}
			*/
			
			fits.setChecksum();
			fits.write(outfile);
			fits.close();
			
			log("");
			log("Operation completed");
		
		} catch (Exception e) {
			
			throw e;
			
		} finally {
			if (fits != null)
				fits.close();
		}
	}
	
	
	private byte[][][] getRGBMatrix(BufferedImage image) {
		
		Raster raster = image.getData();
		
        int[] buffer = new int[3];
        int w = raster.getWidth();
        int h = raster.getHeight();
        byte[][][] matrix = new byte[3][h][w];
        
        for(int y = 0; y < h ; y++) {
        	for(int x = 0 ; x < w ; x++) {
                raster.getPixel(x, y, buffer);
                matrix[0][h - y - 1][x] = (byte)(buffer[0]);
                matrix[1][h - y - 1][x] = (byte)(buffer[1]);
                matrix[2][h - y - 1][x] = (byte)(buffer[2]);
            }
        }
        
        return matrix;
	}
	
	
	private static String getFormattedDateTime(Date date) {
		
		SimpleDateFormat formatter = new SimpleDateFormat(FITS_DATETIME_FORMAT);
		return formatter.format(date);
	}
}
