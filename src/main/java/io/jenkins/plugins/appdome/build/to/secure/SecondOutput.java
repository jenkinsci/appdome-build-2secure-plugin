package io.jenkins.plugins.appdome.build.to.secure;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class SecondOutput {
    private String secondOutput;

    @DataBoundConstructor
    public SecondOutput(String secondOutput) {
        this.secondOutput = secondOutput;
    }

    @DataBoundSetter
    public void setSecondOutput(String secondOutput) {
        this.secondOutput = secondOutput;
    }

    public String getSecondOutput() {
        return secondOutput;
    }

}
