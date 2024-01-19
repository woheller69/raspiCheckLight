/**
 * MIT License
 *
 * Copyright (c) 2019  RasPi Check Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.eidottermihi.rpicheck.activity;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;


import de.eidottermihi.raspicheck.BuildConfig;
import de.eidottermihi.raspicheck.R;
import io.freefair.android.preference.AppCompatPreferenceActivity;

/**
 * Settings activity. Settings items are inflated from xml.
 *
 * @author Michael
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements
        OnSharedPreferenceChangeListener {

    /**
     * Preference keys.
     */
    public static final String KEY_PREF_TEMPERATURE_SCALE = "pref_temperature_scala";
    public static final String KEY_PREF_QUERY_HIDE_ROOT_PROCESSES = "pref_query_hide_root";
    public static final String KEY_PREF_FREQUENCY_UNIT = "pref_frequency_unit";
    public static final String KEY_PREF_QUERY_SHOW_SYSTEM_TIME = "pref_query_show_system_time";

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // init summary texts to reflect users choice
        this.initSummaries();
    }

    private void initSummaries() {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        initSummary(prefs, KEY_PREF_TEMPERATURE_SCALE);
        initSummary(prefs, KEY_PREF_FREQUENCY_UNIT);
    }

    @SuppressWarnings("deprecation")
    private void initSummary(SharedPreferences prefs, String prefKey) {
        final Preference pref = findPreference(prefKey);
        final String prefValue = prefs.getString(prefKey, null);
        if (prefValue != null) {
            pref.setSummary(prefValue);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(KEY_PREF_FREQUENCY_UNIT)) {
            initSummary(sharedPreferences, KEY_PREF_FREQUENCY_UNIT);
        }
        if (key.equals(KEY_PREF_TEMPERATURE_SCALE)) {
            initSummary(sharedPreferences, KEY_PREF_TEMPERATURE_SCALE);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }


}
