package io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method;

import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.CertificateMethodDescriptor;
import io.jenkins.plugins.appdome.build.to.secure.platform.eSignType;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class AutoDevSign extends CertificateMethod {

    private final String fingerprint;
    private Boolean googleSigning;

    @DataBoundConstructor
    public AutoDevSign(String fingerprint) {

        super(eSignType.AUTODEV);
        this.fingerprint = fingerprint;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public Boolean getGoogleSigning() {
        return googleSigning;
    }

    @DataBoundSetter
    public void setGoogleSigning(Boolean googleSigning) {
        this.googleSigning = googleSigning;
    }

    @Symbol("Android_AutoDevSign")
    @Extension
    public static final class DescriptorImpl extends CertificateMethodDescriptor {

        @POST
        public FormValidation doCheckFingerprint(@QueryParameter String fingerprint) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (fingerprint != null && Util.fixEmptyAndTrim(fingerprint) == null) {
                return FormValidation.error("Fingerprint must be provided.");
            }else if (fingerprint != null && fingerprint.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in Fingerprint.");
            }
            // Perform any additional validation here
            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return "Auto-Dev Signing";
        }
    }
}
