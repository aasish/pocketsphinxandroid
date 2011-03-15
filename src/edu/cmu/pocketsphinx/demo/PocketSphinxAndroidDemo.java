package edu.cmu.pocketsphinx.demo;

import java.io.File;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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
	EditText edit_text;
	private ProgressDialog pd;
	private Handler mHandler;
	
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
		CharSequence title = "Carnegie Mellon PocketSphinx Demonstration";
		this.setTitle(title);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        mHandler = new Handler();

		this.downloader = new DownloadData();
		downloadData();
		
		Button b = (Button) findViewById(R.id.Button01);
		b.setOnTouchListener(this);
		this.performance_text = (TextView) findViewById(R.id.PerformanceText);
		this.which_pass = (TextView)findViewById(R.id.WhichPass);
		this.edit_text = (EditText) findViewById(R.id.EditText01);
		
		this.rec = new RecognizerTask();
		this.rec_thread = new Thread(this.rec);
		this.listening = false;
		this.rec.setRecognitionListener(this);
		this.rec_thread.start();

	}

	/** Called when partial results are generated. */
	public void onPartialResults(Bundle b) {
		final PocketSphinxAndroidDemo that = this;
		final String hyp = b.getString("hyp");
		that.edit_text.post(new Runnable() {
			public void run() {
				that.which_pass.setTextColor(Color.RED);
				that.which_pass.setText("First Pass");
				that.edit_text.setText(hyp);
			}
		});
	}

	/** Called with full results are generated. */
	public void onResults(Bundle b) {
		final String hyp = b.getString("hyp");
		final PocketSphinxAndroidDemo that = this;
		this.edit_text.post(new Runnable() {
			public void run() {
				that.which_pass.setTextColor(Color.GREEN);
				that.which_pass.setText("Final Pass");
				
				that.edit_text.setText(hyp);
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
		that.edit_text.post(new Runnable() {
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
	
		case R.id.configure :
			//showConfigureActivity();
			showFillerActivity();
		return true;
		
		case R.id.exit : 
			exitApplication();
		return true;
		
		case R.id.about :
			showAboutActivity();
		return true;
		
		
	  }
	  return false;

    }
	
	public void downloadData(){
		final String url = "http://tts.speech.cs.cmu.edu/apappu/android/edu.cmu.pocketsphinx.temp.zip";
		final String filename = Environment.getExternalStorageDirectory()+"/Android/data/edu.cmu.pocketsphinx.temp.zip";
		File f = new File(filename);
//		if(f.exists())
//		{
//			return;
//		}
		pd = new ProgressDialog(this);
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//		pd.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new OnClickListener() {
//
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				fdload.abort();
//			}
//		});
		pd.show();
		
		final Builder voicedownloadSuccessStatus = new AlertDialog.Builder(this);
		voicedownloadSuccessStatus.setPositiveButton("Ok",null);
		new Thread() {
    		public void run() { 
    			downloader.saveUrlAsFile(url, filename);
    			while(downloader.totalFileLength == 0) {
    				if (downloader.finished)
    					break;
    			}
    			runOnUiThread(new Runnable() {

    				@Override
    				public void run() {
    					pd.setIndeterminate(false);
    					pd.setMax(downloader.totalFileLength);
    				}
    			});
    			int prev = 0;
    			while(!downloader.finished) {
    				if (downloader.finishedFileLength > prev) {
    					prev = downloader.finishedFileLength;
    					runOnUiThread(new Runnable() {

        					@Override
        					public void run() {
        						pd.setProgress(downloader.finishedFileLength);
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
    			if(!downloader.success) {
    				Log.e("PocketSphinxAndroid Demo", "data download failed!");
    				if(downloader.abortDownload)
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
	
	public void exitApplication(){
	
	      Log.i("PocketSphinxAndroidDemo","terminated!!");
	       super.onDestroy();
	       this.finish();
	}
	
	public void setConfiguration(){
		
		//may be open a new activity
		//PocketSphinxSettings
	}

	public void showAboutActivity(){
		
		Intent i = new Intent(this, AboutPocketSphinx.class);
		startActivity(i);
	}
	public void showConfigureActivity(){
		
	}
	public void showFillerActivity(){
		Intent i = new Intent(this, FillerClass.class);
		startActivity(i);
	}
		



		
}