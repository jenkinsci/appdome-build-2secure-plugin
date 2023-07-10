package io.jenkins.plugins.appdome.build.to.secure;

import hudson.util.ListBoxModel;

public class VendorManager {

    public enum Vendor {
        SAUCELABS,
        BITBAR,
        LAMBDATEST,
        BROWSERSTACK
    }

    private static final VendorManager instance = new VendorManager();

    private VendorManager() {
    }

    public static VendorManager getInstance() {
        return instance;
    }

    public ListBoxModel getVendors() {
        ListBoxModel vendors = new ListBoxModel();
        for (Vendor vendor : Vendor.values()) {
            String name = vendor.name();
            String formattedName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            vendors.add(formattedName, name);
        }
        return vendors;
    }
}
