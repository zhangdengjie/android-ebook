package com.ebook.common.util;

import android.content.Context;

import com.blankj.utilcode.util.ThreadUtils;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public enum MixpanelUtil {

    INSTANCE;

    public void openHomePage(Context context) throws JSONException, IOException {
        ThreadUtils.executeByCached(new ThreadUtils.SimpleTask<Void>() {
            @Override
            public Void doInBackground() throws Throwable {
                // Set up an instance of MixpanelAPI
                MixpanelAPI mp =
                        MixpanelAPI.getInstance(context, "c9378eb7b429a0938cfce7e4dd2173f5", true);
                mp.setEnableLogging(true);
                // The second param is a flag for allowing profile updates
                mp.identify("demo_123", true);

                // Identify must be called before properties are set
                mp.getPeople().set("$name", "xixi");
                mp.getPeople().set("$email", "xixi_gmail@example.com");
                mp.getPeople().set("plan", "Premium");

                mp.track("demo");

                JSONObject props = new JSONObject();
                props.put("Signup Type", "Referral");
                mp.track("Signed Up", props);
                return null;
            }

            @Override
            public void onSuccess(Void result) {

            }
        });

    }
}
