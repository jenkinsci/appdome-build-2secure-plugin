package io.jenkins.plugins.appdome.build.to.secure.platform.android;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.appdome.build.to.secure.platform.Platform;
import io.jenkins.plugins.appdome.build.to.secure.platform.PlatformDescriptor;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.CertificateMethod;
import io.jenkins.plugins.appdome.build.to.secure.platform.PlatformType;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import static io.jenkins.plugins.appdome.build.to.secure.AppdomeBuilder.isHttpUrl;

public class AndroidPlatform extends Platform {

    private final CertificateMethod certificateMethod;
    private Crashlytics crashlytics;  // Changed from crashlyticsPublisher to crashlytics

    private Boolean isCrashlytics;

    @DataBoundConstructor
    public AndroidPlatform(CertificateMethod certificateMethod) {
        super(PlatformType.ANDROID);
        this.certificateMethod = certificateMethod;
    }

    public Boolean getIsCrashlytics() {
        if (this.isCrashlytics == null) {
            this.isCrashlytics = false;
        } else {
            this.isCrashlytics = true;
        }
        return this.isCrashlytics;
    }

    public void setIsCrashlytics(Boolean isCrashlytics) {
        this.isCrashlytics = isCrashlytics;
    }

    public String getGoogleCredFile() {
        return this.crashlytics.getGoogleCredFile();
    }

    public String getFirebaseAppId() {
        return this.crashlytics.getFirebaseAppId();
    }


    public String getAppPath() {
        return super.getAppPath();
    }

    @DataBoundSetter
    public void setAppPath(String appPath) {
        super.setAppPath(appPath);
    }

    public String getFusionSetId() {
        return super.getFusionSetId();
    }

    @DataBoundSetter
    public void setFusionSetId(String fusionSetId) {
        super.setFusionSetId(fusionSetId);
    }

    public CertificateMethod getCertificateMethod() {
        return certificateMethod;
    }


    public Crashlytics getCrashlytics() {  // Changed from getCrashlyticsPublisher to getCrashlytics
        return crashlytics;
    }

    @DataBoundSetter
    public void setCrashlytics(Crashlytics crashlytics) {  // Changed from setCrashlyticsPublisher to setCrashlytics
        if (!crashlytics.getFirebaseAppId().isEmpty() && !crashlytics.getGoogleCredFile().isEmpty()) {
            this.isCrashlytics = true;
            this.crashlytics = crashlytics;
        } else {
            this.isCrashlytics = false;
            this.crashlytics = null;
        }
    }

    public DescriptorExtensionList<CertificateMethod, Descriptor<CertificateMethod>> getCertificateMethodDescriptors() {
        return Jenkins.get().getDescriptorList(CertificateMethod.class);
    }

    @Symbol("AndroidPlatform")
    @Extension
    public static final class DescriptorImpl extends PlatformDescriptor {

        @POST
        public FormValidation doCheckAppPath(@QueryParameter String appPath) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (appPath != null && Util.fixEmptyAndTrim(appPath) == null) {
                return FormValidation.warning("Application path was not provided.\n " +
                        "Or please ensure that a valid path is provided for non-protected applications" +
                        " in the environment variable named 'APP_PATH'.");
            } else if (appPath != null && appPath.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in the path.");
            } else if (appPath != null && isHttpUrl(appPath)) {
                return FormValidation.ok("Application remote url provided.");
            } else if (appPath != null && !(appPath.endsWith(".aab") || appPath.endsWith(".apk"))) {
                return FormValidation.error("Android app - File extension is not allowed," +
                        " allowed extensions are: '.apk' or '.aab'. Please rename your file or upload a different file.");
            }
            // Perform any additional validation here
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckFusionSetId(@QueryParameter String fusionSetId) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (fusionSetId != null && Util.fixEmptyAndTrim(fusionSetId) == null) {
                return FormValidation.error("FusionSet-ID must be provided.");
            } else if (fusionSetId != null && fusionSetId.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in FusionSetId.");
            }
            // Perform any additional validation here
            return FormValidation.ok("Chosen fusionSet: " + fusionSetId);
        }

        @Override
        public String getDisplayName() {
            return "Android";
        }
    }
}
