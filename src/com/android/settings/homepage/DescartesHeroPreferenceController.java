/*
 * SPDX-FileCopyrightText: DescartesOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.homepage;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

public class DescartesHeroPreferenceController extends BasePreferenceController {

    public DescartesHeroPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            bind(preference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof LayoutPreference) {
            bind((LayoutPreference) preference);
        }
    }

    private void bind(LayoutPreference preference) {
        String name = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.DEVICE_NAME);
        if (TextUtils.isEmpty(name)) {
            name = Build.MODEL;
        }
        ((TextView) preference.findViewById(R.id.descartes_hero_title)).setText(name);

        String version = SystemProperties.get("ro.descartes.version");
        String os = mContext.getString(R.string.descartes_about_os_name);
        if (!TextUtils.isEmpty(version)) {
            os += " v" + version;
        }
        ((TextView) preference.findViewById(R.id.descartes_hero_summary)).setText(
                mContext.getString(R.string.descartes_homepage_hero_summary, os,
                        Build.VERSION.RELEASE));

        preference.findViewById(R.id.descartes_hero_card).setOnClickListener(v ->
                v.getContext().startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                        .setPackage(v.getContext().getPackageName())));
    }
}
