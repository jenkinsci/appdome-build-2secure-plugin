package io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method;

import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.appdome.build.to.secure.StringWarp;
import io.jenkins.plugins.appdome.build.to.secure.platform.eSignType;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.util.List;

public class AutoSign extends CertificateMethod {

    @SuppressWarnings("lgtm[jenkins/plaintext-storage]")
    private final String keystorePath;
    private final Secret keystorePassword;
    private List<StringWarp> provisioningProfiles;
    private final List<StringWarp> entitlements;

    @DataBoundConstructor
    public AutoSign(String name, String keystorePath, Secret keystorePassword, List<StringWarp> provisioningProfiles, List<StringWarp> entitlements) {
        super(eSignType.AUTO);
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.provisioningProfiles = provisioningProfiles;
        this.entitlements = entitlements;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public Secret getKeystorePassword() {
        return keystorePassword;
    }

    public List<StringWarp> getProvisioningProfiles() {
        return provisioningProfiles;
    }

    @DataBoundSetter
    public void setProvisioningProfiles(List<StringWarp> provisioningProfiles) {
        this.provisioningProfiles = provisioningProfiles;
    }

    public List<StringWarp> getEntitlements() {
        return entitlements;
    }


    public String getEntitlementsPath() {
        return concatenateStrings(entitlements);
    }

    public String getProvisioningProfilesPath() {
        return concatenateStrings(provisioningProfiles);
    }


    @Symbol("iOS_AutoSign")
    @Extension
    public static final class DescriptorImpl extends CertificateMethodDescriptor {


        @POST
        public FormValidation doCheckKeystorePath(@QueryParameter String keystorePath) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (keystorePath != null && Util.fixEmptyAndTrim(keystorePath) == null) {
                return FormValidation.warning("Path to keystore file must be provided." +
                        "Or please ensure that a valid path is provided for non-protected applications " +
                        "in the environment variable named 'KEYSTORE_PATH`'.");
            } else if (keystorePath != null && keystorePath.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in the path.");
            } else if (keystorePath != null && !(keystorePath.lastIndexOf(".") != -1
                    && keystorePath.substring(keystorePath.lastIndexOf(".")).equals(".p12"))) {
                return FormValidation.error("iOS keystore - File extension is not allowed," +
                        " allowed extensions are: '.p12'. Please rename your file or upload a different file.");
            }
            // Perform any additional validation here
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckKeystorePassword(@QueryParameter Secret keystorePassword) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (keystorePassword != null && Util.fixEmptyAndTrim(keystorePassword.getPlainText()) == null) {
                return FormValidation.error("Keystore's password must be provided.");
            }
            // Perform any additional validation here
            return FormValidation.ok();
        }


        @Override
        public String getDisplayName() {
            return "Auto Signing";
        }

    }
}
