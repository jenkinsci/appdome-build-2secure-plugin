package io.jenkins.plugins.appdome.build.to.secure;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class DynamicCertificate {
    private String dynamicCertificate;

    @DataBoundConstructor
    public DynamicCertificate(String dynamicCertificate) {
        this.dynamicCertificate = dynamicCertificate;
    }

    @DataBoundSetter
    public void setDynamicCertificate(String dynamicCertificate) {
        this.dynamicCertificate = dynamicCertificate;
    }

    public String getDynamicCertificate() {
        return this.dynamicCertificate;
    }

}


