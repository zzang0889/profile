package com.akka.profiles.actor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;

import akka.actor.UntypedAbstractActor;

public class ImageMetaArtor extends UntypedAbstractActor {

	@Override
	public void onReceive(Object file) throws Throwable {
		// TODO Auto-generated method stub
		HashMap<String, Object> result = new HashMap<String, Object>();
		File imageFile = (File) file;
        Path path = imageFile.toPath();
        InputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(path));
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        Metadata metadata = ImageMetadataReader.readMetadata(bis);
        metadata.containsDirectoryOfType(ExifIFD0Directory.class);
        Directory Exifdirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        Directory ExifSubdirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        Directory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        

        if(Exifdirectory!=null) {
        	System.out.println(Exifdirectory.getString(ExifIFD0Directory.TAG_IMAGE_HEIGHT));
			System.out.println(Exifdirectory.getString(ExifIFD0Directory.TAG_IMAGE_WIDTH));
			System.out.println(Exifdirectory.getString(ExifIFD0Directory.TAG_DATETIME));
        } else if(ExifSubdirectory!=null){
        	System.out.println(ExifSubdirectory.getString(ExifSubIFDDirectory.TAG_IMAGE_HEIGHT));
			System.out.println(ExifSubdirectory.getString(ExifSubIFDDirectory.TAG_IMAGE_WIDTH));
			System.out.println(ExifSubdirectory.getString(ExifSubIFDDirectory.TAG_DATETIME));
        } else {
        	System.out.println(jpegDirectory.getString(JpegDirectory.TAG_IMAGE_HEIGHT));
            System.out.println(jpegDirectory.getString(JpegDirectory.TAG_IMAGE_WIDTH));
        }
        
        

		getSender().tell(result, getSelf());
	}

}
