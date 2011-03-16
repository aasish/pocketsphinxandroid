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
import android.widget.Spinner;


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
			availableLM = Utility.readLines(PS_DATA_PATH+"lm.list");
			availableAM = Utility.readLines(PS_DATA_PATH+"am.list");
		} catch (IOException e) {
			Log.e("Flite.DownloadVoiceData","Could not read voice list");
		}
    setContentView(R.layout.config);

    
    Spinner LMspinner = (Spinner) findViewById(R.id.spnLM);
    Spinner AMspinner = (Spinner) findViewById(R.id.spnAM);
    ArrayAdapter<CharSequence> adapter;
   // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    //LMspinner.setAdapter(adapter);
}

}
