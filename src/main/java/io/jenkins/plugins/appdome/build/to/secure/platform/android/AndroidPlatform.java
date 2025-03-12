package io.jenkins.plugins.appdome.build.to.secure.platform.android;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.cli.shaded.org.apache.commons.lang.StringUtils;
import io.jenkins.plugins.appdome.build.to.secure.platform.Platform;
import io.jenkins.plugins.appdome.build.to.secure.platform.PlatformDescriptor;
import io.jenkins.plugins.appdome.build.to.secure.platform.PlatformType;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.CertificateMethod;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import static io.jenkins.plugins.appdome.build.to.secure.AppdomeBuilder.isHttpUrl;

public class AndroidPlatform extends Platform {

    private final CertificateMethod certificateMethod;
    private Crashlytics crashlytics;
    private Boolean isCrashlytics;


    private Datadog datadog;
    private Boolean isDatadog;

    @DataBoundConstructor
    public AndroidPlatform(CertificateMethod certificateMethod) {
        super(PlatformType.ANDROID);
        this.certificateMethod = certificateMethod;
    }


    //    Crashlytics
    public Boolean getIsCrashlytics() {
        return this.crashlytics != null && this.crashlytics.getFirebaseAppId() != null;
    }

    public Crashlytics getCrashlytics() {  // Changed from getCrashlyticsPublisher to getCrashlytics
        return crashlytics;
    }

    @DataBoundSetter
    public void setCrashlytics(Crashlytics crashlytics) {
        this.crashlytics = crashlytics;
    }

    public String getFirebaseAppId() {
        if (this.crashlytics != null) {
            return this.crashlytics.getFirebaseAppId().toString();
        } else
            return null;
    }

    //    DATADOG
    public Boolean getIsDatadog() {
        return this.datadog != null && this.datadog.getDatadogKey() != null;
    }

    public Datadog getDatadog() {
        return datadog;
    }

    @DataBoundSetter
    public void setDatadog(Datadog datadog) {
        this.datadog = datadog;
    }

    public String getDatadogKey() {
        if (this.datadog != null) {
            return this.datadog.getDatadogKey();
        } else {
            return null;
        }
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

        @POST
        public FormValidation doCheckFirebaseAppId(@QueryParameter String firebaseAppId) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER); // Ensure correct permission

            if (StringUtils.isBlank(firebaseAppId)) {
                return FormValidation.error("Firebase App ID must be provided.");
            } else if (firebaseAppId.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in Firebase App ID.");
            }

            // Additional validation logic
            try {
                // Simulate additional checks here
                return FormValidation.ok("Chosen Firebase App ID: " + firebaseAppId);
            } catch (Exception e) {
                return FormValidation.error("An error occurred: " + e.getMessage());
            }
        }


        @POST
        public FormValidation doCheckDatadogKey(@QueryParameter String datadogKey) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER); // Ensure correct permission

            if (StringUtils.isBlank(datadogKey)) {
                return FormValidation.error("Firebase App ID must be provided.");
            } else if (datadogKey.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in Firebase App ID.");
            }

            // Additional validation logic
            try {
                // Simulate additional checks here
                return FormValidation.ok("Datadog key: " + datadogKey);
            } catch (Exception e) {
                return FormValidation.error("An error occurred: " + e.getMessage());
            }
        }

        @Override
        public String getDisplayName() {
            return "Android";
        }
    }
}
