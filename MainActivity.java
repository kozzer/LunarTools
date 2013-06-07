package com.webkozzer.lunartools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private final String _fstabPath = "/system/etc/vold.fstab";
	private final String _buildpropPath = "/system/build.prop";
	private final String _blockPath = "/dev/block/";
	
	private Context _context;
	
	private String _currentrom;
	private String _boot;
	private String _recovery;
	private String _extsd;
	private File _lunarDir;
	
	private List<File> _bootImgs;
	private File _chosenBoot;
	private boolean _setValuesViaCommandLine;
	
	private List<File> _recoveryImgs;
	private File _chosenRecovery;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Save a reference to the app's main activity context for when we go down the rabbit hole of dialogs
		_context = this;
		
		//Since the layout has been generated, now populate current rom text
		if (getCurrentRomInfo() == true){
			((TextView)findViewById(R.id.text_current_rom)).setText(_currentrom);
			((TextView)findViewById(R.id.text_current_boot)).setText(_blockPath + _boot);
			((TextView)findViewById(R.id.text_current_recovery)).setText(_blockPath + _recovery);
			((TextView)findViewById(R.id.text_current_sdcard)).setText(_lunarDir.getAbsolutePath());
		} else {
			//If false was returned, then there should be an error message in _currentrom
			((TextView)findViewById(R.id.text_current_rom)).setText(_currentrom);			
		}
		
		//Set button click event handlers
		//Button 1 - flash saved boot
		((Button)(findViewById(R.id.button_flash_saved_boot))).setOnClickListener(new OnClickListener() {
		        public void onClick(View v) {
		            //call functional code method here
		        	menu1_FlashSavedBoot();
		        }
	    	});
		//Button 2 - backup / flash recovery
		((Button)(findViewById(R.id.button_flash_recovery))).setOnClickListener(new OnClickListener() {
		        public void onClick(View v) {
		            //call functional code method here
		        	menu2_FlashRecovery();
		        }
	    	});
		//Button 3 - update kernel with AnyKernel zImage
		((Button)(findViewById(R.id.button_update_kernel))).setOnClickListener(new OnClickListener() {
		        public void onClick(View v) {
		            //call functional code method here
		        	menu3_UpdateKernel();
		        }
	    	});
		//Button 4 - flash boot.img and reboot to recovery
		((Button)(findViewById(R.id.button_flash_boot_from_zip))).setOnClickListener(new OnClickListener() {
		        public void onClick(View v) {
		            //call functional code method here
		        	menu4_FlashBootFromZipAutoReboot();
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
			// mmcblk0p22: 01000000 00000200 "boot"
			//process = rt.exec("grep -i \"boot\" /proc/emmc | sed 's/.*boot(.*)</recovery.*/\1/' | sed 's/:[^:]*$//'");
			process = rt.exec("grep -i \"boot\" /proc/emmc");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			_boot = reader.readLine();
			if (_boot != null && _boot.contains("mmcblk") == true){
				//assume that we got what we need
				String [] boot = _boot.split(" ");
				if (boot != null && boot.length > 0){
					for(int i=0;i<boot.length;i++){
						if (boot[i].contains("mmcblk") == true){
							 _boot = boot[i].replace(":",  "");
						}
					}
				}
			}
			
			//Get recovery
			// mmcblk0p23: 00fffc00 00000200 "recovery"
			//process = rt.exec("grep -i \"recovery\" /proc/emmc | sed 's/.*boot(.*)</recovery.*/\1/' | sed 's/:[^:]*$//'");
			process = rt.exec("grep -i \"recovery\" /proc/emmc");
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			_recovery = reader.readLine();
			if (_recovery != null && _recovery.contains("mmcblk") == true){
				//assume that we got what we need
				String [] recovery = _recovery.split(" ");
				if (recovery != null && recovery.length > 0){
					for(int i=0;i<recovery.length;i++){
						if (recovery[i].contains("mmcblk") == true){
							_recovery = recovery[i].replace(":",  "");
						}
					}
				}
			}
			
			//Get external sd path from fstab
			File fstab = new File(_fstabPath);
			BufferedReader fstabReader = new BufferedReader(new FileReader(fstab));
			String fstabLine = null;
			while((fstabLine = fstabReader.readLine()) != null){
				if (fstabLine.contains("/devices/platform/msm_sdcc.") == true){
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

	private void menu1_FlashSavedBoot(){
		
		try{
			//Get the file listing Lunar's directory (/sdcard/lunar)
			File[] bootFiles = _lunarDir.listFiles();
			//Loop through the files
			if (bootFiles != null && bootFiles.length > 0){
				Toast.makeText(_context, "Found " + Integer.toString(bootFiles.length) + " files in " + _lunarDir.getAbsolutePath() + "...grabbing any .img's/.cfg's now", Toast.LENGTH_SHORT).show();
				_bootImgs = new ArrayList<File>();
				for(int i = 0; i < bootFiles.length; i++){
					String fileName = bootFiles[i].getName();
					//If file name is boot*.img or *.cfg, then pull it
					if (fileName.length() > 4 && 
							((fileName.substring(0, 4).equals("boot") == true && fileName.substring(fileName.length() - 4, fileName.length()).equals(".img") == true) ||
							(fileName.substring(fileName.length() - 4, fileName.length()).equals(".cfg") == true))) {
						//We found a file that ends in ".img"
						_bootImgs.add(bootFiles[i]);
					}
				}
			}
			//Now look for cfg's in .sbin
			File[] sbinFiles = (new File("/sbin")).listFiles();
			//Loop through the files
			if (sbinFiles != null && sbinFiles.length > 0){
				Toast.makeText(_context, "Found " + Integer.toString(sbinFiles.length) + " files in /sbin...grabbing any .cfg's now", Toast.LENGTH_SHORT).show();
				//Make sure _bootImg's and if not create empty list
				if (_bootImgs == null){
					_bootImgs = new ArrayList<File>();
				}
				for(int i = 0; i < sbinFiles.length; i++){
					String fileName = sbinFiles[i].getName();
					//If file name is *.cfg, then pull it
					if (fileName.length() > 4 && (fileName.substring(fileName.length() - 4, fileName.length()).equals(".cfg") == true)) {
						//We found a file that ends in ".cfg"
						_bootImgs.add(bootFiles[i]);
					}
				}
			}
			
			//If we've got some .img files, ask user which one they want
			if (_bootImgs != null && _bootImgs.size() > 0){
				//Ask user which img to flash
				menu1_ShowSavedBoots();
			} else {
				//Tell user no img files were found
				showMessageWithNoActions("No saved boots to flash", "No saved boot.img or cfg files were found.\n\nPlease make sure your file is in\n" + _lunarDir.getAbsolutePath());
			}
						
		} catch (Exception e) {
			//display message
			showMessageWithNoActions("Error", "Error in menu1_FlashSavedBoot(): \n" + e.getMessage());
		}
	}
	
	private void menu1_ShowSavedBoots(){
		//Move the list from a list of File objects to an array of String objects to be displayed to the user
		String [] fileArray = new String [_bootImgs.size()];
		for(int i=0; i<_bootImgs.size();i++){
			fileArray[i] = _bootImgs.get(i).getName();
		}
		//Create the dialog box
		AlertDialog.Builder builder = new AlertDialog.Builder(this)		
		.setTitle("Select img or cfg to flash:")
		.setItems(fileArray, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				dialog.dismiss();
				//save which boot.img was chosen
				_chosenBoot = _bootImgs.get(which);
				//Ask user how they want to set values
				menu1_ShowSetValuesDialog(which);		
			}
		});
		//Display the dialog to the user
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void menu1_ShowSetValuesDialog(int choice){
		//Now that we have a chosen boot.img, ask the user if they want to set values via command line or Init.d
		String [] setValues = {"Set values via Command Line", "Set values via Init.d"};
		AlertDialog.Builder builder = new AlertDialog.Builder(_context)
		.setTitle("How to set values?")
		.setItems(setValues, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				dialog.dismiss();
				if (which == 0){
					//Set via command line
					_setValuesViaCommandLine = true;
				} else {
					//Set via Init.d
					_setValuesViaCommandLine = false;
				}
				//Now that we know how they want to set the values, ask user if they want to flash or save for later
				menu1_ShowFlashOrSaveDialog();
			}
		});
		//Display the dialog to the user
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void menu1_ShowFlashOrSaveDialog(){
		//Simply tells user a message, with no further action to be taken
		new AlertDialog.Builder(this)
	    .setTitle("Flash Now?")
	    .setMessage("Flash [" + _chosenBoot.getName() + "] now, or save for later?\n\nIf you choose to flash now, there's no going back!")
	    .setPositiveButton("Flash Now", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            //User wants to flash now
	        	menu1_FlashChosenBoot();
	        }
	     })
	     .setNegativeButton("Save for Later", new DialogInterface.OnClickListener(){
	    	public void onClick(DialogInterface dialog, int which){
	    		//User wants to save for later
	    		menu1_SaveBootChoices();
	    	}
	     })
	     .show();		
	}
	
	private void menu1_FlashChosenBoot(){
		//We have all the info we need to flash, so try to flash!
		try {
			//Get runtime object
			Runtime rt = Runtime.getRuntime();
			
			Toast.makeText(_context, "starting su session", Toast.LENGTH_SHORT).show();
			
			//Start su session
			Process process = rt.exec("su");

			if (_setValuesViaCommandLine == true){
				//Set values via command line
				if (_chosenBoot.getName().substring(_chosenBoot.getName().length() - 4, _chosenBoot.getName().length()).equals(".cfg") == true){
					
					Toast.makeText(_context, "flashing from cfg file", Toast.LENGTH_SHORT).show();
					
					//config file
					process = rt.exec("cd " + _lunarDir.getAbsolutePath());
					process = rt.exec("dd if=" + _blockPath + _boot + " of=" + _lunarDir.getAbsolutePath() + "/tempboot.img 2>> lunardebug.log");
					process = rt.exec("abootimg -x " + _lunarDir.getAbsolutePath() + "/tempboot.img >> lunardebug.log");
					process = rt.exec("sed -i -e \"s/gov.*//g\" bootimg.cfg");
					process = rt.exec("file_chosen=$(cat " + _chosenBoot + ")");
					process = rt.exec("sed -i -e \"$ s/$/" + _chosenBoot + "/g\" bootimg.cfg");
					process = rt.exec("cat bootimg.cfg >> lunardebug.log");
					process = rt.exec("abootimg -u tempboot.img -f bootimg.cfg >> lunardebug.log");
					process = rt.exec("dd if=" + _lunarDir.getAbsolutePath() + "/tempboot.img of=" + _blockPath + _boot + " 2>> lunardebug.log");
					
					Toast.makeText(_context, "removing old files", Toast.LENGTH_SHORT).show();
					
					process = rt.exec("rm bootimg.cfg");
					process = rt.exec("rm initrd.img");
					process = rt.exec("rm zImage");
					process = rt.exec("rm tempboot.img");
				} else if (_chosenBoot.getName().substring(_chosenBoot.getName().length() - 4, _chosenBoot.getName().length()).equals(".img") == true){
					
					Toast.makeText(_context, "flashing " + _chosenBoot.getName(), Toast.LENGTH_SHORT).show();
					
					//Actual boot.img file, move to the boot block
					//process = rt.exec("dd if=" + _chosenBoot.getName() + " of=" + _blockPath + _boot);
					copyFile(_chosenBoot, new File(_blockPath + _boot));								
				}
				//Delete dalvik data
				Toast.makeText(_context, "clearing dalvik-cache", Toast.LENGTH_SHORT).show();
				process = rt.exec("rm -r /data/dalvik-cache");
				
				//Check if this is a system app, if so then auto reboot phone, otherwise user has to do it manually
				if (isLunarToolsRunningAsSystemApp() == true){
					//Tell user they're rebooting to recovery
					menu1_tellUserThenReboot();
				} else {
					//Tell user they need to reboot to recovery immediately
					showMessageWithNoActions("Reboot to Recovery", "You must reboot to recovery IMMEDIATELY.\n\nPlease exit LunarTools and reboot to recovery NOW.");
				}
			} else {
				//Set values via init.d
				
				//Get current governor, max freq, min freq, i/o scheduler, sweep 2 wake settings
				process = rt.exec("grep -i \"gov=\"" + _chosenBoot + " | sed \"s/ /\n/g\" | sed \"s/gov=//g\" | head -n1 | tail -n1");
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String gov = reader.readLine();
				process = rt.exec("grep -i \"maxkhz=\" " + _chosenBoot + " | sed \"s/ /\n/g\" | sed \"s/maxkhz=//g\" | head -n2 | tail -n1");
				String maxFreq = reader.readLine();
				process = rt.exec("grep -i \"minkhz=\" " + _chosenBoot + " | sed \"s/ /\n/g\" | sed \"s/minkhz=//g\" | head -n3 | tail -n1");
				String minFreq = reader.readLine();
				process = rt.exec("grep -i \"scheduler=\" " + _chosenBoot + " | sed \"s/ /\n/g\" | sed \"s/scheduler=//g\" | head -n4 | tail -n1");	
				String ioScheduler = reader.readLine();
				process = rt.exec("grep -i \"s2w=\" " + _chosenBoot + " | sed \"s/ /\n/g\" | sed \"s/s2w=//g\" | tail -n1");
				String s2Wake = reader.readLine();
				
				//Send those values to init.d
				process = rt.exec("echo '#!/system/bin/sh' > /system/etc/init.d/99lunar");
				process = rt.exec("echo 'echo ''\"'" + ioScheduler + "'\"' '> /sys/block/mmcblk0/queue/scheduler' >> /system/etc/init.d/99lunar");
				process = rt.exec("echo 'echo \"'" + gov + "'\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor' >> /system/etc/init.d/99lunar");
				process = rt.exec("echo 'echo \"'" + minFreq + "'\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq' >> /system/etc/init.d/99lunar");
				process = rt.exec("echo 'echo \"'" + maxFreq + "'\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq' >> /system/etc/init.d/99lunar");
				
				//Get directory objects for 3 cpu's
				File cpu1 = new File("/sys/devices/system/cpu/cpu1/");
				File cpu2 = new File("/sys/devices/system/cpu/cpu2/");
				File cpu3 = new File("/sys/devices/system/cpu/cpu3/");
				//Write out cpu settings (if cpu exists) to lunar init.d
				if (cpu1.exists() && cpu1.isDirectory()){
					process = rt.exec("echo 'echo \"'" + gov + "'\" > /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor' >> /system/etc/init.d/99lunar");
					process = rt.exec("echo 'echo \"'" + minFreq + "'\" > /sys/devices/system/cpu/cpu1/cpufreq/scaling_min_freq' >> /system/etc/init.d/99lunar");
					process = rt.exec("echo 'echo \"'" + maxFreq + "'\" > /sys/devices/system/cpu/cpu1/cpufreq/scaling_max_freq' >> /system/etc/init.d/99lunar");
				}
				if (cpu2.exists() && cpu2.isDirectory()){
					process = rt.exec("echo 'echo \"'" + gov + "'\" > /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor' >> /system/etc/init.d/99lunar");
					process = rt.exec("echo 'echo \"'" + minFreq + "'\" > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq' >> /system/etc/init.d/99lunar");
					process = rt.exec("echo 'echo \"'" + maxFreq + "'\" > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq' >> /system/etc/init.d/99lunar");
				}
				if (cpu3.exists() && cpu3.isDirectory()){
					process = rt.exec("echo 'echo \"'" + gov + "'\" > /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor' >> /system/etc/init.d/99lunar");
					process = rt.exec("echo 'echo \"'" + minFreq + "'\" > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq' >> /system/etc/init.d/99lunar");
					process = rt.exec("echo 'echo \"'" + maxFreq + "'\" > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq' >> /system/etc/init.d/99lunar");
				}
				//Move sweep 2 wake setting if it exists
				File sweep2wake = new File("/sys/android_touch/sweep2wake");
				if (sweep2wake.exists() && sweep2wake.isFile()){
					process = rt.exec("echo 'echo '$s2w' > /sys/android_touch/sweep2wake' >> /system/etc/init.d/99lunar");
				}
				//Set permissions for lunar init.d 
				process = rt.exec("chmod 755 /system/etc/init.d/99lunar");
				
				//Display success message
				showMessageWithNoActions("Success", "No error while creating Lunar init.d");
			}
		} catch (Exception e){
			//Display message to user
			showMessageWithNoActions("Error", "Error trying to flash [" + _chosenBoot + "]:\n" + e.getMessage());
		}
	}
	
	private void menu1_SaveBootChoices(){
		//We have all the info we need, but user wants to save, so save to cfg file
		try {
			//Not sure what to do here..  if anything?	
		} catch (Exception e){
			//nada
		}		
	}

	private void menu1_tellUserThenReboot(){
		//Simply tells user a message, with no further action to be taken
		new AlertDialog.Builder(this)
	    .setTitle("Reboot to Recovery")
	    .setMessage("Your phone is about to reboot to recovery.\n\nTap 'OK' to Reboot.")
	    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	        	try{
	        		//reboot the phone
	        		//Runtime.getRuntime().exec(new String[]{"su","-c","reboot now"});
	        		Runtime.getRuntime().exec(new String[]{"su","-c","busybox reboot recovery"});
	        	} catch (Exception e){
	        		//handle later...
	        	}
	        }
	     })
	     .show();
	}
	
	private void menu2_FlashRecovery(){
		//Ask user if they want to back up current recovery
		new AlertDialog.Builder(this)
	    .setTitle("Back up Recovery?")
	    .setMessage("Do you want to back up your current recovery?")
	    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            //User wants to back up recovery
	        	menu2_backupCurrentRecovery();
	    		//Now actually flash new recovery
	    		menu2_flashNewRecovery();
	        }
	     })
	     .setNegativeButton("No", new DialogInterface.OnClickListener(){
	    	public void onClick(DialogInterface dialog, int which){
	    		//User doesn't want to back up recovery
	    		Toast.makeText(_context, "Skipping creation of a recovery backup", Toast.LENGTH_SHORT).show();
	    		//Now actually flash new recovery
	    		menu2_flashNewRecovery();
	    	}
	     })
	     .show();		
	}
	
	private void menu2_backupCurrentRecovery(){
		try {			
			Toast.makeText(_context, "starting su session", Toast.LENGTH_SHORT).show();
		
			//Start su session
			Runtime rt = Runtime.getRuntime();
			Process process = rt.exec("su");
			
			//CD to lunar dir
			process = rt.exec("cd " + _lunarDir.getAbsolutePath());
			Toast.makeText(_context, "After the cd...", Toast.LENGTH_SHORT).show();
			
			//Make temp recovery backup directory under lunar tools 	
			String recoveryPath = _lunarDir.getAbsolutePath() + "/recoverytemp";
			process = rt.exec("mkdir " + recoveryPath);
			//Test that path exists
			File recoveryDir = new File(recoveryPath);
			if (!recoveryDir.exists() || !recoveryDir.isDirectory()){
				//display error message and abort process
				showMessageWithNoActions("Dir Create Fail", "Failed to create " + recoveryPath + "!\n\nCanceling process.");
			} else {
				
				//Backup recovery img
				process = rt.exec("dd if=" + _blockPath + _recovery + " of=" + recoveryPath + "/recoverytemp.img 2>> lunardebug.log");
				process = rt.exec("cd " + recoveryPath);
				process = rt.exec("abootimg -x recoverytemp.img");
				
				//Get the recovery file size
				process = rt.exec("stat -c %s zImage");
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String strRecoverySize1 = reader.readLine();
				int recoverySize1 = Integer.parseInt(strRecoverySize1);
				process = rt.exec("stat -c %s initrd.img");
				String strRecoverySize2 = reader.readLine();
				int recoverySize2 = Integer.parseInt(strRecoverySize2);
				int recoverySize = recoverySize1 + recoverySize2 + 6120;
				
				//What is this line doing?  Why printf? Does line below accomplish same thing?
				//process = rt.exec("printf -v recoveryhexsize \"%x\" \"" + Integer.toString(recoverySize) + "\"");				
				String recoveryHexSize = Integer.toHexString(recoverySize);
				
				process = rt.exec("sed -i \"s/bootsize.*/bootsize = 0x" + recoveryHexSize + "/g\" bootimg.cfg");
				process = rt.exec("abootimg -u recoverytemp.img -f bootimg.cfg");
				
				//Get/create the back up's destination path & directory
				String dateString = String.format("%1$tY%1$tm%1$te", new Date());	//should return current date of yyyyMMdd
				String backupPath = _lunarDir.getAbsolutePath() + "/recovery_" + dateString;
				process = rt.exec("cd ..");
				File backupDir = new File(backupPath);
				if (!backupDir.exists()){
					process = rt.exec("mkdir " + backupPath);
				}
				//Move the recovery image and delete the temp
				process = rt.exec("mv " + recoveryPath + "/recoverytemp.img " + backupPath + "/recovery.img");
				process = rt.exec("rm -r " + recoveryPath);
	
				//Show user success message, then move on to flashing new recovery
				Toast.makeText(_context, "Your current recovery was saved to " + backupPath + " dir as recovery.img", Toast.LENGTH_SHORT);
			}
		} catch (Exception e){
			//Deal with later, for now just display message
			showMessageWithNoActions("Error", "There was an error while attempting to back up your recovery!\n\nMessage: " + e.getMessage());
		}
	}
	
	private void menu2_flashNewRecovery(){
		//Look for recovery img's in Lunar's directory
		try{
			//Get the file listing Lunar's directory (/sdcard/lunar)
			File[] recoveryFiles = _lunarDir.listFiles();
			//Loop through the files
			if (recoveryFiles != null && recoveryFiles.length > 0){
				_recoveryImgs = new ArrayList<File>();
				for(int i = 0; i < recoveryFiles.length; i++){
					String fileName = recoveryFiles[i].getName();
					//Look for files starting with 'recovery' and ending in '.img'
					if (fileName.length() > 11 && (fileName.substring(0, 8).equals("recovery") == true && fileName.substring(fileName.length() - 4, fileName.length()).equals(".img") == true)) {
						//We found a file
						_recoveryImgs.add(recoveryFiles[i]);
					}
				}
			}
			
			//If we've got some .img files, ask user which one they want
			if (_recoveryImgs != null && _recoveryImgs.size() > 0){
				//Ask user which img to flash
				menu2_ShowRecoveryImages();
			} else {
				//Tell user no img files were found
				showMessageWithNoActions("No saved recovery to flash", "No saved recovery img files were found.\n\nPlease make sure your file is in\n" + _lunarDir.getAbsolutePath());
			}
						
		} catch (Exception e) {
			//display message
		}

	}
	
	private void menu2_ShowRecoveryImages(){
		//Move the list from a list of File objects to an array of String objects to be displayed to the user
		String [] fileArray = new String [_recoveryImgs.size()];
		for(int i=0; i<_recoveryImgs.size();i++){
			fileArray[i] = _recoveryImgs.get(i).getName();
		}
		//Create the dialog box
		AlertDialog.Builder builder = new AlertDialog.Builder(this)		
		.setTitle("Select recovery img to flash:")
		.setItems(fileArray, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				dialog.dismiss();
				//save which boot.img was chosen
				_chosenRecovery = _recoveryImgs.get(which);
				//Ask user if they want to flash now
				menu2_ShowFlashOrSaveDialog();		
			}
		});
		//Display the dialog to the user
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void menu2_ShowFlashOrSaveDialog(){
		//Simply tells user a message, with no further action to be taken
		new AlertDialog.Builder(this)
	    .setTitle("Flash Now?")
	    .setMessage("Flash [" + _chosenRecovery.getName() + "] now, or save for later?\n\nIf you choose to flash now, there's no going back!")
	    .setPositiveButton("Flash Now", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            //User wants to flash now
	        	menu2_FlashChosenRecovery();
	        }
	     })
	     .setNegativeButton("Save for Later", new DialogInterface.OnClickListener(){
	    	public void onClick(DialogInterface dialog, int which){
	    		//User wants to save for later - nothing to do???
	    	}
	     })
	     .show();		
	}
	
	private void menu2_FlashChosenRecovery(){
		//Get runtime object
		Runtime rt = Runtime.getRuntime();
		
		try {
			//Start su session
			//Process process = rt.exec("su");
			//Flash recovery
			//process = rt.exec("dd if=" + _chosenRecovery.getAbsolutePath() + " of=" + _blockPath + _recovery + " 2>> lunardebug.log");
			copyFile(_chosenRecovery, new File(_blockPath + _recovery));
			//display success message
			showMessageWithNoActions("Success", "No errors encountered when flashing new recovery!");
		} catch (Exception e){
			showMessageWithNoActions("Error", "Error when attempting to flash recovery!\n\nMessage: " + e.getMessage());
		
		}
	}
	
	private void menu3_UpdateKernel(){
		Toast.makeText(_context, "updating the kernel not implemented", Toast.LENGTH_LONG).show();
	}

	private void menu4_FlashBootFromZipAutoReboot(){
		Toast.makeText(_context, "flashing boot from zip not implemented", Toast.LENGTH_LONG).show();
	}

	private void showMessageWithNoActions(String dlgTitle, String dlgMessage){
		//Simply tells user a message, with no further action to be taken
		new AlertDialog.Builder(this)
	    .setTitle(dlgTitle)
	    .setMessage(dlgMessage)
	    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            // nothing to do
	        }
	     })
	     .show();
	}
	
	private boolean isLunarToolsRunningAsSystemApp(){
		//Boolean holding false, until we run into com.webkozzer.lunartools in system package list
		boolean lunarIsSystemApp = false;
		
		//Get the system package list
		PackageManager pm = _context.getPackageManager();
		List<PackageInfo> list =pm.getInstalledPackages(0);

		//Loop through the system package list
		for(PackageInfo pi : list) {
			 try {
			 	ApplicationInfo ai = pm.getApplicationInfo(pi.packageName, 0);
			 	if((ai.flags & ApplicationInfo.FLAG_SYSTEM)!=0){
			 		if (ai.name.equals("com.webkozzer.lunartools") == true){
			 			lunarIsSystemApp = true;
			 		}
			 	}
			 } catch (Exception e){
				 
			 }
		}
		
		return lunarIsSystemApp;
	}

	private boolean copyFile(File orig, File dest){
		try {
			FileInputStream in = new FileInputStream(orig.getAbsolutePath());
			FileOutputStream out = new FileOutputStream(dest.getAbsolutePath());
			byte[] buffer = new byte[1024];
			int length;
			while((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}
			in.close();
			out.close();
			return true;
		} catch (Exception e){
			showMessageWithNoActions("File Copy Error", "Error while trying to copy " + orig.getName() + " to " + dest.getName() + "!\n\nMessage: " + e.getMessage());
			return false;
		}
	}
}
