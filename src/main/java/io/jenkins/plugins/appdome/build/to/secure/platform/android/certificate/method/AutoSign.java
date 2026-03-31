package io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method;

import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.appdome.build.to.secure.platform.SignType;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class AutoSign extends CertificateMethod implements TrustedSigningFingerprintsConfig {

    @SuppressWarnings("lgtm[jenkins/plaintext-storage]")
    private final String keystorePath;
    private final Secret keystorePassword;
    private final Secret keystoreAlias;
    private final Secret keyPass;
    private AutoGoogleSign googleSignFingerPrint;
    private Boolean isEnableGoogleSign;
    private Boolean trustedSigningFingerprintsFile;
    private String signingFingerprintListPath;

    @DataBoundConstructor
    public AutoSign(String keystorePath, Secret keystorePassword, Secret keystoreAlias, Secret keyPass, AutoGoogleSign googleSignFingerPrint) {
        super(SignType.AUTO);
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keystoreAlias = keystoreAlias;
        this.keyPass = keyPass;
        this.googleSignFingerPrint = googleSignFingerPrint;
    }

    @DataBoundSetter
    public void setGoogleSignFingerPrint(AutoGoogleSign googleSignFingerPrint) {
        this.googleSignFingerPrint = googleSignFingerPrint;
    }

    public Boolean getEnableGoogleSign() {
        return isEnableGoogleSign;
    }

    public void setEnableGoogleSign(Boolean enableGoogleSign) {
        isEnableGoogleSign = enableGoogleSign;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public Secret getKeystorePassword() {
        return keystorePassword;
    }

    public Secret getKeystoreAlias() {
        return keystoreAlias;
    }

    public Secret getKeyPass() {
        return keyPass;
    }


    public String getGoogleSignFingerPrint() {
        if (googleSignFingerPrint != null) {
            return googleSignFingerPrint.getGoogleSignFingerPrint();
        }
        return null;
    }

    public Boolean getIsEnableGoogleSign() {
        if (googleSignFingerPrint != null) {
            return googleSignFingerPrint.getIsEnableGoogleSign();
        }
        return false;
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


    @DataBoundSetter
    public void setGoogleSign(AutoGoogleSign googleSignFingerPrint) {
        this.googleSignFingerPrint = googleSignFingerPrint;
    }

    @Symbol({"Android_AutoSign", "Android_SignOnAppdome"})
    @Extension
    public static final class DescriptorImpl extends CertificateMethodDescriptor {

        @POST
        public FormValidation doCheckKeystorePath(@QueryParameter String keystorePath) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (keystorePath != null && Util.fixEmptyAndTrim(keystorePath) == null) {
                return FormValidation.warning("Path to keystore file must be provided." +
                        "Or please ensure that a valid path is provided for non-protected applications in the environment variable named 'KEYSTORE_PATH'.");
            } else if (keystorePath != null && keystorePath.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in the path.");
            }
            // Perform any additional validation here
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckKeystorePassword(@QueryParameter Secret keystorePassword) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (keystorePassword != null && Util.fixEmptyAndTrim(keystorePassword.getPlainText()) == null) {
                return FormValidation.error("Keystore's Password must be provided.");
            }
            // Perform any additional validation here
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckKeystoreAlias(@QueryParameter Secret keystoreAlias) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (keystoreAlias != null && Util.fixEmptyAndTrim(keystoreAlias.getPlainText()) == null) {
                return FormValidation.error("Keystore alias must be provided.");
            }
            // Perform any additional validation here
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckKeyPass(@QueryParameter Secret keyPass) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (keyPass != null && Util.fixEmptyAndTrim(keyPass.getPlainText()) == null) {
                return FormValidation.error("Key pass must be provided.");
            }
            // Perform any additional validation here
            return FormValidation.ok();
        }


        @POST
        public FormValidation doCheckGoogleSignFingerPrint(
                @QueryParameter String googleSignFingerPrint,
                @QueryParameter("trustedSigningFingerprintsFile") String trustedSigningFingerprintsFile) {
            return TrustedSigningFingerprintsFormValidation.validateGoogleFingerprintUnlessTrusted(
                    googleSignFingerPrint, trustedSigningFingerprintsFile);
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
            return "Sign On Appdome";
        }
    }
}
