package io.jenkins.plugins.appdome.build.to.secure.platform;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.appdome.build.to.secure.StringWarp;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public abstract class Platform implements Describable<Platform>, ExtensionPoint {

    private String appPath;
    private String fusionSetId;
    private ePlatformType platformType;


    public Platform(ePlatformType platformType) {
        this.platformType = platformType;
    }

    public ePlatformType getPlatformType() {
        return platformType;
    }

    public void setPlatformType(ePlatformType platformType) {
        this.platformType = platformType;
    }


    public String getAppPath() {
        return appPath;
    }


    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }


    public void setFusionSetId(String fusionSetId) {
        this.fusionSetId = fusionSetId;
    }

    public String getFusionSetId() {
        return fusionSetId;
    }


    @Override
    public Descriptor<Platform> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }

//
//    @Extension
//    public static class DescriptorImpl extends Descriptor<Platform> {
//
//
//        public String getDisplayName() {
//            return null;
//        }
//    }
}

