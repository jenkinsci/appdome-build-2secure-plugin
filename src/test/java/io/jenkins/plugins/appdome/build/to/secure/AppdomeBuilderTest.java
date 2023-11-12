package io.jenkins.plugins.appdome.build.to.secure;

import hudson.EnvVars;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.Secret;
import io.jenkins.plugins.appdome.build.to.secure.platform.Platform;
import io.jenkins.plugins.appdome.build.to.secure.platform.PlatformType;
import io.jenkins.plugins.appdome.build.to.secure.platform.SignType;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.AndroidPlatform;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.CertificateMethod;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.PrivateSign;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class AppdomeBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String outputLocation = "/Users/idanhauser/work/output/081123144058_LocalMac_LOCAL_parallel/appdome_builder";
    final boolean buildWithLogs = false;
    final boolean BuildToTest = false;
    final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiMjg1YTRmNTAtNjAyZi0xMWVkLWFkMTYtMTFlM2RjZjJlYjA1Iiwic2FsdCI6Ijc0OGM5OWZhLTQwY2MtNDVhNC04M2I5LWU3ZTQ3NDU1MDg0YSJ9.lhSU5MOCwnvixbmAuygJoC9rKHQfkf0upSD4ows0B-E";
    final String teamId = "46002310-7cab-11ee-bfde-d76f94716e7a";


    @Test(timeout = 600000)
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars env = prop.getEnvVars();
        env.put("APPDOME_SERVER_BASE_URL", "https://qamaster.dev.appdome.com");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
        // Create configuration objects
        PrivateSign privateSign = new PrivateSign("8DF593C1B6EAA6EADADCE36831FE82B08CAC8D74");
        privateSign.setGoogleSigning(false);

        AndroidPlatform androidPlatform = new AndroidPlatform(privateSign);
        androidPlatform.setAppPath("https://github.com/idanhauser/TestAppdome_orb_private/raw/main/files/EmptyApp.apk");
        androidPlatform.setFusionSetId("8c693120-7cab-11ee-8275-c54d0e1c9b7a");



        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, androidPlatform, null);
        appdomeBuilder.setOutputLocation(this.outputLocation);
        appdomeBuilder.setBuildToTest(null);
        appdomeBuilder.setBuildWithLogs(this.buildWithLogs);

        project.getBuildersList().add(appdomeBuilder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // Assert the results
        jenkins.assertBuildStatus(Result.SUCCESS, build); // Check build status


        String consoleOutput = build.getLog();
        assertTrue(consoleOutput.contains("appdome")); // Check console output

        System.out.println("build status = " + build.getResult().toString());
        System.out.println("build console output = " + consoleOutput.toString());
    }
}
