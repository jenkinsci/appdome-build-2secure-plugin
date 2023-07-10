package io.jenkins.plugins.appdome.build.to.secure;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class BuildToTest {
    private String selectedVendor;

    @DataBoundConstructor
    public BuildToTest(String selectedVendor) {
        this.selectedVendor = selectedVendor;
    }

    @DataBoundSetter
    public void setSelectedVendor(String selectedVendor) {
        this.selectedVendor = selectedVendor;
    }

    public String getSelectedVendor() {
        return selectedVendor;
    }
}
