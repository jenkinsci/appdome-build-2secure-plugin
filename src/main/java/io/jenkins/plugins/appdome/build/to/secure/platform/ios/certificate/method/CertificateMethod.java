package io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;

import io.jenkins.plugins.appdome.build.to.secure.StringWarp;
import io.jenkins.plugins.appdome.build.to.secure.platform.Platform;
import io.jenkins.plugins.appdome.build.to.secure.platform.eSignType;
import jenkins.model.Jenkins;

import java.util.List;

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

    public String concatenateStrings(List<StringWarp> strList) {
        if (strList != null) {
            StringBuilder concatenatePath = new StringBuilder();
            for (StringWarp path : strList) {
                concatenatePath.append(((StringWarp) path).getItem()).append(',');
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
