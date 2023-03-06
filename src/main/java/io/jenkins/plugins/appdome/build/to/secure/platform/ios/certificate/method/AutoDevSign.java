package io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method;

import hudson.Extension;
import io.jenkins.plugins.appdome.build.to.secure.StringWarp;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import io.jenkins.plugins.appdome.build.to.secure.platform.eSignType;
import java.util.List;

public class AutoDevSign extends CertificateMethod {
    private final List<StringWarp> provisioningProfiles;
    private final List<StringWarp> entitlements;

    @DataBoundConstructor
    public AutoDevSign(List<StringWarp> provisioningProfiles, List<StringWarp> entitlements) {
        super(eSignType.AUTODEV);
        this.provisioningProfiles = provisioningProfiles;
        this.entitlements = entitlements;
    }

    public List<StringWarp> getProvisioningProfiles() {
        return provisioningProfiles;
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


    @Symbol("iOS_AutoDevSign")
    @Extension
    public static final class DescriptorImpl extends CertificateMethodDescriptor {
        @Override
        public String getDisplayName() {
            return "Auto-Dev Signing";
        }


    }
}
