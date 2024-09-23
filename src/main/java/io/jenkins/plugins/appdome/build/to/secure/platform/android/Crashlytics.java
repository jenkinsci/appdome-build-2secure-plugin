package io.jenkins.plugins.appdome.build.to.secure.platform.android;

import org.kohsuke.stapler.DataBoundConstructor;

public class Crashlytics {
    private final String googleCredFile;
    private final String firebaseAppId;

    @DataBoundConstructor
    public Crashlytics(String googleCredFile, String firebaseAppId) {
        this.googleCredFile = googleCredFile;
        this.firebaseAppId = firebaseAppId;
    }

    public String getGoogleCredFile() {
        return googleCredFile;
    }

    public String getFirebaseAppId() {
        return firebaseAppId;
    }

}
