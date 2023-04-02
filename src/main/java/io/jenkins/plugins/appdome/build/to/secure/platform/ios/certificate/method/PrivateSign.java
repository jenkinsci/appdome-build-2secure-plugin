package io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method;

import hudson.Extension;
import io.jenkins.plugins.appdome.build.to.secure.StringWarp;
import io.jenkins.plugins.appdome.build.to.secure.platform.SignType;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class PrivateSign extends CertificateMethod {
    private final List<StringWarp> provisioningProfiles;

    @DataBoundConstructor
    public PrivateSign(List<StringWarp> provisioningProfiles) {
        super(SignType.PRIVATE);
        this.provisioningProfiles = provisioningProfiles;
    }

    public List<StringWarp> getProvisioningProfiles() {
        return provisioningProfiles;
    }

    public String getProvisioningProfilesPath() {
        return concatenateStrings(provisioningProfiles);
    }

    @Symbol("iOS_PrivateSign")
    @Extension
    public static final class DescriptorImpl extends CertificateMethodDescriptor {
        @Override
        public String getDisplayName() {
            return "Private Signing";
        }

    }
}
