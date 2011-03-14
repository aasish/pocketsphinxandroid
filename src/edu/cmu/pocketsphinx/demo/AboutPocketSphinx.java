package edu.cmu.pocketsphinx.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class AboutPocketSphinx extends Activity {

	TextView aboutPS;
	
	public void onCreate(Bundle savedInstanceState) {
		CharSequence title = "Carnegie Mellon PocketSphinx Help";
		this.setTitle(title);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		aboutPS = (TextView) findViewById(R.id.AboutPS);
		
	}
	
	
}
