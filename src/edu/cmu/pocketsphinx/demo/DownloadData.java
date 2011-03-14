package edu.cmu.pocketsphinx.demo;
import android.util.Log; 

import java.io.BufferedInputStream;
import java.io.File; 
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry; 
import java.util.zip.ZipInputStream; 
 
public class DownloadData {

	public int totalFileLength = 0;
	public int finishedFileLength;

	public boolean abortDownload;
	public boolean finished;
	public boolean success;
	


	public void saveUrlAsFile(final String url, final String filename) {
		finished = false;
		success = false;
		new Thread() {
			public void run() {
				save(url, filename);
				decompressData(filename, "");
			}
		}.start();
	}

	private boolean save(String url, String filename) {
    	try {
   		
    		abortDownload = false;
    		
    		Log.v("PocketSphinx.DownloadData","Trying to save "+url+" as "+filename);
    		URL u = new URL(url);
    		URLConnection uc = u.openConnection();
    		int contentLength = uc.getContentLength();

    		totalFileLength = contentLength;
    		finishedFileLength = 0;
    		
    		InputStream raw = uc.getInputStream();
    		InputStream in = new BufferedInputStream(raw,256);
    		byte[] data = new byte[contentLength];
    		int bytesRead = 0;
    		int offset = 0;
    		while (offset < contentLength) {
    			bytesRead = in.read(data, offset, data.length - offset);
    			if (bytesRead == -1)
    				break;    			
    			finishedFileLength += bytesRead;
    			offset += bytesRead;
    			if(abortDownload)
    				break;
    		}
    		in.close();

    		if(abortDownload) {
    			Log.e("PocketSphinx.DownloadData", "File download aborted by user");
    			success = false;
    			finished = true;
    			return false;
    		}
    		
    		if (offset != contentLength) {
    			throw new IOException("Only read " + offset + " bytes; Expected " + contentLength + " bytes");
    		}
    		

    		FileOutputStream out = new FileOutputStream(filename);
    		out.write(data);
    		out.flush();
    		out.close();
    		finished = true;
    		success = true;
    		return true;
    	}
    	catch (Exception e) {
    		Log.e("PocketSphinx Utility","Could not save url as file.: "+e.getMessage());
    		finished = true;
    		return false;
    	}
    }

	public void abort() {
		abortDownload = true;
	}
	
	public void decompressData(String zipFile, String unzipLocation){
		
		//String zipFile = Environment.getExternalStorageDirectory() + "/files.zip"; 
		//String unzipLocation = Environment.getExternalStorageDirectory() + "/unzipped/"; 
		 
		Decompress d = new Decompress(zipFile, unzipLocation); 
		d.unzip(); 
	}
	



class Decompress { 
  private String _zipFile; 
  private String _location; 
 
  public Decompress(String zipFile, String location) { 
    _zipFile = zipFile; 
    _location = location; 
 
    _dirChecker(""); 
  } 
 
  public void unzip() { 
    try  { 
      FileInputStream fin = new FileInputStream(_zipFile); 
      ZipInputStream zin = new ZipInputStream(fin); 
      ZipEntry ze = null; 
      while ((ze = zin.getNextEntry()) != null) { 
        Log.v("Decompress", "Unzipping " + ze.getName()); 
 
        if(ze.isDirectory()) { 
          _dirChecker(ze.getName()); 
        } else { 
          FileOutputStream fout = new FileOutputStream(_location + ze.getName()); 
          for (int c = zin.read(); c != -1; c = zin.read()) { 
            fout.write(c); 
          } 
 
          zin.closeEntry(); 
          fout.close(); 
        } 
         
      } 
      zin.close(); 
    } catch(Exception e) { 
      Log.e("Decompress", "unzip", e); 
    } 
 
  } 
 
  private void _dirChecker(String dir) { 
    File f = new File(_location + dir); 
 
    if(!f.isDirectory()) { 
      f.mkdirs(); 
    } 
  } 
 }

}