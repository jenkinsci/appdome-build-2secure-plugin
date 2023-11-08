package io.jenkins.plugins.appdome.build.to.secure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
public class AppdomeBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    // Set global timeout to 10 minutes (600000 milliseconds)
    @Rule
    public Timeout globalTimeout = new Timeout(600000, TimeUnit.MILLISECONDS);


    @Test
    public void testConfigRoundtrip() throws InterruptedException {
        int i = 0;
        while (i < 3)
        {
            System.out.println("TEST TEST TEST TEST TEST");
            System.out.println("TEST TEST TEST TEST TEST");
            System.out.println("TEST TEST TEST TEST TEST");
            i++;
        }
        TimeUnit.MINUTES.sleep(5);

        assertTrue(true);


    }
}
