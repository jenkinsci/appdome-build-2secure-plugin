package io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class AutoGoogleSign extends AbstractDescribableImpl<AutoGoogleSign> {

    private String googleSignFingerPrint;


    private Boolean isEnableGoogleSign;

    @DataBoundConstructor
    public AutoGoogleSign(String googleSignFingerPrint) {
        if (googleSignFingerPrint != null) {
            this.googleSignFingerPrint = googleSignFingerPrint;
            this.isEnableGoogleSign = true;
        } else {
            this.googleSignFingerPrint = null;
            this.isEnableGoogleSign = false;
        }
    }

    public String getGoogleSignFingerPrint() {
        return googleSignFingerPrint;
    }

    public Boolean getIsEnableGoogleSign() {
        return isEnableGoogleSign;
    }

    @DataBoundSetter
    public void setGoogleSignFingerPrint(String googleSignFingerPrint) {
        if (googleSignFingerPrint != null) {
            this.googleSignFingerPrint = googleSignFingerPrint;
            this.isEnableGoogleSign = true;
        } else {
            this.googleSignFingerPrint = null;
            this.isEnableGoogleSign = false;
        }
    }


    @Symbol({"Android_AutoGoogleSign", "SignOnAppdome_GoogleSign"})
    @Extension
    public static class DescriptorImpl extends Descriptor<AutoGoogleSign> {

        @POST
        public FormValidation doCheckGoogleSignFingerPrint(
                @QueryParameter String googleSignFingerPrint,
                @QueryParameter("trustedSigningFingerprintsFile") String trustedSigningFingerprintsFile) {
            return TrustedSigningFingerprintsFormValidation.validateGoogleFingerprintUnlessTrusted(
                    googleSignFingerPrint, trustedSigningFingerprintsFile);
        }
        public String getDisplayName() {
            return "";
        }
    }
}
