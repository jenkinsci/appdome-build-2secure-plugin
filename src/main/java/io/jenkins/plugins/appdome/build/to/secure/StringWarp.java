package io.jenkins.plugins.appdome.build.to.secure;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class StringWarp extends AbstractDescribableImpl<StringWarp> {

    private final String item;

    @DataBoundConstructor
    public StringWarp(String item) {
        this.item = item;
    }

    public String getItem() {
        return this.item;
    }

    public String getProvisioningProfiles() {
        return this.item;
    }

    public String getEntitlements() {
        return this.item;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<StringWarp> {

        @POST
        public FormValidation doCheckProvisioningProfiles(@QueryParameter String item) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (item != null && Util.fixEmptyAndTrim(item) == null) {
                return FormValidation.warning("please add at least one Provisioning Profile file and ensure that all entries" +
                        " are filled without any empty fields.Or please ensure that a valid path is provided " +
                        "for Provisioning Profile files in the environment variable named 'MOBILE_PROVISION_PROFILE_PATHS' instead");
            } else if (item != null && item.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in the path.");
            } else if (item != null && !(item.lastIndexOf(".") != -1
                    && item.substring(item.lastIndexOf(".")).equals(".mobileprovision"))) {
                return FormValidation.error("Provisioning profile - File extension is not allowed," +
                        " allowed extensions are: '.mobileprovision'. Please rename your file or upload a different file.");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckEntitlements(@QueryParameter String item) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (item != null && Util.fixEmptyAndTrim(item) == null) {
                return FormValidation.warning("please add at least one entitlement file and ensure that all entries" +
                        " are filled without any empty fields.Or please ensure that a valid path is provided " +
                        "for entitlements files in the environment variable named 'ENTITLEMENTS_PATHS' instead");
            } else if (item != null && item.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in the path.");
            } else if (item != null && !(item.lastIndexOf(".") != -1
                    && item.substring(item.lastIndexOf(".")).equals(".plist"))) {
                return FormValidation.error("Entitlements - File extension is not allowed," +
                        " allowed extensions are: '.plist'. Please rename your file or upload a different file.");
            }

            // Perform any additional validation here
            return FormValidation.ok();
        }

        public String getDisplayName() {
            return "";
        }
    }
}
