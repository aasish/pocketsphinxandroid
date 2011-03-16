package edu.cmu.pocketsphinx.demo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log; 
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File; 
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry; 
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream; 

import javax.rmi.CORBA.Util;
 
public class DownloadData extends Activity{

	public int totalFileLength = 0;
	public int finishedFileLength;
	private final static String PS_DATA_PATH = Environment.getExternalStorageDirectory()
    + "Android/data/edu.cmu.pocketsphinx/";
	private final static String ROOT_URL = "http://tts.speech.cs.cmu.edu/apappu/android/edu.cmu.pocketsphinx/";
	public boolean abortDownload;	
	public boolean finished;
	public boolean success;
	private Handler mHandler;
	private ProgressDialog pd;

	private ArrayAdapter<CharSequence> mLMList;
	private ArrayAdapter<CharSequence> mAMList;
	
	private static ArrayList<String> availableLM = null;
	private static ArrayList<String> availableAM = null;

	ArrayList<String> available = new ArrayList<String>();
	ArrayList<String> unavailable = new ArrayList<String>();
	
	
   public void onCreate(Bundle savedInstanceState)
   {
		super.onCreate(savedInstanceState);
		

		/* Connect to CMU TTS server and get the list of voices available, 
		 * if we don't already have a file. 
		 */
	
		createDirectoryStructure();
		
		
		setContentView(R.layout.download);

		
		Spinner lmSpinner = (Spinner) findViewById(R.id.spnLM);
		Spinner amSpinner = (Spinner) findViewById(R.id.spnAM);
		mLMList = new ArrayAdapter<CharSequence>(this,android.R.layout.simple_spinner_item);
		mAMList = new ArrayAdapter<CharSequence>(this,android.R.layout.simple_spinner_item);
		
		lmSpinner.setAdapter(mLMList);
        amSpinner.setAdapter(mAMList);
        
        mLMList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAMList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        Button bDownloadLM =  (Button) findViewById(R.id.bDownloadLM);
        Button bDownloadAM = (Button) findViewById(R.id.bDownloadAM);
         
        bDownloadLM.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				downloadSelectedModel("lm");				
			}

		});
        
        bDownloadAM.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				downloadSelectedModel("am");				
			}

		});
        
        mHandler = new Handler();
        populateModels();

   }
   private boolean voiceAvailable(String[] voiceParams) {
   	String voxdataFileName = PS_DATA_PATH + "cg/"+voiceParams[0]+"/"+voiceParams[1]+"/"+voiceParams[2]+".cg.voxdata";
   	return Utility.pathExists(voxdataFileName);
   }
	
   public void saveUrlAsFile( final String url,  final String filename) {
		finished = false;
		success = false;
		new Thread() {
			public void run() {
				save(url, filename);
				
			}
		}.start();
	}

   
   private void createDirectoryStructure()
   {
	   //copy file to the local directory structure
		
	   //get the am.list, lm.list, current-conf, available LMs and AMs (only directory names)
	   		if(!Utility.pathExists(PS_DATA_PATH)) {
			// Create the directory.
			Log.e("PocketSphinx.DownloadData", "PocketSphinx directory missing. Trying to create it.");
			boolean success;
			try {
				Log.e("PocketSphinx.DownloadData",PS_DATA_PATH);
				success = new File(Environment.getExternalStorageDirectory()+"Android/data/"+"edu.cmu.pocketsphinx/").mkdirs();
			}
			catch (Exception e) {
				Log.e("PocketSphinx.DownloadData","Could not create directory structure. "+e.getMessage());
				success = false;
			}

			if(!success) {
				Log.e("PocketSphinx.DownloadData", "Failed");
				// Can't do anything without appropriate directory structure.
			}
		}
	   		
    String currentConf = PS_DATA_PATH+"current.conf";
    if(!Utility.pathExists(currentConf))
    {
    	Log.e("PocketSphinx.DownloadData", "current conf file doesn't exist. Try getting it from server.");
    	String currentConfURL = ROOT_URL+"current.conf";
    	boolean savedCurrentConf = Utility.saveUrlAsFile(currentConfURL,currentConf);
    	
    	if(!savedCurrentConf)
			Log.w("PocketSphinx.DownloadData","Could not download current conf file");
		else
			Log.w("PocketSphinx.DownloadData","Successfully downloaded current conf file");
    }

	String amListFile = PS_DATA_PATH+"am.list";
	if(!Utility.pathExists(amListFile)) {
		Log.e("PocketSphinx.DownloadData", "AM list file doesn't exist. Try getting it from server.");
		String amListURL = ROOT_URL+"am.list";

		boolean savedAMList = Utility.saveUrlAsFile(amListURL, amListFile);
		
		if(!savedAMList)
			Log.w("PocketSphinx.DownloadData","Could not update am list from server");
		else
			Log.w("PocketSphinx.DownloadData","Successfully updated am list from server");
	}
	
	
		
	
	//same with lm list
	String lmListFile = PS_DATA_PATH+"lm.list";
	if(!Utility.pathExists(lmListFile)) {
		Log.e("PocketSphinx.DownloadData", "AM list file doesn't exist. Try getting it from server.");
		String lmListURL = ROOT_URL+"lm.list";

		boolean savedLMList = Utility.saveUrlAsFile(lmListURL, lmListFile);

		if(!savedLMList)
			Log.w("PocketSphinx.DownloadData","Could not update lm list from server");
		else
			Log.w("PocketSphinx.DownloadData","Successfully updated lm list from server");
	}
	
	try {
		if(!Utility.pathExists(PS_DATA_PATH+"hmm"))
			if(! new File(PS_DATA_PATH+"hmm").mkdirs()) {
				abort();
			}
	} catch (Exception e) {
		abort();
	}
	

	try {
		if(!Utility.pathExists(PS_DATA_PATH+"lm"))
			if(! new File(PS_DATA_PATH+"lm").mkdirs()) {
				abort();
			}
	} catch (Exception e) {
		abort();
	}


   }
   private void populateModels()
   {
	   //show models through the spinners, ArrayList of data
	   
	   try {
			availableLM = Utility.readLines(PS_DATA_PATH+"lm.list");
			availableAM = Utility.readLines(PS_DATA_PATH+"am.list");
		} catch (IOException e) {
			Log.e("Flite.DownloadVoiceData","Could not read voice list");
		}
	   mAMList.clear();
		mLMList.clear();
		for(String s: availableLM) {
			
			String lmFile = PS_DATA_PATH + "lm/"+s;
			if(!Utility.pathExists(lmFile)) {
				// We need to install this lm.
				mLMList.add(s);			
			}
		}
		
		for(String s: availableAM) {
			
			String amFile = PS_DATA_PATH + "hmm/"+s;
			if(!Utility.pathExists(amFile)) {
				// We need to install this am.
				mAMList.add(s);			
			}
		}
		if(mAMList.getCount()==0) {
			TextView tv = (TextView) findViewById(R.id.txtInfoLM);
			tv.setText("All Acoustic Models correctly installed");

			Button b = (Button) findViewById(R.id.bDownloadAM);
			b.setEnabled(false);
		}
		if(mLMList.getCount()==0) {
			TextView tv = (TextView) findViewById(R.id.txtInfoAM);
			tv.setText("All Language Models correctly installed");

			Button b = (Button) findViewById(R.id.bDownloadLM);
			b.setEnabled(false);
		}

		
   }
   
   
   private void downloadSelectedModel(String LMOrAM)
   {
		final String url = "http://tts.speech.cs.cmu.edu/apappu/android/edu.cmu.pocketsphinx.temp.zip";
		final String filename = Environment.getExternalStorageDirectory()+"/Android/data/edu.cmu.pocketsphinx.temp.zip";

		
		Spinner sp = (Spinner) findViewById(R.id.spnLM);
		ArrayAdapter<CharSequence> list = mLMList;
		
		//if you find am then change the default to AM
		if(LMOrAM.equals("am")){
			sp = (Spinner) findViewById(R.id.spnAM);
			list = mAMList;
			
		}
		
		
		String selectedModel = (String) list.getItem(sp.getSelectedItemPosition());

		String[] voiceParams = selectedModel.split("-");
		if(voiceParams.length != 3) {
			Log.e("Flite.CheckVoiceData","Incorrect voicename:" + selectedModel);
			return;
		}

		String datapath = PS_DATA_PATH + "cg/" + voiceParams[0] + "/" + voiceParams[1];
		try {
			if(!Utility.pathExists(datapath))
				if(! new File(datapath).mkdirs()) {
					abort();
				}
		} catch (Exception e) {
			abort();
		}

	   downloadData(url,filename);
   }
	private void downloadData(final String url, final String filename){
		File f = new File(filename);
//		if(f.exists())
//		{
//			return;
//		}
		pd = new ProgressDialog(this);
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

		pd.show();
		
		final Builder voicedownloadSuccessStatus = new AlertDialog.Builder(this);
		voicedownloadSuccessStatus.setPositiveButton("Ok",null);
		new Thread() {
    		public void run() { 
    			saveUrlAsFile(url, filename);
    			while(totalFileLength == 0) {
    				if (finished)
    					break;
    			}
    			runOnUiThread(new Runnable() {

    				@Override
    				public void run() {
    					pd.setIndeterminate(false);
    					pd.setMax(totalFileLength);
    				}
    			});
    			int prev = 0;
    			while(!finished) {
    				if (finishedFileLength > prev) {
    					prev = finishedFileLength;
    					runOnUiThread(new Runnable() {

        					@Override
        					public void run() {
        						pd.setProgress(finishedFileLength);
        					}
        				});
    				}
    			}
    			runOnUiThread(new Runnable() {

					@Override
					public void run() {

		    			pd.dismiss();

					}
				});
    			if(!success) {
    				Log.e("PocketSphinxAndroid Demo", "data download failed!");
    				if(abortDownload)
    					voicedownloadSuccessStatus.setMessage("download aborted.");
    				else
    					voicedownloadSuccessStatus.setMessage("download failed! Check your internet settings.");
    			}
    			else {
    				voicedownloadSuccessStatus.setMessage("download succeeded");
    			}
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						voicedownloadSuccessStatus.show();
					}
				});
    			
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
    		Log.v("PocketSphinx.DownloadData","Trying to save the file of length "+contentLength);
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
	

	private String listRemoteDir(String directoryURL) throws Exception
	{
		 URL url = null;
		    BufferedReader reader = null;
		    StringBuilder stringBuilder;

		    try
		    {
		      url = new URL(directoryURL);
		      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		      
		      connection.setRequestMethod("GET");
		      
		      connection.setReadTimeout(15*1000);
		      connection.connect();

		      reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		      stringBuilder = new StringBuilder();

		      String line = null;
		      while ((line = reader.readLine()) != null)
		      {
		        stringBuilder.append(line + "\n");
		      }
		      return stringBuilder.toString();
		    }
		    catch (Exception e)
		    {
		      e.printStackTrace();
		      throw e;
		    }
		    finally
		    {
		      if (reader != null)
		      {
		        try
		        {
		          reader.close();
		        }
		        catch (IOException ioe)
		        {
		          ioe.printStackTrace();
		        }
		      }
		    }
	}


}