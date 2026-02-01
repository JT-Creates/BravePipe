package org.schabi.newpipe;

import android.content.Context;
import android.content.SharedPreferences;


//import org.schabi.newpipe.extractor.services.bilibili.settings.BiliBiliSettings;

import org.schabi.newpipe.extractor.services.rumble.settings.RumbleSettings;
import org.schabi.newpipe.extractor.services.youtube.settings.YoutubeSettings;

import java.util.Objects;

import androidx.preference.PreferenceManager;
/***
    TODO: Implement BiliBili settings.
 ***/
/**
 * BravePipeExtractor Settings that will be pushed into the extractor library.
 */
public class BraveExtractorSettings {
    private final Context context;

    public BraveExtractorSettings(final Context context) {
        this.context = context;
    }

    public void initExtractorConfig() {
        initExtractorPremiumSetting(PreferenceManager.getDefaultSharedPreferences(context));
    }

    private void initExtractorPremiumSetting(final SharedPreferences sharedCfg) {
        final boolean doHidePaidContent = Objects.requireNonNull(sharedCfg)
                .getBoolean(context.getResources().getString(
                        R.string.brave_settings_hide_paid_content_key), false);

        YoutubeSettings.getInstance()
                .setting(doHidePaidContent, YoutubeSettings.HIDE_MEMBERS_ONLY_STREAMS);
        RumbleSettings.getInstance()
                .setting(doHidePaidContent, RumbleSettings.HIDE_PREMIUM_STREAMS);
    }
}
