package io.jenkins.plugins.appdome.build.to.secure.platform.android;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.export.Exported;

public class Datadog extends AbstractDescribableImpl<Datadog> {

    private final String datadogKey;

    @DataBoundConstructor
    public Datadog(String datadogKey) {
        this.datadogKey = datadogKey;
    }

    @Exported
    public String getDatadogKey() {
        return datadogKey;
    }

    @Symbol("Datadog")
    @Extension
    public static class DescriptorImpl extends Descriptor<Datadog> {
        public String getDisplayName() {
            return "Datadog Configuration";
        }
    }
}
