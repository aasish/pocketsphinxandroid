package edu.cmu.pocketsphinx.demo;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.*;
import android.graphics.Color;

public class PocketSphinxAndroidDemo extends Activity implements OnTouchListener, RecognitionListener {
	static {
		System.loadLibrary("pocketsphinx_jni");
	}

	/**
	 * Recognizer task, which runs in a worker thread.
	 */
	RecognizerTask rec;
	/**
	 * Download data
	 * 
	 */
	DownloadData downloader;
	/**
	 * Thread in which the recognizer task runs.
	 */
	Thread rec_thread;
	/**
	 * Time at which current recognition started.
	 */
	Date start_date;
	/**
	 * Number of seconds of speech.
	 */
	float speech_dur;
	/**
	 * Are we listening?
	 */
	boolean listening;
	/**
	 * Progress dialog for final recognition.
	 */
	ProgressDialog rec_dialog;
	/**
	 * Performance counter view.
	 */
	TextView performance_text;
	
	TextView which_pass;
	/**
	 * Editable text view.
	 */
	
	final static int ACTIVITY_CREATE = 1;

	EditText words;
	EditText number;
	EditText digit;
	EditText activeField;
	String activeFieldStr = "";
	
	private ProgressDialog pd;
	private final static String PS_DATA_PATH = Environment.getExternalStorageDirectory() + "/Android/data/edu.cmu.pocketsphinx/";

	
	/**
	 * Respond to touch events on the Speak button.
	 * 
	 * This allows the Speak button to function as a "push and hold" button, by
	 * triggering the start of recognition when it is first pushed, and the end
	 * of recognition when it is released.
	 * 
	 * @param v
	 *            View on which this event is called
	 * @param event
	 *            Event that was triggered.
	 */
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			this.activeField.setText("");
			
			this.which_pass.setTextColor(Color.RED);
			this.which_pass.setText("First Pass");
			start_date = new Date();
			this.listening = true;
			this.rec.start();
			break;
		case MotionEvent.ACTION_UP:
			Date end_date = new Date();
			long nmsec = end_date.getTime() - start_date.getTime();
			this.speech_dur = (float)nmsec / 1000;
			if (this.listening) {
				Log.d(getClass().getName(), "Showing Dialog");
				this.rec_dialog = ProgressDialog.show(PocketSphinxAndroidDemo.this, "", "Computing Final Pass .. ", true);
				this.rec_dialog.setCancelable(false);
				this.listening = false;
			}
			this.rec.stop();
			break;
		default:
			;
		}
		/* Let the button handle its own state */
		return false;
	}

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		
		final Context context = getApplicationContext();
		CharSequence text = "Hello toast!";
		final int duration = Toast.LENGTH_SHORT;

		
		
		CharSequence title = "Carnegie Mellon PocketSphinx Demonstration";
		this.setTitle(title);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if(!Utility.pathExists(PS_DATA_PATH+"currentconf")){
			downloadData();
		}
//		else{
//	    	  String[] defaultConfig = getConfiguration();
//		  		this.rec = new RecognizerTask(defaultConfig[0],defaultConfig[1],defaultConfig[2]);
//		  		this.rec_thread = new Thread(this.rec);
//		  		this.listening = false;
//		  		this.rec.setRecognitionListener(this);
//		  		this.rec_thread.start();
//		}
		
		Button b = (Button) findViewById(R.id.Button01);
		b.setOnTouchListener(this);
		this.performance_text = (TextView) findViewById(R.id.PerformanceText);
		this.which_pass = (TextView)findViewById(R.id.WhichPass);
		this.words = (EditText) findViewById(R.id.EditText01);
		//words.setInputType(0);
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(words.getWindowToken(), 0);
		words.setOnFocusChangeListener(new View.OnFocusChangeListener() {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus) {
		        if (hasFocus) {
		         //change thte config
		         final Toast toast = Toast.makeText(context, "Words", duration);
		         toast.show();
		         setConfiguration(1);
		         activeField = words;
		         activeFieldStr = "words";
		        }
		    }
		});
		this.number = (EditText) findViewById(R.id.EditText02);
		number.setInputType(0);
		imm.hideSoftInputFromWindow(number.getWindowToken(), 0);

		number.setOnFocusChangeListener(new View.OnFocusChangeListener() {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus) {
		        if (hasFocus) {
		         //change thte config
		        	final Toast toast = Toast.makeText(context, "Numbers", duration);
		        	toast.show();
		        	setConfiguration(2);
		        	activeField = number;
		        	activeFieldStr = "number";
		        }
		    }
		});
		this.digit = (EditText) findViewById(R.id.EditText03);
		digit.setInputType(0);
		imm.hideSoftInputFromWindow(digit.getWindowToken(), 0);

		digit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus) {
		        if (hasFocus) {
		        	final Toast toast = Toast.makeText(context, "Digits", duration);
		        	toast.show();
		        	setConfiguration(3);
		        	activeField = digit;
		        	activeFieldStr  = "digit";
		        	}
		    }
		});
		
	}
	
	

	/** Called when partial results are generated. */
	public void onPartialResults(Bundle b) {
		final PocketSphinxAndroidDemo that = this;
		final String hyp = b.getString("hyp");
		
		that.activeField.post(new Runnable() {
			public void run() {
				that.which_pass.setTextColor(Color.RED);
				that.which_pass.setText("First Pass");
				if(that.activeFieldStr.equals("number")||that.activeFieldStr.equals("digit"))
				{
					that.activeField.setText(convertWordsToNumbers(hyp));
				}
				else{
				that.activeField.setText(hyp);
				}
			}
		});
	}

	/** Called with full results are generated. */
	public void onResults(Bundle b) {
		final String hyp = b.getString("hyp");
		final PocketSphinxAndroidDemo that = this;
		this.words.post(new Runnable() {
			public void run() {
				that.which_pass.setTextColor(Color.GREEN);
				that.which_pass.setText("Final Pass");
				
				if(that.activeFieldStr.equals("number")||that.activeFieldStr.equals("digit"))
				{
					that.activeField.setText(convertWordsToNumbers(hyp));
				}
				else{
				that.activeField.setText(hyp);
				}
				Date end_date = new Date();
				long nmsec = end_date.getTime() - that.start_date.getTime();
				float rec_dur = (float)nmsec / 1000;
				that.performance_text.setText(String.format("%.2f seconds %.2f xRT",
															that.speech_dur,
															rec_dur / that.speech_dur));
				Log.d(getClass().getName(), "Hiding Dialog");
				that.rec_dialog.dismiss();
			}
		});
	}

	public void onError(int err) {
		final PocketSphinxAndroidDemo that = this;
		that.activeField.post(new Runnable() {
			public void run() {
				that.rec_dialog.dismiss();
			}
		});
	}
	
	public boolean onCreateOptionsMenu(Menu menu){

		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		

		return true;

		}
	
	public boolean onOptionsItemSelected (MenuItem item){

		switch (item.getItemId()){

		
		case R.id.download_data:
			downloadData();
			//showFillerActivity();
		
		return true;
	
//		case R.id.configure :
//			showConfigureActivity();
//			
//		return true;
		
		case R.id.exit : 
			exitApplication();
		return true;
		
		case R.id.about :
			showAboutActivity();
		return true;
		
		
	  }
	  return false;

    }
	
	public void downloadData()
	{
			Intent i = new Intent(this,DownloadData.class);
			startActivityForResult(i, 0);
	}
	
	public void exitApplication(){
	
	      Log.i("PocketSphinxAndroidDemo","terminated!!");
	       super.onDestroy();
	       this.finish();
	}
	
	public void setConfiguration(int type){
		//hardcoded
		//may be open a new activity
		//PocketSphinxSettings
			String[] defaultConfig = new String[3];
			switch(type){
			
			//default hub4 words
			case 1:
				defaultConfig[0] = "hub4wsj_sc_8k";
				defaultConfig[1] = "hub4.5000.DMP";
				defaultConfig[2] = "hub4.5000.dic";
				break;
			//numbers	
			case 2:
				defaultConfig[0] = "hub4wsj_sc_8k";
				defaultConfig[1] = "number.DMP";
				defaultConfig[2] = "number.dic";
				break;
			//digits	
			case 3:
				defaultConfig[0] = "tidigits";
				defaultConfig[1] = "tidigits.DMP";
				defaultConfig[2] = "tidigits.dic";
				break;
	
			}
	  		this.rec = new RecognizerTask(defaultConfig[0],defaultConfig[1],defaultConfig[2]);
	  		this.rec_thread = new Thread(this.rec);
	  		this.listening = false;
	  		this.rec.setRecognitionListener(this);
	  		this.rec_thread.start();
		
	}
	public String[] getConfiguration(){
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
		
	
	public void showAboutActivity(){
		
		Intent i = new Intent(this, AboutPocketSphinx.class);
		startActivity(i);
	}
	public void showConfigureActivity(){
		Intent i = new Intent(this,PocketSphinxSettings.class);
		startActivityForResult(i, 1);
	}
	public void showFillerActivity(){
		Intent i = new Intent(this, FillerClass.class);
		startActivity(i);
	}
	
		
	
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
	  super.onActivityResult(requestCode, resultCode, intent);
	  Log.d("PocketSphinx.Main", "result code: " + resultCode + " from " + requestCode);
	  if(resultCode == RESULT_OK){
	    switch(requestCode) {
	      case 0:
	      try{

//	    	if(rec_thread!=null && rec_thread.isAlive()){
//	    		rec_thread.stop();
//	    		rec_thread.destroy();
//	    	}
	    	 Log.d("PocketSphinx.Main", "result code: " + resultCode + " from " + requestCode);  
	  		String[] defaultConfig = getConfiguration();
	  		//Log.d("PocketSphinx.Main","configuration is " + defaultConfig[0]);
	  		//RecognizerTask(String hmm, String lm, String dict)
	  		this.rec = new RecognizerTask(defaultConfig[0],defaultConfig[1],defaultConfig[2]);
	  		
	  		this.rec_thread = new Thread(this.rec);
	  		this.listening = false;
	  		this.rec.setRecognitionListener(this);
	  		this.rec_thread.start();
	    	  
	      }catch(NullPointerException e){
	    	  e.printStackTrace();
	      }break;
	      
	      case 1:
//	    	  if(rec_thread!=null && rec_thread.isAlive()){
//		    		rec_thread.stop();
//		    		rec_thread.destroy();
//		    	}
	    	  String[] defaultConfig = getConfiguration();
		  		this.rec = new RecognizerTask(defaultConfig[0],defaultConfig[1],defaultConfig[2]);
		  		this.rec_thread = new Thread(this.rec);
		  		this.listening = false;
		  		this.rec.setRecognitionListener(this);
		  		this.rec_thread.start();
	    break;  	      	      
	    }
	    
	  }
	}
	
	protected static String convertWordsToNumbers(String hyp)
	{
		String updatedHyp = "";
		
		String[] wordNum = SegmentNumber.segmentNum(hyp.toLowerCase()).replace(" | ","%").split("%");
				
		for(String word:wordNum){
		try {
			word = word.replace("| ","");
			System.out.println(word);
			if(word.equals(""))
				continue;
			String dig = ConvertWordToNumber.WithSeparator(ConvertWordToNumber.parse(word));
			//System.out.println(dig);
			updatedHyp+=dig+" ";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		System.out.println(updatedHyp);
		return updatedHyp;
	}
		
}