package io.jenkins.plugins.appdome.build.to.secure.platform.android;

import org.kohsuke.stapler.DataBoundConstructor;

public class Datadog {

    private final String datadogKey;

    @DataBoundConstructor
    public Datadog(String datadogKey) {
        this.datadogKey = datadogKey;
    }


    public String getDatadogKey() {
        return this.datadogKey;
    }

}