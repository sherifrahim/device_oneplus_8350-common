/*
 * Copyright (C) 2016 The OmniROM Project
 * Copyright (C) 2022 The Nameless-AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nameless.device.OnePlusSettings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.telephony.SubscriptionManager;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import java.util.Arrays;

import com.qualcomm.qcrilmsgtunnel.IQcrilMsgTunnel;

import org.nameless.device.OnePlusSettings.Constants; 
import org.nameless.device.OnePlusSettings.Doze.DozeSettingsActivity;
import org.nameless.device.OnePlusSettings.Preferences.CustomSeekBarPreference;
import org.nameless.device.OnePlusSettings.Preferences.SwitchPreference;
import org.nameless.device.OnePlusSettings.Preferences.VibratorStrengthPreference;
import org.nameless.device.OnePlusSettings.Utils.FileUtils;
import org.nameless.device.OnePlusSettings.Utils.FpsUtils;
import org.nameless.device.OnePlusSettings.Utils.HBMUtils;
import org.nameless.device.OnePlusSettings.Utils.Protocol;
import org.nameless.device.OnePlusSettings.Utils.SwitchUtils;
import org.nameless.device.OnePlusSettings.Utils.VibrationUtils;
import org.nameless.device.OnePlusSettings.Utils.VolumeUtils;

public class MainSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "KeyHanlderMainSettings";
    
    public static final String KEY_MUTE_MEDIA = "mute_media";
    public static final String KEY_DC_SWITCH = "dc_dim";
    public static final String KEY_AUTO_HBM_SWITCH = "auto_hbm";
    public static final String KEY_AUTO_HBM_THRESHOLD = "auto_hbm_threshold";
    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_FPS_INFO = "fps_info";
    public static final String KEY_VIBSTRENGTH = "vib_strength";

    private static final String KEY_PREF_DOZE = "advanced_doze_settings";
    private static final String KEY_FPS_INFO_POSITION = "fps_info_position";
    private static final String KEY_FPS_INFO_COLOR = "fps_info_color";
    private static final String KEY_FPS_INFO_TEXT_SIZE = "fps_info_text_size";
    private static final String KEY_NR_MODE_SWITCHER = "nr_mode_switcher";

    private ListPreference mFpsInfoColor;
    private ListPreference mFpsInfoPosition;
    private ListPreference mNrModeSwitcher;

    private ListPreference mTopKeyPref;
    private ListPreference mMiddleKeyPref;
    private ListPreference mBottomKeyPref;

    private Preference mDozeSettings;
    private SwitchPreference mMuteMedia;
    private SwitchPreference mDCModeSwitch;
    private SwitchPreference mAutoHBMSwitch;
    private SwitchPreference mHBMModeSwitch;
    private SwitchPreference mFpsInfo;
    private CustomSeekBarPreference mFpsInfoTextSizePreference;
    private VibratorStrengthPreference mVibratorStrengthPreference;

    private ModeSwitch DCModeSwitch;
    private ModeSwitch HBMModeSwitch;

    private Protocol mProtocol;
    private Runnable mUnbindService;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Context context = getContext();
        addPreferencesFromResource(R.xml.main);

        Intent intent = new Intent();
        intent.setClassName("com.qualcomm.qcrilmsgtunnel", "com.qualcomm.qcrilmsgtunnel.QcrilMsgTunnelService");
        context.bindServiceAsUser(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                IQcrilMsgTunnel tunnel = IQcrilMsgTunnel.Stub.asInterface(service);
                if (tunnel != null)
                    mProtocol = new Protocol(tunnel);

                ServiceConnection serviceConnection = this;

                mUnbindService = () -> context.unbindService(serviceConnection);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mProtocol = null;
            }
        }, context.BIND_AUTO_CREATE, UserHandle.CURRENT);

        mMuteMedia = (SwitchPreference) findPreference(KEY_MUTE_MEDIA);
        mMuteMedia.setChecked(VolumeUtils.isCurrentlyEnabled(context));
        mMuteMedia.setOnPreferenceChangeListener(this);

        mDCModeSwitch = (SwitchPreference) findPreference(KEY_DC_SWITCH);
        DCModeSwitch = SwitchUtils.getDCModeSwitch(context, mDCModeSwitch);
        if (DCModeSwitch.isSupported()) {
            mDCModeSwitch.setEnabled(true);
        } else {
            mDCModeSwitch.setEnabled(false);
            mDCModeSwitch.setSummary(getString(R.string.unsupported_feature));
        }
        mDCModeSwitch.setChecked(DCModeSwitch.isCurrentlyEnabled());
        mDCModeSwitch.setOnPreferenceChangeListener(DCModeSwitch);

        mHBMModeSwitch = (SwitchPreference) findPreference(KEY_HBM_SWITCH);
        HBMModeSwitch = SwitchUtils.getHBMModeSwitch(context, mHBMModeSwitch);
        if (HBMModeSwitch.isSupported()) {
            mHBMModeSwitch.setEnabled(true);
        } else {
            mHBMModeSwitch.setEnabled(false);
            mHBMModeSwitch.setSummary(getString(R.string.unsupported_feature));
        }
        mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled());
        mHBMModeSwitch.setOnPreferenceChangeListener(HBMModeSwitch);

        mAutoHBMSwitch = (SwitchPreference) findPreference(KEY_AUTO_HBM_SWITCH);
        if (mHBMModeSwitch.isEnabled()) {
            mAutoHBMSwitch.setEnabled(true);
        } else {
            mAutoHBMSwitch.setEnabled(false);
            mAutoHBMSwitch.setSummary(getString(R.string.unsupported_feature));
        }
        mAutoHBMSwitch.setChecked(HBMUtils.isAutoHBMEnabled(context));
        mAutoHBMSwitch.setOnPreferenceChangeListener(this);

        mDozeSettings = (Preference) findPreference(KEY_PREF_DOZE);
        mDozeSettings.setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(getActivity().getApplicationContext(), DozeSettingsActivity.class);
            startActivity(i);
            return true;
        });

        mFpsInfo = (SwitchPreference) findPreference(KEY_FPS_INFO);
        mFpsInfo.setChecked(FpsUtils.isFPSOverlayRunning(context));
        mFpsInfo.setOnPreferenceChangeListener(this);

        mFpsInfoPosition = (ListPreference) findPreference(KEY_FPS_INFO_POSITION);
        mFpsInfoPosition.setOnPreferenceChangeListener(this);

        mFpsInfoColor = (ListPreference) findPreference(KEY_FPS_INFO_COLOR);
        mFpsInfoColor.setOnPreferenceChangeListener(this);

        mFpsInfoTextSizePreference = (CustomSeekBarPreference) findPreference(KEY_FPS_INFO_TEXT_SIZE);
        mFpsInfoTextSizePreference.setOnPreferenceChangeListener(this);

        mNrModeSwitcher = (ListPreference) findPreference(KEY_NR_MODE_SWITCHER);
        mNrModeSwitcher.setOnPreferenceChangeListener(this);

        mVibratorStrengthPreference =  (VibratorStrengthPreference) findPreference(KEY_VIBSTRENGTH);
        if (FileUtils.isFileWritable(VibrationUtils.FILE_LEVEL)) {
            mVibratorStrengthPreference.setValue(PreferenceManager.getDefaultSharedPreferences(context).
                    getInt(KEY_VIBSTRENGTH, VibrationUtils.getVibStrength()));
            mVibratorStrengthPreference.setOnPreferenceChangeListener(this);
        } else {
            mVibratorStrengthPreference.setEnabled(false);
            mVibratorStrengthPreference.setSummary(getString(R.string.unsupported_feature));
        }

        initNotificationSliderPreference();
    }

    private void initNotificationSliderPreference() {
        registerPreferenceListener(Constants.NOTIF_SLIDER_USAGE_KEY);
        registerPreferenceListener(Constants.NOTIF_SLIDER_ACTION_TOP_KEY);
        registerPreferenceListener(Constants.NOTIF_SLIDER_ACTION_MIDDLE_KEY);
        registerPreferenceListener(Constants.NOTIF_SLIDER_ACTION_BOTTOM_KEY);

        ListPreference usagePref = (ListPreference) findPreference(
                Constants.NOTIF_SLIDER_USAGE_KEY);
        handleSliderUsageChange(usagePref.getValue());
    }

    private void registerPreferenceListener(String key) {
        Preference p = findPreference(key);
        p.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUnbindService != null) {
            mUnbindService.run();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled());
        mFpsInfo.setChecked(FpsUtils.isFPSOverlayRunning(getContext()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getContext();
        if (preference == mMuteMedia) {
            Boolean enabled = (Boolean) newValue;
            VolumeUtils.setEnabled(context, enabled);
        } else if (preference == mAutoHBMSwitch) {
            Boolean enabled = (Boolean) newValue;
            HBMUtils.setAutoHBMEnabled(context, enabled);
            HBMUtils.enableService(context);
        } else if (preference == mFpsInfo) {
            boolean enabled = (Boolean) newValue;
            FpsUtils.setFpsService(context, enabled);
        } else if (preference == mFpsInfoPosition) {
            int position = Integer.parseInt(newValue.toString());
            if (FpsUtils.isPositionChanged(context, position)) {
                FpsUtils.setPosition(context, position);
                FpsUtils.notifySettingsUpdated(context);
            }
        } else if (preference == mFpsInfoColor) {
            int color = Integer.parseInt(newValue.toString());
            if (FpsUtils.isColorChanged(context, color)) {
                FpsUtils.setColorIndex(context, color);
                FpsUtils.notifySettingsUpdated(context);
            }
        } else if (preference == mFpsInfoTextSizePreference) {
            int size = Integer.parseInt(newValue.toString());
            if (FpsUtils.isSizeChanged(context, size - 1)) {
                FpsUtils.setSizeIndex(context, size - 1);
                FpsUtils.notifySettingsUpdated(context);
            }
        } else if (preference == mNrModeSwitcher) {
            int mode = Integer.parseInt(newValue.toString());
            return setNrModeChecked(mode);
        } else if (preference == mVibratorStrengthPreference) {
            int value = Integer.parseInt(newValue.toString());
            PreferenceManager.getDefaultSharedPreferences(context).edit().
                    putInt(KEY_VIBSTRENGTH, value).commit();
            VibrationUtils.setVibStrength(context, value);
            VibrationUtils.doHapticFeedback(context, VibrationEffect.EFFECT_CLICK, true);
            return true;
        }

        String key = preference.getKey();
        switch (key) {
            case Constants.NOTIF_SLIDER_USAGE_KEY:
                return handleSliderUsageChange((String) newValue) &&
                        handleSliderUsageDefaultsChange((String) newValue) &&
                        notifySliderUsageChange((String) newValue);
            case Constants.NOTIF_SLIDER_ACTION_TOP_KEY:
                return notifySliderActionChange(0, (String) newValue);
            case Constants.NOTIF_SLIDER_ACTION_MIDDLE_KEY:
                return notifySliderActionChange(1, (String) newValue);
            case Constants.NOTIF_SLIDER_ACTION_BOTTOM_KEY:
                return notifySliderActionChange(2, (String) newValue);
            default:
                break;
        }

        String node = Constants.sBooleanNodePreferenceMap.get(key);
        if (!TextUtils.isEmpty(node) && FileUtils.isFileWritable(node)) {
            Boolean value = (Boolean) newValue;
            FileUtils.writeLine(node, value ? "1" : "0");
            return true;
        }
        node = Constants.sStringNodePreferenceMap.get(key);
        if (!TextUtils.isEmpty(node) && FileUtils.isFileWritable(node)) {
            FileUtils.writeLine(node, (String) newValue);
            return true;
        }

        return false;
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        // Initialize node preferences
        for (String pref : Constants.sBooleanNodePreferenceMap.keySet()) {
            SwitchPreference b = (SwitchPreference) findPreference(pref);
            if (b == null) continue;
            String node = Constants.sBooleanNodePreferenceMap.get(pref);
            if (FileUtils.isFileReadable(node)) {
                String curNodeValue = FileUtils.readOneLine(node);
                b.setChecked(curNodeValue.equals("1"));
                b.setOnPreferenceChangeListener(this);
            } else {
                removePref(b);
            }
        }
        for (String pref : Constants.sStringNodePreferenceMap.keySet()) {
            ListPreference l = (ListPreference) findPreference(pref);
            if (l == null) continue;
            String node = Constants.sStringNodePreferenceMap.get(pref);
            if (FileUtils.isFileReadable(node)) {
                l.setValue(FileUtils.readOneLine(node));
                l.setOnPreferenceChangeListener(this);
            } else {
                removePref(l);
            }
        }
    }

    private void removePref(Preference pref) {
        PreferenceGroup parent = pref.getParent();
        if (parent == null) {
            return;
        }
        parent.removePreference(pref);
        if (parent.getPreferenceCount() == 0) {
            removePref(parent);
        }
    }

    private boolean handleSliderUsageChange(String newValue) {
        if(newValue == null){
            Log.d(TAG, "newValue is null");
            return false;
        }
        switch (newValue) {
            case Constants.NOTIF_SLIDER_FOR_NOTIFICATION:
                return updateSliderActions(
                        R.array.notification_slider_mode_entries,
                        R.array.notification_slider_mode_entry_values);
            case Constants.NOTIF_SLIDER_FOR_FLASHLIGHT:
                return updateSliderActions(
                        R.array.notification_slider_flashlight_entries,
                        R.array.notification_slider_flashlight_entry_values);
            case Constants.NOTIF_SLIDER_FOR_BRIGHTNESS:
                return updateSliderActions(
                        R.array.notification_slider_brightness_entries,
                        R.array.notification_slider_brightness_entry_values);
            case Constants.NOTIF_SLIDER_FOR_ROTATION:
                return updateSliderActions(
                        R.array.notification_slider_rotation_entries,
                        R.array.notification_slider_rotation_entry_values);
            case Constants.NOTIF_SLIDER_FOR_RINGER:
                return updateSliderActions(
                        R.array.notification_slider_ringer_entries,
                        R.array.notification_slider_ringer_entry_values);
            case Constants.NOTIF_SLIDER_FOR_NOTIFICATION_RINGER:
                return updateSliderActions(
                        R.array.notification_ringer_slider_mode_entries,
                        R.array.notification_ringer_slider_mode_entry_values);
            default:
                return false;
        }
    }

    private boolean handleSliderUsageDefaultsChange(String newValue) {
        int defaultsResId = getDefaultResIdForUsage(newValue);
        if (defaultsResId == 0) {
            return false;
        }
        return updateSliderActionDefaults(defaultsResId);
    }

    private boolean updateSliderActions(int entriesResId, int entryValuesResId) {
        String[] entries = getResources().getStringArray(entriesResId);
        String[] entryValues = getResources().getStringArray(entryValuesResId);
        return updateSliderPreference(Constants.NOTIF_SLIDER_ACTION_TOP_KEY,
                entries, entryValues) &&
            updateSliderPreference(Constants.NOTIF_SLIDER_ACTION_MIDDLE_KEY,
                    entries, entryValues) &&
            updateSliderPreference(Constants.NOTIF_SLIDER_ACTION_BOTTOM_KEY,
                    entries, entryValues);
    }

    private boolean updateSliderActionDefaults(int defaultsResId) {
        String[] defaults = getResources().getStringArray(defaultsResId);
        if (defaults.length != 3) {
            return false;
        }

        return updateSliderPreferenceValue(Constants.NOTIF_SLIDER_ACTION_TOP_KEY,
                defaults[0]) &&
            updateSliderPreferenceValue(Constants.NOTIF_SLIDER_ACTION_MIDDLE_KEY,
                    defaults[1]) &&
            updateSliderPreferenceValue(Constants.NOTIF_SLIDER_ACTION_BOTTOM_KEY,
                    defaults[2]);
    }

    private boolean updateSliderPreference(CharSequence key,
            String[] entries, String[] entryValues) {
        ListPreference pref = (ListPreference) findPreference(key);
        if (pref == null) {
            return false;
        }
        pref.setEntries(entries);
        pref.setEntryValues(entryValues);
        return true;
    }

    private boolean updateSliderPreferenceValue(CharSequence key,
            String value) {
        ListPreference pref = (ListPreference) findPreference(key);
        if (pref == null) {
            return false;
        }
        pref.setValue(value);
        return true;
    }

    private int[] getCurrentSliderActions() {
        int[] actions = new int[3];
        ListPreference p;

        p = (ListPreference) findPreference(
                Constants.NOTIF_SLIDER_ACTION_TOP_KEY);
        actions[0] = Integer.parseInt(p.getValue());

        p = (ListPreference) findPreference(
                Constants.NOTIF_SLIDER_ACTION_MIDDLE_KEY);
        actions[1] = Integer.parseInt(p.getValue());

        p = (ListPreference) findPreference(
                Constants.NOTIF_SLIDER_ACTION_BOTTOM_KEY);
        actions[2] = Integer.parseInt(p.getValue());

        return actions;
    }

    private boolean notifySliderUsageChange(String usage) {
        sendUpdateBroadcast(getActivity().getApplicationContext(), Integer.parseInt(usage),
                getCurrentSliderActions());
        return true;
    }

    private boolean notifySliderActionChange(int index, String value) {
        ListPreference p = (ListPreference) findPreference(
                Constants.NOTIF_SLIDER_USAGE_KEY);
        int usage = Integer.parseInt(p.getValue());

        int[] actions = getCurrentSliderActions();
        actions[index] = Integer.parseInt(value);

        sendUpdateBroadcast(getActivity().getApplicationContext(), usage, actions);
        return true;
    }

    public static void sendUpdateBroadcast(Context context,
            int usage, int[] actions) {
        Intent intent = new Intent(Constants.ACTION_UPDATE_SLIDER_SETTINGS);
        intent.putExtra(Constants.EXTRA_SLIDER_USAGE, usage);
        intent.putExtra(Constants.EXTRA_SLIDER_ACTIONS, actions);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        Log.d(TAG, "update slider usage " + usage + " with actions: " +
                Arrays.toString(actions));
    }

    public static void restoreSliderStates(Context context) {
        Resources res = context.getResources();
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);

        String usage = prefs.getString(Constants.NOTIF_SLIDER_USAGE_KEY,
                res.getString(R.string.config_defaultNotificationSliderUsage));

        int defaultsResId = getDefaultResIdForUsage(usage);
        if (defaultsResId == 0) {
            return;
        }

        String[] defaults = res.getStringArray(defaultsResId);
        if (defaults.length != 3) {
            return;
        }

        String actionTop = prefs.getString(
                Constants.NOTIF_SLIDER_ACTION_TOP_KEY, defaults[0]);

        String actionMiddle = prefs.getString(
                Constants.NOTIF_SLIDER_ACTION_MIDDLE_KEY, defaults[1]);

        String actionBottom = prefs.getString(
                Constants.NOTIF_SLIDER_ACTION_BOTTOM_KEY, defaults[2]);

        prefs.edit()
            .putString(Constants.NOTIF_SLIDER_USAGE_KEY, usage)
            .putString(Constants.NOTIF_SLIDER_ACTION_TOP_KEY, actionTop)
            .putString(Constants.NOTIF_SLIDER_ACTION_MIDDLE_KEY, actionMiddle)
            .putString(Constants.NOTIF_SLIDER_ACTION_BOTTOM_KEY, actionBottom)
            .commit();

        sendUpdateBroadcast(context, Integer.parseInt(usage), new int[] {
            Integer.parseInt(actionTop),
            Integer.parseInt(actionMiddle),
            Integer.parseInt(actionBottom)
        });
    }

    private boolean setNrModeChecked(int mode) {
        if (mode == 0) {
            return setNrModeChecked(Protocol.NR_5G_DISABLE_MODE_TYPE.NAS_NR5G_DISABLE_MODE_SA);
        } else if (mode == 1) {
            return setNrModeChecked(Protocol.NR_5G_DISABLE_MODE_TYPE.NAS_NR5G_DISABLE_MODE_NSA);
        } else {
            return setNrModeChecked(Protocol.NR_5G_DISABLE_MODE_TYPE.NAS_NR5G_DISABLE_MODE_NONE);
        }
    }

    private boolean setNrModeChecked(Protocol.NR_5G_DISABLE_MODE_TYPE mode) {
        if (mProtocol == null) {
            Toast.makeText(getContext(), R.string.service_not_ready, Toast.LENGTH_LONG).show();
            return false;
        }
        int index = SubscriptionManager.getSlotIndex(SubscriptionManager.getDefaultDataSubscriptionId());
        if (index == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            Toast.makeText(getContext(), R.string.unavailable_sim_slot, Toast.LENGTH_LONG).show();
            return false;
        }
        new Thread(() -> mProtocol.setNrMode(index, mode)).start();
        return true;
    }

    private static int getDefaultResIdForUsage(String usage) {
        switch (usage) {
            case Constants.NOTIF_SLIDER_FOR_NOTIFICATION:
                return R.array.config_defaultSliderActionsForNotification;
            case Constants.NOTIF_SLIDER_FOR_FLASHLIGHT:
                return R.array.config_defaultSliderActionsForFlashlight;
            case Constants.NOTIF_SLIDER_FOR_BRIGHTNESS:
                return R.array.config_defaultSliderActionsForBrightness;
            case Constants.NOTIF_SLIDER_FOR_ROTATION:
                return R.array.config_defaultSliderActionsForRotation;
            case Constants.NOTIF_SLIDER_FOR_RINGER:
                return R.array.config_defaultSliderActionsForRinger;
            case Constants.NOTIF_SLIDER_FOR_NOTIFICATION_RINGER:
                return R.array.config_defaultSliderActionsForNotificationRinger;
            default:
                return 0;
        }
    }
}
