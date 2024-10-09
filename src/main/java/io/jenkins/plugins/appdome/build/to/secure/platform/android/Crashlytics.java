package io.jenkins.plugins.appdome.build.to.secure.platform.android;

import org.kohsuke.stapler.DataBoundConstructor;

public class Crashlytics {

    private final String firebaseAppId;

    @DataBoundConstructor
    public Crashlytics(String firebaseAppId) {
        this.firebaseAppId = firebaseAppId;
    }


    public String getFirebaseAppId() {
        return firebaseAppId;
    }

}
