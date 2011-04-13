package edu.cmu.pocketsphinx.demo;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Pattern;

import android.util.Log;

public class Utility {
    
    public static boolean saveUrlAsFile(String url, String filename) {
    	try {
    		//TODO (aup): Improve the exception handling. This is cruel.
    		Log.v("PocketSphinx.Utility","Trying to save "+url+" as "+filename);
    		URL u = new URL(url);
    		URLConnection uc = u.openConnection();
    		int contentLength = uc.getContentLength();
    		String type = uc.getContentType();

    		Log.i("PocketSphinx Utility", "contentlength "  + contentLength);
    		Log.i("PocketSphinx Utility", "contentType " + type);
    		if(type.contains("text/plain") && (filename.contains("dic") || filename.contains("list") || filename.equals("currentconf")||filename.endsWith(".params"))){
    			ArrayList<String> lines = readLinesFromURL(uc);
    			writeToFile(filename, lines);
    			return true;
    		}
    		InputStream raw = uc.getInputStream();
    		InputStream in = new BufferedInputStream(raw,8000);
    		byte[] data = new byte[contentLength];
    		int bytesRead = 0;
    		int offset = 0;
    		while (offset < contentLength) {
    			bytesRead = in.read(data, offset, data.length - offset);
    			if (bytesRead == -1)
    				break;
    			offset += bytesRead;
    		}
    		in.close();

//    		if (offset != contentLength) {
//    			throw new IOException("Only read " + offset + " bytes; Expected " + contentLength + " bytes");
//    		}

    		FileOutputStream out = new FileOutputStream(filename);
    		out.write(data);
    		out.flush();
    		out.close();
    		return true;
    	}
    	catch (Exception e) {
    		Log.e("PocketSphinx Utility","Could not save url as file.: "+e.getMessage());
    		return false;
    	}
    }
    
    public static boolean pathExists(String pathname){
        File tempFile = new File(pathname);
        if ((!tempFile.exists()) ){ 
            return false;
        }
        return true;
    }
    
    public static  ArrayList<String>readLines(String filename) throws IOException {
    	ArrayList<String> strLines = new ArrayList<String>();
    	FileInputStream fstream = new FileInputStream(filename);
    	DataInputStream in = new DataInputStream(fstream);
    	BufferedReader br = new BufferedReader(new InputStreamReader(in),1024);
    	String strLine;
    	while ((strLine = br.readLine()) != null)   {
    		strLines.add(strLine.trim().replace("\n",""));				
    	}
    	in.close();		
		return strLines;
    }   
    public static ArrayList<String>listDirs(String dir) throws IOException{
    	
    	ArrayList<String> dirNames = new ArrayList<String>();
    	File path = new File(dir);
    	FileFilter fileFilter = new FileFilter() {
    	    public boolean accept(File file) {
    	        return file.isDirectory();
    	    }
    	};
    	File[] dirs = path.listFiles(fileFilter);
    	for(File f:dirs)
    	{
    		dirNames.add(f.getName());
    	}
    	return dirNames;
    	
    }
    
    public static ArrayList<String> listFiles(String dir) throws IOException{
    	
    	ArrayList<String> dirNames = new ArrayList<String>();
    	File path = new File(dir);
    	FileFilter fileNameFilter = new FileFilter() {
    	    public boolean accept(File file) {
    	        return file.getName().endsWith(".DMP"); //check for language model
    	    }
    	};
    	File[] dirs = path.listFiles(fileNameFilter);
    	for(File f:dirs)
    	{
    		dirNames.add(f.getName());
    	}
    	return dirNames;
    	
    }
    

    /*
     *Its the caller's headache to write new lines after each line. 
     * 
     */
    public static boolean writeToFile(String filename, ArrayList<String> lines) {
    	 FileWriter fstream;
		try {
			fstream = new FileWriter(filename);
			  BufferedWriter out = new BufferedWriter(fstream);
		         for(String line:lines)
		           out.write(line);         
		         out.close();
		         return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
       
    }
    
    public static ArrayList<String> readLinesFromURL(URLConnection connection){
    	
	    BufferedReader reader = null;
	    ArrayList<String> lines = new ArrayList<String>();
    	 try {
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		

	      String line = null;
	      Pattern p = 
	            Pattern.compile(">"      // end quote, then skip to end of tag
	                    + "([^<]+)"       // name is data up to next tag
	                    + "</a>");
	      

	      while ((line = reader.readLine()) != null)
	      {
	    	
//	    	if(!line.contains("[DIR]")&&!line.contains("[ICO]"))
//	    	{
//	    		
//	    		Matcher m = p.matcher(line);
//	    		while(m.find()){
//	    			String tempFileName = m.group().replace("</a>","").replace(">",""); 
//	    			System.out.println(tempFileName);
//	              	files.add(tempFileName);
//	    		}
//	    	}
	    	  lines.add(line+"\n");
	        
	      }
    	 } catch (IOException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
    	 return lines;
    }
    
    
}


