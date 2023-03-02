package io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.appdome.build.to.secure.platform.eSignType;
import jenkins.model.Jenkins;

public class CertificateMethod implements Describable<CertificateMethod>, ExtensionPoint {
    private eSignType signType;

    public CertificateMethod(eSignType signType) {
        this.signType = signType;
    }


    public eSignType getSignType() {
        return signType;
    }

    public void setSignType(eSignType signType) {
        this.signType = signType;
    }
    @Override
    public Descriptor<CertificateMethod> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }
}
