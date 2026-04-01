package io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method;

import hudson.Extension;
import hudson.util.FormValidation;
import io.jenkins.plugins.appdome.build.to.secure.platform.SignType;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class AutoDevSign extends CertificateMethod implements TrustedSigningFingerprintsConfig {

    private final String fingerprint;
    private Boolean googleSigning;
    private Boolean trustedSigningFingerprintsFile;
    private String signingFingerprintListPath;

    @DataBoundConstructor
    public AutoDevSign(String fingerprint) {

        super(SignType.AUTODEV);
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

    public Boolean getTrustedSigningFingerprintsFile() {
        return trustedSigningFingerprintsFile;
    }

    @DataBoundSetter
    public void setTrustedSigningFingerprintsFile(Boolean trustedSigningFingerprintsFile) {
        this.trustedSigningFingerprintsFile = trustedSigningFingerprintsFile;
    }

    public String getSigningFingerprintListPath() {
        return signingFingerprintListPath;
    }

    @DataBoundSetter
    public void setSigningFingerprintListPath(String signingFingerprintListPath) {
        this.signingFingerprintListPath = signingFingerprintListPath;
    }

    @Symbol("Android_AutoDevSign")
    @Extension
    public static final class DescriptorImpl extends CertificateMethodDescriptor {

        @POST
        public FormValidation doCheckFingerprint(
                @QueryParameter String fingerprint,
                @QueryParameter("trustedSigningFingerprintsFile") String trustedSigningFingerprintsFile) {
            return TrustedSigningFingerprintsFormValidation.validateFingerprintUnlessTrusted(
                    fingerprint, trustedSigningFingerprintsFile);
        }

        @POST
        public FormValidation doCheckSigningFingerprintListPath(
                @QueryParameter String signingFingerprintListPath,
                @QueryParameter("trustedSigningFingerprintsFile") String trustedSigningFingerprintsFile) {
            return TrustedSigningFingerprintsFormValidation.validateListPath(
                    signingFingerprintListPath, trustedSigningFingerprintsFile);
        }

        @Override
        public String getDisplayName() {
            return "Auto-Dev Signing";
        }
    }
}
