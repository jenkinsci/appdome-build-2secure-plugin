package io.jenkins.plugins.appdome.build.to.secure.platform;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public abstract class Platform implements Describable<Platform>, ExtensionPoint {

    private String appPath;
    private String fusionSetId;
    private PlatformType platformType;


    public Platform(PlatformType platformType) {
        this.platformType = platformType;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public void setPlatformType(PlatformType platformType) {
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

