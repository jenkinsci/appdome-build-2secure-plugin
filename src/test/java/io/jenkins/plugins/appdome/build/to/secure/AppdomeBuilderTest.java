package io.jenkins.plugins.appdome.build.to.secure;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.security.ACL;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.Secret;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.AndroidPlatform;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.AutoDevSign;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.AutoSign;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.PrivateSign;

import io.jenkins.plugins.appdome.build.to.secure.platform.ios.IosPlatform;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AppdomeBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private static final String PATH_TO_FILES = "downloaded_files/";
    final String androidFusionSet = "8c693120-7cab-11ee-8275-c54d0e1c9b7a";
    final String iosFusionSet = "13ded0a0-7cad-11ee-b531-29c8c84aedcc";
    final String fingerprint = "8DF593C1B6EAA6EADADCE36831FE82B08CAC8D74";
    private String token;
    final String teamId = "46002310-7cab-11ee-bfde-d76f94716e7a";
    private String aabAppPath;
    private String apkApp1Path;
    private String apkApp2Path;
    private String certificateFile1Path;
    private String certificateFile2Path;
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
    private String ipaApp3Path;
    private String keystoreFilePath;

    private String keystoreAlias;
    private String keystoreKeyPass;
    private String keystorePassword;
    private String p12Password;

    @Before
    public void setUp() throws Exception {
        this.token = System.getenv("APPDOME_API_TOKEN");
        this.keystoreAlias = System.getenv("KEYSTORE_ALIAS");
        this.keystoreKeyPass = System.getenv("KEYSTORE_KEY_PASS");
        this.keystorePassword = System.getenv("KEYSTORE_PASSWORD");
        this.p12Password = System.getenv("P12_PASSWORD");


        this.
                setCommonEnvironmentVariables();
        setFiles();
    }

    private void setFiles() {
        this.aabAppPath = buildFilePath("aab_app.aab");
        this.apkApp1Path = buildFilePath("apk_app_1.apk");
        this.apkApp2Path = buildFilePath("apk_app_2.apk");
        this.certificateFile1Path = buildFilePath("certificate_file1.p12");
        this.certificateFile2Path = buildFilePath("certificate_file2.p12");
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
        this.ipaApp3Path = buildFilePath("ipa_app_3.ipa");
        this.keystoreFilePath = buildFilePath("keystore_file.keystore");


    }


    private String buildFilePath(String filename) {
        File file = new File(PATH_TO_FILES, filename);
        System.out.println(filename + " : " + file.getAbsolutePath().toString());
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
        androidPlatform.setAppPath(this.apkApp1Path);
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
        androidPlatform.setAppPath(this.apkApp2Path);
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

    @Test
    public void testAndroidAutoSignBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        // Create configuration objects

        AutoSign autoSign =
                new AutoSign(this.keystoreFilePath,
                        Secret.fromString(this.keystorePassword), Secret.fromString(this.keystoreAlias),
                        Secret.fromString(keystoreKeyPass), null);

        AndroidPlatform androidPlatform = new AndroidPlatform(autoSign);
        androidPlatform.setFusionSetId(androidFusionSet);
        androidPlatform.setAppPath(aabAppPath);
        System.out.println("project.getSomeWorkspace : " + project.getSomeWorkspace().getRemote());
        System.out.println("project.getWorkspace : " + project.getWorkspace());
        System.out.println("project.getCustomWorkspace : " + project.getCustomWorkspace());
        FilePath second_output_file = new FilePath(project.getWorkspace(), "output/second_output.apk");
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId,
                androidPlatform, new StringWarp("/output/second_output.apk"));
        executeShellCommand("pwd");
        executeShellCommand("ls");
        appdomeBuilder.setBuildToTest(null);


        project.getBuildersList().add(appdomeBuilder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        FilePath workspace = build.getWorkspace();

        // Check that the file exists in the workspace
        FilePath output_location = workspace.child("output");


        FilePath outputFile = output_location.child("second_output.apk");
        System.out.println("output_location : " + output_location.getRemote() );
        System.out.println("outputFile : " + outputFile.getRemote() );
        assertNotNull("Workspace should exist", output_location);
        assertTrue("Output APK file should exist", outputFile.exists());

        String consoleOutput = build.getLog();
        System.out.println("build console output = " + consoleOutput);
        System.out.println("build status = " + build.getResult().toString());
        jenkins.assertBuildStatus(Result.SUCCESS, build); // Check build status
    }


    @Test
    public void testIosAutoSignBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        List<StringWarp> provision_profiles = new ArrayList<>();
        provision_profiles.add(new StringWarp(ipa2MobileProvisioning1Path));
        provision_profiles.add(new StringWarp(ipa2MobileProvisioning2Path));
        provision_profiles.add(new StringWarp(ipa2MobileProvisioning3Path));
        List<StringWarp> entitlements = new ArrayList<>();
        entitlements.add(new StringWarp(ipa2Entitlements1Path));
        entitlements.add(new StringWarp(ipa2Entitlements2Path));
        entitlements.add(new StringWarp(ipa2Entitlements3Path));

        // Create configuration objects
        io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.AutoSign autoSign
                = new io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.
                AutoSign(this.certificateFile2Path, Secret.fromString(this.p12Password), provision_profiles, entitlements);

        IosPlatform iosPlatform = new IosPlatform(autoSign);
        iosPlatform.setFusionSetId(iosFusionSet);
        iosPlatform.setAppPath(this.ipaApp2Path);
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, iosPlatform, null);
        BuildToTest buildToTest = new BuildToTest(VendorManager.Vendor.SAUCELABS.name());
        appdomeBuilder.setBuildToTest(buildToTest);
        appdomeBuilder.setBuildWithLogs(true);

        project.getBuildersList().add(appdomeBuilder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        String consoleOutput = build.getLog();
        System.out.println("build console output = " + consoleOutput);
        System.out.println("build status = " + build.getResult().toString());
        jenkins.assertBuildStatus(Result.SUCCESS, build); // Check build status
    }

    @Test
    public void testIosPrivateSignBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        List<StringWarp> provision_profiles = new ArrayList<>();
        provision_profiles.add(new StringWarp(ipa1MobileProvisioningPath));


        // Create configuration objects
        io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.PrivateSign privateSign
                = new io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.
                PrivateSign(provision_profiles);
        IosPlatform iosPlatform = new IosPlatform(privateSign);
        iosPlatform.setFusionSetId(iosFusionSet);
        iosPlatform.setAppPath(this.ipaApp1Path);
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, iosPlatform, null);
        BuildToTest buildToTest = new BuildToTest(VendorManager.Vendor.BROWSERSTACK.name());
        appdomeBuilder.setBuildToTest(buildToTest);
        appdomeBuilder.setBuildWithLogs(false);

        project.getBuildersList().add(appdomeBuilder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        String consoleOutput = build.getLog();
        System.out.println("build console output = " + consoleOutput);
        System.out.println("build status = " + build.getResult().toString());
        jenkins.assertBuildStatus(Result.SUCCESS, build); // Check build status
    }

    @Test
    public void testIosAutoDevPrivateSignBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        List<StringWarp> provision_profiles = new ArrayList<>();
        provision_profiles.add(new StringWarp(ipa1MobileProvisioningPath));
        List<StringWarp> entitlements = new ArrayList<>();
        entitlements.add(new StringWarp(this.ipa1EntitlementsPath));

        // Create configuration objects
        io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.AutoDevSign
                autoDevSign = new io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.
                AutoDevSign(provision_profiles, entitlements);
        IosPlatform iosPlatform = new IosPlatform(autoDevSign);
        iosPlatform.setFusionSetId(iosFusionSet);
        iosPlatform.setAppPath(this.ipaApp3Path);
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, iosPlatform, null);

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
