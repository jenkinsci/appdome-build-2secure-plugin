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
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.AutoDevSign;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.PrivateSign;

import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.jvnet.hudson.test.JenkinsRule;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class AppdomeBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private static final String PATH_TO_FILES = "/presigned_urls/downloaded_files";
    final String androidFusionSet = "8c693120-7cab-11ee-8275-c54d0e1c9b7a";
    final String iosFusionSet = "8c693120-7cab-11ee-8275-c54d0e1c9b7a";
    final String fingerprint = "8DF593C1B6EAA6EADADCE36831FE82B08CAC8D74";
    final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiMjg1YTRmNTAtNjAyZi0xMWVkLWFkMTYtMTFlM2RjZjJlYjA1Iiwic2FsdCI6Ijc0OGM5OWZhLTQwY2MtNDVhNC04M2I5LWU3ZTQ3NDU1MDg0YSJ9.lhSU5MOCwnvixbmAuygJoC9rKHQfkf0upSD4ows0B-E";
    final String teamId = "46002310-7cab-11ee-bfde-d76f94716e7a";
    private String aabAppPath;
    private String apkAppPath;
    private String certificateFilePath;
    private String ipa1EntitlementsPath;
    private String ipa1MobileProvisioningPath;
    private String ipa2Entitlements1Path;
    private String ipa2Entitlements2Path;
    private String ipa2Entitlements3Path;
    private String ipa2MobileProvisioning1Path;
    private String ipa2MobileProvisioning2Path;
    private String ipa2MobileProvisioning3Path;
    private String ipaApp1Path;
    private String ipaApp2Path;
    private String keystoreFilePath;


    @Before
    public void setUp() throws Exception {
        setCommonEnvironmentVariables();
        setFiles();
    }

    private void setFiles() {
        this.aabAppPath = buildFilePath("aab_app.aab");
        this.apkAppPath = buildFilePath("apk_app.apk");
        this.certificateFilePath = buildFilePath("certificate_file.p12");
        this.ipa1EntitlementsPath = buildFilePath("ipa_1_entitlements.plist");
        this.ipa1MobileProvisioningPath = buildFilePath("ipa_1_mobile_provisioning.mobileprovision");
        this.ipa2Entitlements1Path = buildFilePath("ipa_2_entitlements_1.plist");
        this.ipa2Entitlements2Path = buildFilePath("ipa_2_entitlements_2.plist");
        this.ipa2Entitlements3Path = buildFilePath("ipa_2_entitlements_3.plist");
        this.ipa2MobileProvisioning1Path = buildFilePath("ipa_2_mobile_provisioning_1.mobileprovision");
        this.ipa2MobileProvisioning2Path = buildFilePath("ipa_2_mobile_provisioning_2.mobileprovision");
        this.ipa2MobileProvisioning3Path = buildFilePath("ipa_2_mobile_provisioning_3.mobileprovision");
        this.ipaApp1Path = buildFilePath("ipa_app_1.ipa");
        this.ipaApp2Path = buildFilePath("ipa_app_2.ipa");
        this.keystoreFilePath = buildFilePath("keystore_file.keystore");
    }


    private String buildFilePath(String filename) {
        System.out.println("BuildFilePath");
        executeShellCommand("pwd");
        executeShellCommand("ls -a");

        File file = new File(PATH_TO_FILES, filename);
        System.out.println(filename+" : "+file.getAbsolutePath().toString());
        if (!file.exists()) {
            throw new IllegalStateException("Required file not found: " + file.getAbsolutePath());
        }
        return file.getAbsolutePath();
    }

    private void setCommonEnvironmentVariables() {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars env = prop.getEnvVars();
        env.put("APPDOME_SERVER_BASE_URL", "https://qamaster.dev.appdome.com");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
    }

    @Test
    public void testAndroidPrivateSignBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        // Create configuration objects
        PrivateSign privateSign = new PrivateSign(fingerprint);
        privateSign.setGoogleSigning(false);
        AndroidPlatform androidPlatform = new AndroidPlatform(privateSign);
        androidPlatform.setFusionSetId(androidFusionSet);
        androidPlatform.setAppPath(this.apkAppPath);
        executeShellCommand("pwd");
        executeShellCommand("ls -a");
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, androidPlatform, null);

        appdomeBuilder.setBuildToTest(null);
        appdomeBuilder.setBuildWithLogs(true);

        project.getBuildersList().add(appdomeBuilder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        String consoleOutput = build.getLog();
        System.out.println("build console output = " + consoleOutput);
        System.out.println("build status = " + build.getResult().toString());
        jenkins.assertBuildStatus(Result.SUCCESS, build); // Check build status
    }

    @Test
    public void testAndroidAutoDevSignBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        // Create configuration objects
        AutoDevSign autoDevSign = new AutoDevSign(fingerprint);
        autoDevSign.setGoogleSigning(true);
        AndroidPlatform androidPlatform = new AndroidPlatform(autoDevSign);
        androidPlatform.setFusionSetId(androidFusionSet);
        androidPlatform.setAppPath(this.aabAppPath);
        executeShellCommand("pwd");
        executeShellCommand("ls -a");
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, androidPlatform, null);
        BuildToTest buildToTest = new BuildToTest(VendorManager.Vendor.SAUCELABS.name());
        appdomeBuilder.setBuildToTest(buildToTest);
        appdomeBuilder.setBuildWithLogs(false);

        project.getBuildersList().add(appdomeBuilder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
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
