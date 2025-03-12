package io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;

import io.jenkins.plugins.appdome.build.to.secure.StringWarp;
import io.jenkins.plugins.appdome.build.to.secure.platform.SignType;
import jenkins.model.Jenkins;

import java.util.List;

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

    public String concatenateStrings(List<StringWarp> strList) {
        if (strList != null) {
            StringBuilder concatenatePath = new StringBuilder();
            for (StringWarp path : strList) {
                concatenatePath.append(path.getItem()).append(',');
            }
            return concatenatePath.substring(0, concatenatePath.length() - 1).trim();
        }
        return null;
    }


    @Override
    public Descriptor<CertificateMethod> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }
}
