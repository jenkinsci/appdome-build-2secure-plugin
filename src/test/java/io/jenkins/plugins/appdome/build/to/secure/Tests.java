package io.jenkins.plugins.appdome.build.to.secure;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.Secret;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.AndroidPlatform;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.AutoDevSign;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.AutoGoogleSign;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.AutoSign;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.PrivateSign;
import io.jenkins.plugins.appdome.build.to.secure.platform.ios.IosPlatform;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

public class Tests {

    public static void testAndroidAutoSignBuild(JenkinsRule jenkins, String token, String teamId, String appPath, String fusionSet, String keystoreFilePath,
                                                String keystorePassword, String keystoreAlias, String keystoreKeyPass,
                                                String fingerprint, StringWarp secondOutput, BuildToTest buildToTest,
                                                Boolean buildWithLogs, Logger logger) throws Exception {
        logger.info("Inside testAndroidAutoSignBuild");
        FreeStyleProject project = jenkins.createFreeStyleProject();
        // Create configuration objects
        AutoGoogleSign autoGoogleSign = null;
        if (fingerprint != null) {
            autoGoogleSign = new AutoGoogleSign(fingerprint);
        }
        Boolean isSecondOutput = false;
        if (secondOutput != null) {
            isSecondOutput = true;
        }

        AutoSign autoSign =
                new AutoSign(keystoreFilePath,
                        Secret.fromString(keystorePassword), Secret.fromString(keystoreAlias),
                        Secret.fromString(keystoreKeyPass), autoGoogleSign);

        AndroidPlatform androidPlatform = new AndroidPlatform(autoSign);
        androidPlatform.setFusionSetId(fusionSet);
        androidPlatform.setAppPath(appPath);


        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId,
                androidPlatform, secondOutput);

        appdomeBuilder.setBuildToTest(buildToTest);
        appdomeBuilder.setBuildWithLogs(buildWithLogs);

        project.getBuildersList().add(appdomeBuilder);
        checkingResults(project, isSecondOutput,jenkins);
    }

    public static void testAndroidPrivateSignBuild(JenkinsRule jenkins, String token, String teamId, String appPath, String fusionSet, String fingerprint,
                                                   StringWarp secondOutput, BuildToTest buildToTest, Boolean buildWithLogs,
                                                   Boolean googleSigning, Logger logger) throws Exception {
        logger.info("Inside testAndroidPrivateSignBuild");
        FreeStyleProject project = jenkins.createFreeStyleProject();
        Boolean isSecondOutput = false;
        if (secondOutput != null) {
            isSecondOutput = true;
        }
        // Create configuration objects
        PrivateSign privateSign = new PrivateSign(fingerprint);
        privateSign.setGoogleSigning(googleSigning);
        AndroidPlatform androidPlatform = new AndroidPlatform(privateSign);
        androidPlatform.setFusionSetId(fusionSet);
        androidPlatform.setAppPath(appPath);
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, androidPlatform, secondOutput);
        appdomeBuilder.setBuildWithLogs(buildWithLogs);
        project.getBuildersList().add(appdomeBuilder);
        checkingResults(project, isSecondOutput, jenkins);
    }

    public static void testAndroidAutoDevSignBuild(JenkinsRule jenkins, String token, String teamId, String appPath, String fusionSet, String fingerprint,
                                                   StringWarp secondOutput, BuildToTest buildToTest, Boolean buildWithLogs,
                                                   Boolean googleSigning, Logger logger) throws Exception {
        logger.info("Inside testAndroidAutoDevSignBuild");
        FreeStyleProject project = jenkins.createFreeStyleProject();
        // Create configuration objects
        Boolean isSecondOutput = false;
        if (secondOutput != null) {
            isSecondOutput = true;
        }
        AutoDevSign autoDevSign = new AutoDevSign(fingerprint);
        autoDevSign.setGoogleSigning(googleSigning);
        AndroidPlatform androidPlatform = new AndroidPlatform(autoDevSign);
        androidPlatform.setFusionSetId(fusionSet);
        androidPlatform.setAppPath(appPath);
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, androidPlatform, secondOutput);
        appdomeBuilder.setBuildToTest(buildToTest);
        appdomeBuilder.setBuildWithLogs(buildWithLogs);

        project.getBuildersList().add(appdomeBuilder);
        checkingResults(project, isSecondOutput, jenkins);
    }


    public static void testIosAutoSignBuild(JenkinsRule jenkins, String token, String teamId, String appPath, String fusionSet,
                                     String certificateFilePath, String certificatePassword, List<StringWarp>
                                             provisionProfiles, List<StringWarp> entitlements, BuildToTest buildToTest,
                                     Boolean buildWithLogs, Logger logger) throws Exception {
        logger.info("Inside testIosAutoSignBuild");

        FreeStyleProject project = jenkins.createFreeStyleProject();
        // Create configuration objects
        io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.AutoSign autoSign
                = new io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.
                AutoSign(certificateFilePath, Secret.fromString(certificatePassword), provisionProfiles, entitlements);

        IosPlatform iosPlatform = new IosPlatform(autoSign);
        iosPlatform.setFusionSetId(fusionSet);
        iosPlatform.setAppPath(appPath);
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, iosPlatform, null);
        appdomeBuilder.setBuildToTest(buildToTest);
        appdomeBuilder.setBuildWithLogs(buildWithLogs);

        project.getBuildersList().add(appdomeBuilder);
        checkingResults(project, false, jenkins);
    }


    public static void testIosPrivateSignBuild(JenkinsRule jenkins, String token, String teamId, String appPath, String fusionSet,
                                        List<StringWarp> provisionProfiles, BuildToTest buildToTest,
                                        Boolean buildWithLogs, Logger logger) throws Exception {
        logger.info("Inside testIosPrivateSignBuild");

        FreeStyleProject project = jenkins.createFreeStyleProject();

        // Create configuration objects
        io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.PrivateSign privateSign
                = new io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.
                PrivateSign(provisionProfiles);
        IosPlatform iosPlatform = new IosPlatform(privateSign);
        iosPlatform.setFusionSetId(fusionSet);
        iosPlatform.setAppPath(appPath);
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, iosPlatform, null);
        appdomeBuilder.setBuildToTest(buildToTest);
        appdomeBuilder.setBuildWithLogs(buildWithLogs);

        project.getBuildersList().add(appdomeBuilder);
        checkingResults(project, false, jenkins);

    }


    public static void testIosAutoDevPrivateSignBuild(JenkinsRule jenkins, String token, String teamId, String appPath, String fusionSet,
                                                      List<StringWarp> provisionProfiles, List<StringWarp> entitlements,
                                                      BuildToTest buildToTest, Boolean buildWithLogs, Logger logger) throws Exception {
        logger.info("Inside testIosAutoDevPrivateSignBuild");
        FreeStyleProject project = jenkins.createFreeStyleProject();

        // Create configuration objects
        io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.AutoDevSign
                autoDevSign = new io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.
                AutoDevSign(provisionProfiles, entitlements);
        IosPlatform iosPlatform = new IosPlatform(autoDevSign);
        iosPlatform.setFusionSetId(fusionSet);
        iosPlatform.setAppPath(appPath);
        AppdomeBuilder appdomeBuilder = new AppdomeBuilder(Secret.fromString(token), teamId, iosPlatform, null);
        appdomeBuilder.setBuildToTest(buildToTest);
        appdomeBuilder.setBuildWithLogs(buildWithLogs);
        project.getBuildersList().add(appdomeBuilder);
        checkingResults(project, false, jenkins);

    }


    private static void checkingResults(FreeStyleProject project, boolean isSecondOutput, JenkinsRule jenkins) throws Exception {
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        String consoleOutput = build.getLog();
        FilePath workspace = build.getWorkspace();

        // Check that the file exists in the workspace
        FilePath output_location = workspace.child("output");
        assertTrue("output_location should exist", output_location.exists());
        if (isSecondOutput) {
            jenkins.assertLogContains("Download Second Output", build);
        }
        System.out.println("build console output = " + consoleOutput);
        System.out.println("build status = " + build.getResult().toString());
        jenkins.assertBuildStatus(Result.SUCCESS, build); // Check build status
    }

}
