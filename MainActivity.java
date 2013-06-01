package com.webkozzer.lunartools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Button;

public class MainActivity extends Activity {

	private final String _fstabPath = "/system/etc/vold.fstab";
	private final String _buildpropPath = "/system/build.prop";
	
	private String _currentrom;
	private String _boot;
	private String _recovery;
	private String _extsd;
	private File _lunarDir;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Since the layout has been generated, now populate current rom text
		if (getCurrentRomInfo() == true){
			((TextView)findViewById(R.id.text_current_rom)).setText(_currentrom);
			((TextView)findViewById(R.id.text_current_boot)).setText(_boot);
			((TextView)findViewById(R.id.text_current_recovery)).setText(_recovery);
			((TextView)findViewById(R.id.text_current_sdcard)).setText(_extsd);
		} else {
			//If false was returned, then there should be an error message in _currentrom
			((TextView)findViewById(R.id.text_current_rom)).setText(_currentrom);			
		}
		
		//Set button click event handlers
		//Button 1 - flash saved boot
		((Button)(findViewById(R.id.button_flash_saved_boot))).setOnClickListener(new OnClickListener() {
		        public void onClick(View v) {
		            //call functional code method here
		        }
	    	});
		//Button 2 - backup / flash recovery
		((Button)(findViewById(R.id.button_flash_recovery))).setOnClickListener(new OnClickListener() {
		        public void onClick(View v) {
		            //call functional code method here
		        }
	    	});
		//Button 3 - update kernel with AnyKernel zImage
		((Button)(findViewById(R.id.button_update_kernel))).setOnClickListener(new OnClickListener() {
		        public void onClick(View v) {
		            //call functional code method here
		        }
	    	});
		//Button 4 - flash boot.img and reboot to recovery
		((Button)(findViewById(R.id.button_flash_boot_from_zip))).setOnClickListener(new OnClickListener() {
		        public void onClick(View v) {
		            //call functional code method here
		        }
	    	});
		//Button 5 - exit LunarTools
		((Button)(findViewById(R.id.button_exit_lunartools))).setOnClickListener(new OnClickListener() {
		        public void onClick(View v) {
		            //exit application
		        	System.exit(0);
		        }
	    	});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private boolean getCurrentRomInfo(){
		//Wrap in try...catch block so all possible errors are handled and don't lead to FC
		try{
				
			//Get runtime object
			Runtime rt = Runtime.getRuntime();
			//Start su session
			Process process = rt.exec("su");
			
			//Get boot
			process = rt.exec("grep -i \"boot\" /proc/emmc | sed 's/.*boot(.*)</recovery.*/\1/' | sed 's/:[^:]*$//'");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			_boot = reader.readLine();
			
			//Get recovery
			process = rt.exec("grep -i \"recovery\" /proc/emmc | sed 's/.*boot(.*)</recovery.*/\1/' | sed 's/:[^:]*$//'");
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			_recovery = reader.readLine();
			
			//Get external sd path from fstab
			File fstab = new File(_fstabPath);
			BufferedReader fstabReader = new BufferedReader(new FileReader(fstab));
			String fstabLine = null;
			while((fstabLine = fstabReader.readLine()) != null){
				if (fstabLine.contains("/devices/platform/msm_sdcc.3/mmc_host/mmc2") == true){
					//We've got the line we need, so split line into array of strings using space as split char
					String [] sdcardLine = fstabLine.split(" ");
					if (sdcardLine != null && sdcardLine.length >= 3){
						//We know the absolute path is the 3rd item (because that's how fstabs are structured)
						_extsd = sdcardLine[2];
					}
				}			
			}
			//Close the file
			if (fstabReader != null){
				fstabReader.close();
			}	
			//Check to see if LunarTools already has a directory on the sd card
			_lunarDir = new File(_extsd + "/lunar/");
			if (!_lunarDir.exists()){
				//Dir doesn't exist, so create it
				_lunarDir.mkdir();
			}
			
			//Declare rom strings
			String currentrom1 = null;
			String currentrom2 = null;
			String currentrom3 = null;
			String currentrom4 = null;
			String currentrom5 = null;
			//String currentrom6;
			String currentrom7 = null;
			String currentrom8 = null;
			
			//Read through build.prop
			File buildProp = new File(_buildpropPath);
			BufferedReader propReader = new BufferedReader(new FileReader(buildProp));
			String propLine = null;
			while((propLine = propReader.readLine()) != null){
				if (propLine.contains("ro.aa.taskid=") == true) {
					currentrom1 = propLine.replace("ro.aa.taskid=", "");
				}
				if (propLine.contains("otaupdater.otaver=") == true){
					currentrom2 = propLine.replace("otaupdater.otaver=", "");
				}
				if (propLine.contains("otaupdater.otaid=") == true) {
					currentrom3 = propLine.replace("otaupdater.otaid=", "");
				}
				if (propLine.contains("ro.goo.version=") == true){
					currentrom4 = propLine.replace("ro.goo.version=", "");
				}
				if (propLine.contains("ro.goo.developerid=") == true) {
					currentrom5 = propLine.replace("ro.goo.developerid=", "");
				}
				if (propLine.contains("ro.cm.version=") == true) {
					currentrom7 = "CM " + propLine.replace("ro.cm.version=", "");
				}
				if (propLine.contains("ro.build.version.release=") == true) {
					currentrom8 = propLine.replace("ro.build.version.release=", "");
				}
			}
			//Done looping through build.prop, so close the file
			if (propReader != null){
				propReader.close();
			}
			
			//Sense
			if (currentrom1 != null) {
				if (currentrom1.equals("264016")){
					_currentrom = "Sense-3.6-OTA";
				}
				if (currentrom1.equals("275361") || currentrom1.equals("311390")){
					_currentrom = "Sense-3.6-Global";
				}
				if (currentrom1.equals("290923") || currentrom1.equals("249167")){
					_currentrom = "Sense-3.6-_Sense4.X";
				}
				if (currentrom1.equals("263510")){
					_currentrom = "Sense4.X";
				}
			}
			//OTA updater
			if (currentrom3 != null && currentrom3.length() > 0) {
				_currentrom = currentrom3;
				if (currentrom2 != null && currentrom2.length() > 0){
					_currentrom = _currentrom + "-v" + currentrom2;
				}
			}
			//Goo
			if (currentrom5 != null && currentrom5.length() > 0) {
				_currentrom = currentrom5;
				if (currentrom4 != null && currentrom4.length() > 0){
					_currentrom = _currentrom + "-v" + currentrom4;
				}
			}
			//CM
			if (currentrom7 != null && currentrom7.length() > 0) {
				_currentrom = currentrom7.replace("-UNOFFICIAL", "").replace("-vigor", "");
			}
			//Append the Android version
			if (currentrom8 != null && currentrom8.length() > 0){
				_currentrom = _currentrom + "-" + currentrom8;
			}
		} catch (Exception e){
			//Put the error message into the current rom string so the user can see what happened
			_currentrom = _currentrom + " [Error: " + e.getMessage() + "]";
			//Exception thrown, so return false
			return false;
		}	
		//We made it to the end without an exception, so return true (success)
		return true;	
	}
}
