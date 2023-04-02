package io.jenkins.plugins.appdome.build.to.secure.platform.ios;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.appdome.build.to.secure.platform.Platform;
import io.jenkins.plugins.appdome.build.to.secure.platform.PlatformDescriptor;
import io.jenkins.plugins.appdome.build.to.secure.platform.PlatformType;
import io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.CertificateMethod;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class IosPlatform extends Platform {

    private final CertificateMethod certificateMethod;

    @DataBoundConstructor
    public IosPlatform(CertificateMethod certificateMethod) {
        super(PlatformType.IOS);
        this.certificateMethod = certificateMethod;
    }

    @DataBoundSetter
    public void setAppPath(String appPath) {
        super.setAppPath(appPath);
    }


    @DataBoundSetter
    public void setFusionSetId(String fusionSetId) {
        super.setFusionSetId(fusionSetId);
    }

    public String getAppPath() {
        return super.getAppPath();
    }

    public String getFusionSetId() {
        return super.getFusionSetId();
    }

    public CertificateMethod getCertificateMethod() {
        return certificateMethod;
    }

    public DescriptorExtensionList<CertificateMethod, Descriptor<CertificateMethod>> getCertificateMethodDescriptors() {
        return Jenkins.get().getDescriptorList(CertificateMethod.class);
    }

    @Symbol("IosPlatform")
    @Extension
    public static final class DescriptorImpl extends PlatformDescriptor {

        @POST
        public FormValidation doCheckAppPath(@QueryParameter String appPath) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (appPath != null && Util.fixEmptyAndTrim(appPath) == null) {
                return FormValidation.warning("Application path was not provided.\n " +
                        "Please ensure that a valid path is provided for non-protected applications in the environment variable called 'APP_PATH'.");
            } else if (appPath != null && appPath.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in the path.");
            } else if (appPath != null && !(appPath.endsWith(".ipa"))) {
                return FormValidation.error("iOS app - File extension is not allowed," +
                        " allowed extensions are: '.ipa'. Please rename your file or upload a different file.");
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
            return "iOS";
        }

    }
}
