package net.osmtracker.activity;

import java.io.File;
import java.io.FilenameFilter;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;


/**
 * Manages preferences screen.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class Preferences extends PreferenceActivity {

	@SuppressWarnings("unused")
	private static final String TAG = Preferences.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// Set summary of some preferences to their actual values
		// and register a change listener to set again the summary in case of change
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// External storage directory
		EditTextPreference storageDirPref = (EditTextPreference) findPreference(OSMTracker.Preferences.KEY_STORAGE_DIR);
		storageDirPref.setSummary(prefs.getString(OSMTracker.Preferences.KEY_STORAGE_DIR, OSMTracker.Preferences.VAL_STORAGE_DIR));
		storageDirPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Ensure there is always a leading slash
				if (! ((String) newValue).startsWith(File.separator)) {
					newValue = File.separator + (String) newValue;
				}

				// Set summary with the directory value
				preference.setSummary((String) newValue);

				return true;
			}
		});

		// Update GPS logging interval summary to the current value
		Preference pref = findPreference(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL);
		pref.setSummary(
				prefs.getString(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)
						+ " " + getResources().getString(R.string.prefs_gps_logging_interval_seconds)
						+ ". " + getResources().getString(R.string.prefs_gps_logging_interval_summary));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Set summary with the interval and "seconds"
				preference.setSummary(newValue
						+ " " + getResources().getString(R.string.prefs_gps_logging_interval_seconds)
						+ ". " + getResources().getString(R.string.prefs_gps_logging_interval_summary));
				return true;
			}
		});

		pref = findPreference(OSMTracker.Preferences.KEY_GPS_OSSETTINGS);
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				return true;
			}
		});

		// Button screen orientation option
		pref = findPreference(OSMTracker.Preferences.KEY_UI_ORIENTATION);
		ListPreference orientationListPreference = (ListPreference) pref;
		String displayValueKey = prefs.getString(OSMTracker.Preferences.KEY_UI_ORIENTATION, OSMTracker.Preferences.VAL_UI_ORIENTATION);
		int displayValueIndex = orientationListPreference.findIndexOfValue(displayValueKey);
		String displayValue = orientationListPreference.getEntries()[displayValueIndex].toString();
		orientationListPreference.setSummary(displayValue + ".\n" 
				+ getResources().getString(R.string.prefs_ui_orientation_summary));

		// Set a listener to update the preference display after a change is made
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Set summary with the display text of the item and a description of the preference
				ListPreference orientationListPreference = (ListPreference)preference;
				// Pull the display string from the list preference rather than simply using the key value
				int newValueIndex = orientationListPreference.findIndexOfValue((String)newValue);
				String newPreferenceDisplayValue = orientationListPreference.getEntries()[newValueIndex].toString();

				preference.setSummary(newPreferenceDisplayValue
						+ ".\n" + getResources().getString(R.string.prefs_ui_orientation_summary));
				return true;
			}
		});

	}
	
}
