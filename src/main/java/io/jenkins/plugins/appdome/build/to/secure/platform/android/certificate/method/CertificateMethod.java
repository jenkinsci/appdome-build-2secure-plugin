package io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.appdome.build.to.secure.platform.SignType;
import jenkins.model.Jenkins;

public class CertificateMethod implements Describable<CertificateMethod>, ExtensionPoint {
    private SignType signType;

    public CertificateMethod(SignType signType) {
        this.signType = signType;
    }


    public SignType getSignType() {
        return signType;
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }
    @Override
    public Descriptor<CertificateMethod> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }
}
