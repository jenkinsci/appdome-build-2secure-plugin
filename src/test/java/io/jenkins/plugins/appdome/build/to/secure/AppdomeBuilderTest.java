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
import org.junit.jupiter.api.DisplayName;
import org.jvnet.hudson.test.JenkinsRule;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        System.out.println("Jenkins Test Timeout: " + System.getProperty("jenkins.test.timeout"));
        setCommonEnvironmentVariables();
        printEnvironmentVariables();
//        getpassword();

//        downloadFilesForTestBuilds();

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

    private static void printEnvironmentVariables() {
        System.out.println("Printing Environment variables");
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            EnvironmentVariablesNodeProperty prop = jenkins.getGlobalNodeProperties().get(EnvironmentVariablesNodeProperty.class);
            if (prop != null) {
                EnvVars env = prop.getEnvVars();
                for (String key : env.keySet()) {
                    String value = env.get(key);
                    System.out.println(key + "=" + value);
                }
            } else {
                System.out.println("Environment variables property not found");
            }
        } else {
            System.out.println("Jenkins instance not found");
        }
    }
    private void setCommonEnvironmentVariables() {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars env = prop.getEnvVars();
        env.put("APPDOME_SERVER_BASE_URL", "https://qamaster.dev.appdome.com");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
    }

    @Test
    @DisplayName("Test Android Private Sign Build with valid parameters")
    public void testAndroidPrivateSignBuild() throws Exception {
        setCommonEnvironmentVariables();
        System.out.println(jenkins.jenkins.root);
        FreeStyleProject project = jenkins.createFreeStyleProject();
                // Create configuration objects
        PrivateSign privateSign = new PrivateSign("8DF593C1B6EAA6EADADCE36831FE82B08CAC8D74");
        privateSign.setGoogleSigning(false);
        executeShellCommand("pwd");
        executeShellCommand("ls -a");
        AndroidPlatform androidPlatform = new AndroidPlatform(privateSign);
        androidPlatform.setAppPath("https://github.com/idanhauser/TestAppdome_orb_private/raw/main/files/EmptyApp.apk");
        androidPlatform.setFusionSetId("8c693120-7cab-11ee-8275-c54d0e1c9b7a");


        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, androidPlatform, null);

        appdomeBuilder.setBuildToTest(null);
        appdomeBuilder.setBuildWithLogs(this.buildWithLogs);

        project.getBuildersList().add(appdomeBuilder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        System.err.println("TEST TEST TEST TEST");
        String consoleOutput = build.getLog();
        System.out.println("build console output = " + consoleOutput);
        System.out.println("build status = " + build.getResult().toString());
        jenkins.assertBuildStatus(Result.SUCCESS, build); // Check build status
    }

    private void executeShellCommand(String command) {
        System.out.println(command);
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();
            reader.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
