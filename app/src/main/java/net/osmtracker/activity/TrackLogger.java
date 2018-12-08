package net.osmtracker.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import android.widget.*;
import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.layout.GpsStatusRecord;
import net.osmtracker.listener.SensorListener;
import net.osmtracker.service.gps.GPSLogger;
import net.osmtracker.service.gps.GPSLoggerServiceConnection;
import net.osmtracker.util.ThemeValidator;

import java.io.File;
import java.util.Date;


/**
 * Main track logger activity. Communicate with the GPS service to display GPS
 * status, and allow user to record waypoints.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class TrackLogger extends Activity {

	private static final String TAG = TrackLogger.class.getSimpleName();

	/**
	 * Bundle state key for tracking flag.
	 */
	public static final String STATE_IS_TRACKING = "isTracking";
	
	/**
	 * Bundle state key button state.
	 */
	public static final String STATE_BUTTONS_ENABLED = "buttonsEnabled";

	/**
	 * GPS Logger service, to receive events and be able to update UI.
	 */
	private GPSLogger gpsLogger;

	/**
	 * GPS Logger service intent, to be used in start/stopService();
	 */
	private Intent gpsLoggerServiceIntent;

	/**
	 * Flag to check GPS status at startup. Is cleared after the first
	 * displaying of GPS status dialog, to prevent the dialog to display if user
	 * goes to settings/about/other screen.
	 */
	private boolean checkGPSFlag = true;
	
	/**
	 * Keeps track of the image file when taking a picture.
	 */
	private File currentImageFile;
	
	/**
	 * Keeps track of the current track id.
	 */
	private long currentTrackId;

	/**
	 * Handles the bind to the GPS Logger service
	 */
	private ServiceConnection gpsLoggerConnection = new GPSLoggerServiceConnection(this);
	
	/**
	 * Keeps the SharedPreferences
	 */
	private SharedPreferences prefs = null;
	
	/**
	 * keeps track of current button status
	 */
	private boolean buttonsEnabled = false;
	
	/**
	 * sensor listener for the azimuth display
	 */
	private SensorListener sensorListener;

	private boolean startedByShortcut;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Get the track id to work with
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			startedByShortcut = false;
			// Track was created/provided by the TrackManager
			currentTrackId = extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID);
		} else {
			startedByShortcut = true;
			// Coming from outside, but maybe a track was already being recorded
			currentTrackId = DataHelper.getActiveTrackId(getContentResolver());
			if (currentTrackId == -1) {
				// Create the track ourselves and start tracking
				currentTrackId = TrackManager.createNewTrack(getContentResolver());
			}
		}
		Log.v(TAG, "Starting for track id " + currentTrackId);

		gpsLoggerServiceIntent = new Intent(this, GPSLogger.class);
		gpsLoggerServiceIntent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);

		// Populate default preference values
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// get shared preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Set application theme according to user settings
		setTheme(getResources().getIdentifier(ThemeValidator.getValidTheme(prefs, getResources()), null, null));

		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.tracklogger);
		
		// set trackLogger to keepScreenOn depending on the user's preference
		View trackLoggerView = findViewById(R.id.tracklogger_root);
		trackLoggerView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));

		// we'll restore previous button state, GPSStatusRecord will enable all buttons, as soon as there's a gps fix
		if(savedInstanceState != null){
			buttonsEnabled = savedInstanceState.getBoolean(STATE_BUTTONS_ENABLED, false);
		}
		
		// create sensor listener
		sensorListener = new SensorListener();

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onResume() {

		setTitle(getResources().getString(R.string.tracklogger) + ": #" + currentTrackId);
		
		// set trackLogger to  keepScreenOn depending on the user's preference
		View trackLoggerView = findViewById(R.id.tracklogger_root);
		trackLoggerView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
	
		// Fix to the user's preferred orientation (if any)  
		String preferredOrientation = prefs.getString(OSMTracker.Preferences.KEY_UI_ORIENTATION, 
				OSMTracker.Preferences.VAL_UI_ORIENTATION);
		if (preferredOrientation.equals(OSMTracker.Preferences.VAL_UI_ORIENTATION_PORTRAIT)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if (preferredOrientation.equals(OSMTracker.Preferences.VAL_UI_ORIENTATION_LANDSCAPE)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}

		View mainLayout = new LinearLayout(this) {{
			setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT, 1));
			TableLayout tblLayout = new TableLayout(getContext());
			tblLayout.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.FILL_PARENT,
					LinearLayout.LayoutParams.FILL_PARENT,
					1));
			TableRow tblRow = new TableRow(getContext());
			tblRow.setLayoutParams(new TableLayout.LayoutParams(
					TableLayout.LayoutParams.FILL_PARENT,
					TableLayout.LayoutParams.FILL_PARENT,
					1));
			tblLayout.addView(tblRow);
			Button button = new Button(getContext());
			button.setText("Helloo");
			button.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.FILL_PARENT,
					TableRow.LayoutParams.FILL_PARENT,
					1));
			tblRow.addView(button);
			this.addView(tblLayout);
		}};
		//mainLayout = new UserDefinedLayout(this, currentTrackId, null);
		((ViewGroup) findViewById(R.id.tracklogger_root)).removeAllViews();
		((ViewGroup) findViewById(R.id.tracklogger_root)).addView(mainLayout);
		
		// Check GPS status
		if (checkGPSFlag
				&& prefs.getBoolean(OSMTracker.Preferences.KEY_GPS_CHECKSTARTUP,
						OSMTracker.Preferences.VAL_GPS_CHECKSTARTUP)) {
			checkGPSProvider();
		}

		// Register GPS status update for upper controls
		((GpsStatusRecord) findViewById(R.id.gpsStatus)).requestLocationUpdates(true);

		// Start GPS Logger service
		startService(gpsLoggerServiceIntent);

		// Bind to GPS service.
		// We can't use BIND_AUTO_CREATE here, because when we'll ubound
		// later, we want to keep the service alive in background
		bindService(gpsLoggerServiceIntent, gpsLoggerConnection, 0);
		
		// connect the sensor listener
		sensorListener.register(this);

		setEnabledActionButtons(buttonsEnabled);
		if(!buttonsEnabled){
			Toast.makeText(this, R.string.tracklogger_waiting_gps, Toast.LENGTH_LONG).show();
		}

		super.onResume();
	}

	private void checkGPSProvider() {
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			// GPS isn't enabled. Offer user to go enable it
			new AlertDialog.Builder(this)
					.setTitle(R.string.tracklogger_gps_disabled)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(getResources().getString(R.string.tracklogger_gps_disabled_hint))
					.setCancelable(true).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
						}
					}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					}).create().show();
			checkGPSFlag = false;
		}
	}

	@Override
	protected void onPause() {
		
		// Un-register GPS status update for upper controls
		((GpsStatusRecord) findViewById(R.id.gpsStatus)).requestLocationUpdates(false);

		if (gpsLogger != null) {
			if (!gpsLogger.isTracking()) {
				Log.v(TAG, "Service is not tracking, trying to stopService()");
				unbindService(gpsLoggerConnection);
				stopService(gpsLoggerServiceIntent);
			} else {
				unbindService(gpsLoggerConnection);
			}
		}
		
		if (sensorListener!=null) {
			sensorListener.unregister();
		}

		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Save the fact that we are currently tracking or not
		if(gpsLogger != null){
			outState.putBoolean(STATE_IS_TRACKING, gpsLogger.isTracking());
		}
		outState.putBoolean(STATE_BUTTONS_ENABLED, buttonsEnabled);

		super.onSaveInstanceState(outState);
	}

	/**
	 * Called when GPS is disabled
	 */
	public void onGpsDisabled() {
		// GPS disabled. Grey all.
		setEnabledActionButtons(false);
	}

	/**
	 * Called when GPS is enabled
	 */
	public void onGpsEnabled() {
		// Buttons can be enabled
		if (gpsLogger != null && gpsLogger.isTracking()) {
			setEnabledActionButtons(true);
		}
	}

	/**
	 * Enable buttons associated to tracking
	 * 
	 * @param enabled true to enable, false to disable
	 */
	public void setEnabledActionButtons(boolean enabled) {
	}

	// Create options menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracklogger_menu, menu);
		return true;
	}

	// Manage options menu selections
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.tracklogger_menu_stoptracking:
			// Start / Stop tracking	
			if (gpsLogger.isTracking()) {
				Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
				sendBroadcast(intent);
				((GpsStatusRecord) findViewById(R.id.gpsStatus)).manageRecordingIndicator(false);
				finish();
			}		
			break;
		case R.id.tracklogger_menu_settings:
			// Start settings activity
			startActivity(new Intent(this, Preferences.class));
			break;
		case R.id.tracklogger_menu_about:
			// Start About activity
			startActivity(new Intent(this, About.class));
			break;
		case R.id.tracklogger_menu_displaytrack:
			// Start display track activity, with or without OSM background
			boolean useOpenStreetMapBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					OSMTracker.Preferences.KEY_UI_DISPLAYTRACK_OSM, OSMTracker.Preferences.VAL_UI_DISPLAYTRACK_OSM);
			if (useOpenStreetMapBackground) {
				i = new Intent(this, DisplayTrackMap.class);
			} else {
				i = new Intent(this, DisplayTrack.class);
			}
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
			startActivity(i);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Getter for gpsLogger
	 * 
	 * @return Activity {@link GPSLogger}
	 */
	public GPSLogger getGpsLogger() {
		return gpsLogger;
	}

	/**
	 * Setter for gpsLogger
	 * 
	 * @param l
	 *				{@link GPSLogger} to set.
	 */
	public void setGpsLogger(GPSLogger l) {
		this.gpsLogger = l;
	}
	
	/**
	 * Gets a File for storing an image in the current track dir
	 * and stores it in a class variable.
	 * 
	 * @return A File pointing to an image file inside the current track directory
	 */
	public File pushImageFile() {
		currentImageFile = null;

		// Query for current track directory
		File trackDir = DataHelper.getTrackDirectory(currentTrackId);

		// Create the track storage directory if it does not yet exist
		if (!trackDir.exists()) {
			if ( !trackDir.mkdirs() ) {
				Log.w(TAG, "Directory [" + trackDir.getAbsolutePath() + "] does not exist and cannot be created");
			}
		}

		// Ensure that this location can be written to 
		if (trackDir.exists() && trackDir.canWrite()) {
			currentImageFile = new File(trackDir, 
					DataHelper.FILENAME_FORMATTER.format(new Date()) + DataHelper.EXTENSION_JPG);			
		} else {
			Log.w(TAG, "The directory [" + trackDir.getAbsolutePath() + "] will not allow files to be created");
		}
		
		return currentImageFile;
	}
	
	/**
	 * @return The current image file, and clear the internal variable.
	 */
	public File popImageFile() {
		File imageFile = currentImageFile;
		currentImageFile = null;
		return imageFile;
	}

	@Override
	protected void onNewIntent(Intent newIntent) {
		if (newIntent.getExtras() != null) {
			if (newIntent.getExtras().containsKey(TrackContentProvider.Schema.COL_TRACK_ID)) {
				currentTrackId = newIntent.getExtras().getLong(TrackContentProvider.Schema.COL_TRACK_ID);
				setIntent(newIntent);
			}
		}
		super.onNewIntent(newIntent);
	}

	public long getCurrentTrackId() {
		return this.currentTrackId;
	}
	
	@Override
	public void onBackPressed() {
		if (startedByShortcut) {
			startActivity(new Intent(TrackLogger.this, TrackManager.class));
			finish();
		} else {
			super.onBackPressed();
		}
	}

}
