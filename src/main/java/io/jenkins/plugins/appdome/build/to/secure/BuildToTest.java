package io.jenkins.plugins.appdome.build.to.secure;

import hudson.Extension;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;


public class BuildToTest extends AbstractDescribableImpl<BuildToTest> {


    private String vendors;
    private Boolean isBuildToTest;

    @DataBoundConstructor
    public BuildToTest(String vendors) {
        if (vendors != null) {
            this.vendors = vendors;
            this.isBuildToTest = true;
        } else {
            this.vendors = null;
            this.isBuildToTest = false;
        }
    }


    @DataBoundSetter
    public void setBuildToTest(String vendors) {
        if (vendors != null) {
            this.vendors = vendors;
            this.isBuildToTest = true;
        } else {
            this.vendors = null;
            this.isBuildToTest = false;
        }
    }

    public String getVendors() {
        return vendors;
    }

    public Boolean getBuildToTest() {
        return isBuildToTest;
    }

    @Symbol("AppdomeBuilder")
    @Extension
    public static class DescriptorImpl extends Descriptor<BuildToTest> {

        public ListBoxModel doFillVendorsItems() {
            ListBoxModel vendors = new ListBoxModel();

            for (Vendor vendor : Vendor.values()) {
                String name = vendor.name();
                String formattedName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                vendors.add(formattedName, name);
            }

            return vendors;
        }

        public String getDisplayName() {
            return "";
        }
    }

}
