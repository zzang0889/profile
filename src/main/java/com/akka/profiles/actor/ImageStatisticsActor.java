package com.akka.profiles.actor;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;

import akka.actor.UntypedAbstractActor;

public class ImageStatisticsActor extends UntypedAbstractActor {

	@Override
	public void onReceive(Object file) throws Throwable {
		// TODO Auto-generated method stub
		HashMap<String, Object> result = new HashMap<String, Object>();
		File Imagefile = (File) ((HashMap<String, Object>) file).get("image");
        Path path = Imagefile.toPath();
        InputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(path));
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        Metadata metadata = ImageMetadataReader.readMetadata(bis);
        metadata.containsDirectoryOfType(ExifIFD0Directory.class);
        Directory Exifdirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        Directory ExifSubdirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        Directory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        
        result.put("imageName", ((HashMap<String, Object>) file).get("imageName"));
        if(Exifdirectory!=null) {
        	result.put("imageHeight", Exifdirectory.getInt(ExifIFD0Directory.TAG_IMAGE_HEIGHT));
        	result.put("imageWidth", Exifdirectory.getInt(ExifIFD0Directory.TAG_IMAGE_WIDTH));
        	result.put("imageSdate", Exifdirectory.getDate(ExifIFD0Directory.TAG_DATETIME));
        } else if(ExifSubdirectory!=null){
			result.put("imageHeight", ExifSubdirectory.getInt(ExifSubIFDDirectory.TAG_IMAGE_HEIGHT));
        	result.put("imageWidth", ExifSubdirectory.getInt(ExifSubIFDDirectory.TAG_IMAGE_WIDTH));
        	result.put("imageSdate", ExifSubdirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME));
        } else {
            result.put("imageHeight", jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_HEIGHT));
        	result.put("imageWidth", jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_WIDTH));
        	result.put("imageSdate", null);
        }
		
		BufferedImage image = null;
		image = ImageIO.read(Imagefile);
		int width = image.getWidth();
	    int height = image.getHeight();
	    long r_sum= 0, g_sum = 0, b_sum = 0;
	    int r_min= 0, g_min = 0, b_min = 0;
	    int r_max= 0, g_max= 0, b_max = 0;
	    int r_count[] = new int[256];
	    int g_count[] = new int[256];
	    int b_count[] = new int[256];
	    
	    for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				//get pixel value
			    int p = image.getRGB(i,j);
			    //get red
			    int r = (p>>16) & 0xff;
			    r_sum +=r;
			    r_min = r > r_min ? r_min: r;
			    r_max = r < r_max ? r_max: r;
			    r_count[r]+=1;
			    //get green
			    int g = (p>>8) & 0xff;
			    g_sum += g;
			    g_min = g > g_min ? g_min: g;
			    g_max = g < g_max ? g_max: g;
			    g_count[g]+=1;
			    //get blue
			    int b = p & 0xff;
			    b_sum += b;
			    b_min = b > b_min ? b_min: b;
			    b_max = b < b_max ? b_max: b;
			    b_count[b]+=1;
			}
		}

	    result.put("averageR", r_sum/(width+height));
	    result.put("averageG", g_sum/(width+height));
	    result.put("averageB", b_sum/(width+height));
	    
	    result.put("maxR", r_max);
	    result.put("maxG", g_max);
	    result.put("maxB", b_max);
	    
	    result.put("minR", r_min);
	    result.put("minG", g_min);
	    result.put("minB", b_min);
	    
	    result.put("histogramR", Arrays.toString(r_count));
	    result.put("histogramG", Arrays.toString(g_count));
	    result.put("histogramB", Arrays.toString(b_count));
		getSender().tell(result, getSelf());
	}
	
}
