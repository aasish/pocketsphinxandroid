package edu.cmu.pocketsphinx.demo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;


public class PocketSphinxSettings extends Activity{
	private final static String PS_DATA_PATH = Environment.getExternalStorageDirectory()
    + "/Android/data/edu.cmu.pocketsphinx/";
	private ArrayAdapter<CharSequence> mLMList;
	private ArrayAdapter<CharSequence> mAMList;
	
	private static ArrayList<String> availableLM = null;
	private static ArrayList<String> availableAM = null;
	private String currentLM = "";
	private String currentAM = "";
	public void onCreate(Bundle savedInstanceState) {
    
		
		super.onCreate(savedInstanceState);
		try {
			availableLM = Utility.listFiles(PS_DATA_PATH+"lm");
			availableAM = Utility.listDirs(PS_DATA_PATH+"hmm");
		} catch (IOException e) {
			Log.e("PocketSphinx.Settings","Could not read directories");
		}
    setContentView(R.layout.config);

    
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
 
	Button bsetLMAM = (Button) findViewById(R.id.bsetLMAM);
	
	bsetLMAM.setOnClickListener(new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			setConfiguration();
		}

	});

	populateModels();
	lmSpinner.setSelection(mLMList.getPosition(currentLM));
	amSpinner.setSelection(mAMList.getPosition(currentAM));
	
}
	
	public void onBackPressed(){
		setResult(RESULT_OK);
		finish();
	}
	

	private void populateModels() {
		// show models through the spinners, ArrayList of data

		try {
			availableLM = Utility.listFiles(PS_DATA_PATH+"lm/");
			
			availableAM = Utility.listDirs(PS_DATA_PATH + "hmm/");
		} catch (IOException e) {
			Log.e("PocketSphinx.DownloadData", "Could not read the model list");
		}

		mAMList.clear();
		mLMList.clear();
		for (String s : availableLM) {

			String lmFile = PS_DATA_PATH + "lm/" + s;
			if (Utility.pathExists(lmFile)) {
				if(s.endsWith(".DMP"))
					s = s.replace(".DMP","");
				mLMList.add(s);
			}
		}

		for (String s : availableAM) {

			String amFile = PS_DATA_PATH + "hmm/" + s;
			if (Utility.pathExists(amFile)) {
				mAMList.add(s);
			}
		}
		String[] currentConf = getConfiguration();
		//String hmm, String lm, String dict)
		currentLM = currentConf[1].replace(".DMP","");
		currentAM = currentConf[0];
	}
	
	private String[] getConfiguration(){
		//read from config file
		String[] config = new String[3];
		String configFile = PS_DATA_PATH+"currentconf";
		boolean exists = (new File(configFile)).exists();
		if(exists){
			
			//open and write the config to the file
			
			ArrayList<String> lines;
			try {
				lines = Utility.readLines(configFile);
				if(lines.size()>0){
					Log.d("PocketSphinx.Settings","reading configuration");
					config = lines.get(0).split("\t");
					return config;
				}
				else
					Log.d("PocketSphinx.Settings","couldn't read the config file");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return config;	
		}
	private void setConfiguration(){
		Spinner spLM = (Spinner) findViewById(R.id.spnLM);
		Spinner spAM = (Spinner) findViewById(R.id.spnAM);

		String selectedLModel = (String) mLMList.getItem(spLM
				.getSelectedItemPosition());
		String selectedAModel = (String) mAMList.getItem(spAM.getSelectedItemPosition());
		
		//open and write the config to the file
		String configFile = PS_DATA_PATH+"currentconf";
		ArrayList<String> lines = new ArrayList<String>();
		lines.add(selectedAModel+"\t"+selectedLModel+".DMP\t"+selectedLModel+".dic");
		boolean success = Utility.writeToFile(configFile, lines);
		if(success)
			Log.i("PocketSphinx.Settings","configured successfully");
		else
			Log.e("PocketSphinx.Settings","couldn't write the config file");
	}
	
	
	
	
	

}
