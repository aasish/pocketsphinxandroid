package edu.cmu.pocketsphinx.demo;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


public class PocketSphinxSettings extends Activity{
	private final static String PS_DATA_PATH = Environment.getExternalStorageDirectory()
    + "Android/data/edu.cmu.pocketsphinx/";
	private ArrayAdapter<CharSequence> mLMList;
	private ArrayAdapter<CharSequence> mAMList;
	
	private static ArrayList<String> availableLM = null;
	private static ArrayList<String> availableAM = null;

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
    
}

	private void populateModels() {
		// show models through the spinners, ArrayList of data

		try {
			availableLM = Utility.readLines(PS_DATA_PATH + "lm.list");
			availableAM = Utility.readLines(PS_DATA_PATH + "am.list");
		} catch (IOException e) {
			Log.e("PocketSphinx.DownloadData", "Could not read the model list");
		}

		mAMList.clear();
		mLMList.clear();
		for (String s : availableLM) {

			String lmFile = PS_DATA_PATH + "lm/" + s;
			if (!Utility.pathExists(lmFile)) {
				// We need to install this lm.
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
	}
	
	

}
