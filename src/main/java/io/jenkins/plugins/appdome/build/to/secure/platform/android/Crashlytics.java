package io.jenkins.plugins.appdome.build.to.secure.platform.android;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

public class Crashlytics implements Describable<Crashlytics> {

    private final String firebaseAppId;

    @DataBoundConstructor
    public Crashlytics(String firebaseAppId) {
        this.firebaseAppId = firebaseAppId;
    }

    @Exported
    public String getFirebaseAppId() {
        return firebaseAppId;
    }

    @Override
    public Descriptor<Crashlytics> getDescriptor() {
        return Jenkins.get().getDescriptor(Crashlytics.class);
    }

    @Symbol("Crashlytics")  // ✅ Register as DSL symbol
    @Extension
    public static class DescriptorImpl extends Descriptor<Crashlytics> {
        @Override
        public String getDisplayName() {
            return "Crashlytics Configuration";
        }
    }

}
