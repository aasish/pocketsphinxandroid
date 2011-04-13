package edu.cmu.pocketsphinx.demo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class DownloadData extends Activity {

	public int totalFileLength = 0;
	public int finishedFileLength;
	private final static String PS_DATA_PATH = Environment
			.getExternalStorageDirectory()
			+ "/Android/data/edu.cmu.pocketsphinx/";
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

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*
		 * Connect to CMU TTS server and get the list of voices available, if we
		 * don't already have a file.
		 */

		createDirectoryStructure();

		setContentView(R.layout.download);

		Spinner lmSpinner = (Spinner) findViewById(R.id.spnLM);
		Spinner amSpinner = (Spinner) findViewById(R.id.spnAM);
		mLMList = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item);
		mAMList = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item);

		lmSpinner.setAdapter(mLMList);
		amSpinner.setAdapter(mAMList);

		mLMList
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAMList
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Button bDownloadLM = (Button) findViewById(R.id.bDownloadLM);
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

//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//	    if (keyCode == KeyEvent.KEYCODE_BACK) {
//
//	    	setResult(RESULT_OK);
//	    	finish();
//	        return true;
//	    }
//	    return super.onKeyDown(keyCode, event);
//	}
	
	public void onBackPressed(){
		setResult(RESULT_OK);
		finish();
	}
	
	public void saveUrlAsFile(final String url, final String filename) {
		finished = false;
		success = false;
		new Thread() {
			public void run() {
				save(url, filename);

			}
		}.start();
	}

	private void createDirectoryStructure() {
		// copy file to the local directory structure

		// get the am.list, lm.list, current-conf, available LMs and AMs (only
		// directory names)
		if (!Utility.pathExists(PS_DATA_PATH)) {
			// Create the directory.
			Log.e("PocketSphinx.DownloadData",
					"PocketSphinx directory missing. Trying to create it.");
			boolean success;
			try {
				Log.e("PocketSphinx.DownloadData", PS_DATA_PATH);
				success = new File(Environment.getExternalStorageDirectory()
						+ "/Android/data/" + "edu.cmu.pocketsphinx/").mkdirs();
			} catch (Exception e) {
				Log.e("PocketSphinx.DownloadData",
						"Could not create directory structure. "
								+ e.getMessage());
				success = false;
			}

			if (!success) {
				Log.e("PocketSphinx.DownloadData", "Failed");
				// Can't do anything without appropriate directory structure.
			}
		}

		String currentConf = PS_DATA_PATH + "currentconf";
		if (!Utility.pathExists(currentConf)) {
			Log
					.e("PocketSphinx.DownloadData",
							"current conf file doesn't exist. Try getting it from server.");
			String currentConfURL = ROOT_URL + "currentconf";
			boolean savedCurrentConf = Utility.saveUrlAsFile(currentConfURL,
					currentConf);

			if (!savedCurrentConf)
				Log.w("PocketSphinx.DownloadData",
						"Could not download current conf file");
			else
				Log.w("PocketSphinx.DownloadData",
						"Successfully downloaded current conf file");
		}

		String amListFile = PS_DATA_PATH + "am-list";
		if (!Utility.pathExists(amListFile)) {
			Log.e("PocketSphinx.DownloadData",
					"AM list file doesn't exist. Try getting it from server.");
			String amListURL = ROOT_URL + "am-list";

			boolean savedAMList = Utility.saveUrlAsFile(amListURL, amListFile);

			if (!savedAMList)
				Log.w("PocketSphinx.DownloadData",
						"Could not update am list from server");
			else
				Log.w("PocketSphinx.DownloadData",
						"Successfully updated am list from server");
		}

		// same with lm list
		String lmListFile = PS_DATA_PATH + "lm-list";
		if (!Utility.pathExists(lmListFile)) {
			Log.e("PocketSphinx.DownloadData",
					"AM list file doesn't exist. Try getting it from server.");
			String lmListURL = ROOT_URL + "lm-list";

			boolean savedLMList = Utility.saveUrlAsFile(lmListURL, lmListFile);

			if (!savedLMList)
				Log.w("PocketSphinx.DownloadData",
						"Could not update lm list from server");
			else
				Log.w("PocketSphinx.DownloadData",
						"Successfully updated lm list from server");
		}

		try {
			if (!Utility.pathExists(PS_DATA_PATH + "hmm"))
				if (!new File(PS_DATA_PATH + "hmm").mkdirs()) {
					abort();
				}
		} catch (Exception e) {
			abort();
		}

		try {
			if (!Utility.pathExists(PS_DATA_PATH + "lm"))
				if (!new File(PS_DATA_PATH + "lm").mkdirs()) {
					abort();
				}
		} catch (Exception e) {
			abort();
		}

	}

	private void populateModels() {
		// show models through the spinners, ArrayList of data

		try {
			availableLM = Utility.readLines(PS_DATA_PATH + "lm-list");
			availableAM = Utility.readLines(PS_DATA_PATH + "am-list");
			
		} catch (IOException e) {
			Log.e("PocketSphinx.DownloadData", "Could not read the model list");
		}

		mAMList.clear();
		mLMList.clear();
		for (String s : availableLM) {

			String lmFile = PS_DATA_PATH + "lm/" + s.split("\t")[0];
			if (!Utility.pathExists(lmFile)) {
				// We need to install this lm.
				if(!s.endsWith(".dic"))
					s = s.concat(".dic");
				mLMList.add(s);
			}
		}

		for (String s : availableAM) {

			String amFile = PS_DATA_PATH + "hmm/" + s;
			if (!Utility.pathExists(amFile)) {
				// We need to install this am.
				mAMList.add(s);
			}
		}
		if (mAMList.getCount() == 0) {
			TextView tv = (TextView) findViewById(R.id.txtInfoLM);
			tv.setText("All Acoustic Models correctly installed");

			Button b = (Button) findViewById(R.id.bDownloadAM);
			b.setEnabled(false);
		}
		if (mLMList.getCount() == 0) {
			TextView tv = (TextView) findViewById(R.id.txtInfoAM);
			tv.setText("All Language Models correctly installed");

			Button b = (Button) findViewById(R.id.bDownloadLM);
			b.setEnabled(false);
		}

	}

	private void downloadSelectedModel(String LMOrAM) {

		Spinner sp = (Spinner) findViewById(R.id.spnLM);
		ArrayAdapter<CharSequence> list = mLMList;
	
		
		// if you find am then change the default to AM
		Log.i("PocketSphinx.DownloadData", "downloading " + LMOrAM);
		
		if (LMOrAM.equals("am")) {
			sp = (Spinner) findViewById(R.id.spnAM);
			list = mAMList;
			
		}
		final String tempLMorAM = LMOrAM;
	    final String selectedModel = (String) list.getItem(sp
				.getSelectedItemPosition());

		Log.d("PocketSphinx.DownloadData", "downloading " + selectedModel);
		//datapath += selectedModel;
		//dataURL +=selectedModel;
		
		pd = new ProgressDialog(this);
		pd.setTitle("Downloading ... " + selectedModel);
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);

		pd.show();
		
		new Thread() {
			public void run() {
			
				
		try {
			String datapath = PS_DATA_PATH + "lm/";
			String dataURL = ROOT_URL + "lm/";
			
			if(tempLMorAM.equals("am")){
				datapath = PS_DATA_PATH + "hmm/";
				dataURL = ROOT_URL + "hmm/";
			}
			if (dataURL.contains("hmm")) {
				
				String modelPath = datapath+selectedModel;
				String modelURL = dataURL + selectedModel;
				
				downloadAM(modelURL,modelPath);
				} 
		
		
			 else{
				 downloadLM(dataURL,datapath,selectedModel);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (totalFileLength == 0) {
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
		while (!finished) {
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



	}
		}.start();
	}
	
	private  void downloadAM(String modelURL, String modelPath) throws Exception
	{
		totalFileLength = 0;
		finishedFileLength = 0;
		if (!Utility.pathExists(modelPath))
			if (!new File(modelPath).mkdirs()) {
				abort();
			}
		ArrayList<String> files = listRemoteDir(modelURL);
		totalFileLength = files.size();
		for (String f : files){
			Log.d("PocketSphinx.DownloadData", "downloading " +modelURL + "/" + f);

			Utility.saveUrlAsFile(modelURL + "/" + f, modelPath + "/" + f);
			finishedFileLength+=1;
		}
		success = true;
		finished = true;
	}
	private void downloadLM(String dataURL, String dataPath, String selectedModel)
	{
		totalFileLength =2;
		finishedFileLength = 0;
		
		
		String[] lmDic = selectedModel.split("\t");
		Log.d("PocketSphinx.DownloadData", "downloading " + lmDic[1]);
		Utility.saveUrlAsFile(dataURL+lmDic[1], dataPath+lmDic[1]);
		finishedFileLength+=1;
		
		Utility.saveUrlAsFile(dataURL+lmDic[0], dataPath+lmDic[0]);
		finishedFileLength+=1;

		success = true;
		finished = true;

//		Utility.saveUrlAsFile("http://tts.speech.cs.cmu.edu/android/general/voices.list", PS_DATA_PATH+"voices.list");
		
	}

	/*
	private void downloadData(final String url, final String filename) {

		pd = new ProgressDialog(this);
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

		pd.show();

		new Thread() {
			public void run() {
				saveUrlAsFile(url, filename);
				while (totalFileLength == 0) {
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
				while (!finished) {
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

			}
		}.start();

	}
*/
	private boolean save(String url, String filename) {
		try {

			abortDownload = false;
			Log.v("PocketSphinx.DownloadData", "Trying to save " + url + " as "
					+ filename);
			URL u = new URL(url);

			HttpURLConnection uc = (HttpURLConnection)u.openConnection();
			uc.setReadTimeout(15 * 1000);

			//URLConnection uc = u.openConnection();
			int contentLength = uc.getContentLength();
			
			uc.connect();
			
			totalFileLength = contentLength;
			Log.v("PocketSphinx.DownloadData",
					"Trying to save the file of length " + contentLength);
			finishedFileLength = 0;

			InputStream raw = uc.getInputStream();
			InputStream in = new BufferedInputStream(raw, 256);
			byte[] data = new byte[contentLength];
			int bytesRead = 0;
			int offset = 0;
			while (offset < contentLength) {
				bytesRead = in.read(data, offset, data.length - offset);
				if (bytesRead == -1)
					break;
				finishedFileLength += bytesRead;
				offset += bytesRead;
				if (abortDownload)
					break;
			}
			in.close();

			if (abortDownload) {
				Log.e("PocketSphinx.DownloadData",
						"File download aborted by user");
				success = false;
				finished = true;
				return false;
			}

//			if (offset != contentLength) {
//				throw new IOException("Only read " + offset
//						+ " bytes; Expected " + contentLength + " bytes");
//			}

			FileOutputStream out = new FileOutputStream(filename);
			out.write(data);
			out.flush();
			out.close();

			finished = true;
			success = true;
			return true;
		} catch (Exception e) {
			Log.e("PocketSphinx Utility", "Could not save url as file.: "
					+ e.getMessage());
			finished = true;
			return false;
		}
	}

	public void abort() {
		abortDownload = true;
	}

	private static ArrayList<String> listRemoteDir(String directoryURL)
			throws Exception {
		URL url = null;
		BufferedReader reader = null;
		ArrayList<String> files = new ArrayList<String>();

		try {
			url = new URL(directoryURL);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();

			connection.setRequestMethod("GET");

			connection.setReadTimeout(15 * 1000);
			connection.connect();

			reader = new BufferedReader(new InputStreamReader(connection
					.getInputStream()));

			String line = null;
			Pattern p = Pattern.compile(">" // end quote, then skip to end of
											// tag
					+ "([^<]+)" // name is data up to next tag
					+ "</a>");

			while ((line = reader.readLine()) != null) {
				if (!line.contains("[DIR]") && !line.contains("[ICO]")) {
					Matcher m = p.matcher(line);
					while (m.find()) {
						String tempFileName = m.group().replace("</a>", "")
								.replace(">", "");
						files.add(tempFileName);
						//Log.d("PocketSphinx.DownloadData", "downloading " + tempFileName);
					}
				}

			}
			return files;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

}