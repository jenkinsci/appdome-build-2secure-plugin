package io.jenkins.plugins.appdome.build.to.secure;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.security.ACL;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.Secret;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.AndroidPlatform;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.PrivateSign;

import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;


import java.util.*;

import static org.junit.Assert.assertTrue;

public class AppdomeBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    //    final String outputLocation = "/Users/idanhauser/work/output/081123144058_LocalMac_LOCAL_parallel/appdome_builder";
    final boolean buildWithLogs = false;
    final boolean BuildToTest = false;
    final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiMjg1YTRmNTAtNjAyZi0xMWVkLWFkMTYtMTFlM2RjZjJlYjA1Iiwic2FsdCI6Ijc0OGM5OWZhLTQwY2MtNDVhNC04M2I5LWU3ZTQ3NDU1MDg0YSJ9.lhSU5MOCwnvixbmAuygJoC9rKHQfkf0upSD4ows0B-E";
    final String teamId = "46002310-7cab-11ee-bfde-d76f94716e7a";


    @Before
    public void setUp() throws Exception {
        System.out.println("I AM IN SETUP");
        getpassword();
        setCommonEnvironmentVariables();
        downloadFilesForTestBuilds();

    }

    private void getpassword() {
        System.out.println("TRYING TO GET AWS PASSWORDS");
        List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class,
                Jenkins.getInstance(),
                null,
                Collections.emptyList());

        StandardUsernamePasswordCredentials awsCredentials = CredentialsMatchers.firstOrNull(
                credentials,
                CredentialsMatchers.withId("f6b5e6a2-6e97-498e-a32f-d478667ce94c"));

        if (awsCredentials != null) {

            System.out.println(awsCredentials.toString());
            String accessKey = awsCredentials.getUsername();
            String secretKey = awsCredentials.getPassword().getPlainText();
            System.out.println("accessKey " + accessKey);
            System.out.println("secretKey " + secretKey);
            // Use accessKey and secretKey for S3 operations
        }

    }

    private static void downloadFilesForTestBuilds() {

        String awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        String awsSecretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String awsDefaultRegion = "eu-central-1";
        String bucketName = "appdome-automation-vanilla-apps";

        Map<String, String> objects = new HashMap<>();
        // Add all the object mappings as in Python code

        if (awsAccessKeyId == null || awsSecretAccessKey == null) {
            System.out.println("Missing required environment variables.");
            System.exit(1);
        }
    }

    private void setCommonEnvironmentVariables() {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars env = prop.getEnvVars();
        env.put("APPDOME_SERVER_BASE_URL", "https://qamaster.dev.appdome.com");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
    }

    @Test(timeout = 600000)
    public void testAndroidPrivateSignBuild() throws Exception {
        setCommonEnvironmentVariables();
        System.out.println(jenkins.jenkins.root);
        FreeStyleProject project = jenkins.createFreeStyleProject();

        // Create configuration objects
        PrivateSign privateSign = new PrivateSign("8DF593C1B6EAA6EADADCE36831FE82B08CAC8D74");
        privateSign.setGoogleSigning(false);

        AndroidPlatform androidPlatform = new AndroidPlatform(privateSign);
        androidPlatform.setAppPath("https://github.com/idanhauser/TestAppdome_orb_private/raw/main/files/EmptyApp.apk");
        androidPlatform.setFusionSetId("8c693120-7cab-11ee-8275-c54d0e1c9b7a");


        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, androidPlatform, null);

        appdomeBuilder.setBuildToTest(null);
        appdomeBuilder.setBuildWithLogs(this.buildWithLogs);

        project.getBuildersList().add(appdomeBuilder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);


        String consoleOutput = build.getLog();
        System.out.println("build console output = " + consoleOutput);
        System.out.println("build status = " + build.getResult().toString());
        jenkins.assertBuildStatus(Result.SUCCESS, build); // Check build status
    }
}
