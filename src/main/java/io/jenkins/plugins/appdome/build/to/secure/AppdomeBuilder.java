package io.jenkins.plugins.appdome.build.to.secure;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.appdome.build.to.secure.platform.Platform;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.AndroidPlatform;
import io.jenkins.plugins.appdome.build.to.secure.platform.ios.IosPlatform;
import io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.AutoDevSign;
import io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.AutoSign;
import io.jenkins.plugins.appdome.build.to.secure.platform.ios.certificate.method.PrivateSign;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.File;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jenkins.plugins.appdome.build.to.secure.AppdomeBuilderConstants.*;

public class AppdomeBuilder extends Builder implements SimpleBuildStep {

    private final Secret token;
    private final String teamId;
    private final Platform platform;
    private String outputLocation;
    private StringWarp secondOutput;
    private Boolean buildWithLogs;
    private BuildToTest buildToTest;

    @DataBoundConstructor
    public AppdomeBuilder(Secret token, String teamId, Platform platform, StringWarp secondOutput) {
        this.teamId = teamId;
        this.token = token;
        this.platform = platform;
        this.secondOutput = secondOutput;

    }

    @DataBoundSetter
    public void setBuildToTest(BuildToTest buildToTest) {
        this.buildToTest = buildToTest;
    }

    public BuildToTest getBuildToTest() {
        return buildToTest;
    }

    public String getSelectedVendor() {
        if (this.buildToTest != null) {
            return buildToTest.getSelectedVendor();
        }
        return null;
    }

    public Secret getToken() {
        return token;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getOutputLocation() {
        return outputLocation;
    }

    public Boolean getBuildWithLogs() {
        return buildWithLogs;
    }

    @DataBoundSetter
    public void setBuildWithLogs(Boolean buildWithLogs) {
        this.buildWithLogs = buildWithLogs;
    }

    @DataBoundSetter
    public void setOutputLocation(String outputLocation) {
        this.outputLocation = outputLocation;
    }

    public void perform(@NonNull Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        int exitCode;
        FilePath appdomeWorkspace = workspace.createTempDir("AppdomeBuild", "Build");
        exitCode = CloneAppdomeApi(listener, appdomeWorkspace, launcher);
        if (exitCode == 0) {
            listener
                    .getLogger()
                    .println("Appdome engine updated successfully");
            try {

                exitCode = ExecuteAppdomeApi(listener, appdomeWorkspace, workspace, env, launcher);
            } catch (Exception e) {
                listener.error("Couldn't run Appdome Builder, read logs for more information. error:" + e);
                run.setResult(Result.FAILURE);
                deleteAppdomeWorkspacce(listener, appdomeWorkspace);
            }
            if (exitCode == 0) {
                listener
                        .getLogger()
                        .println("Executed Build successfully");
            } else {

                listener.error("Couldn't run Appdome Builder, exitcode " + exitCode + ".\nCouldn't run Appdome Builder, read logs for more information.");
                run.setResult(Result.FAILURE);
                deleteAppdomeWorkspacce(listener, appdomeWorkspace);
            }
        } else {
            listener.error("Couldn't Update Appdome engine, read logs for more information.");
            run.setResult(Result.FAILURE);
            deleteAppdomeWorkspacce(listener, appdomeWorkspace);
        }
        deleteAppdomeWorkspacce(listener, appdomeWorkspace);
    }

    private int ExecuteAppdomeApi(TaskListener listener, FilePath appdomeWorkspace, FilePath agentWorkspace, EnvVars env, Launcher launcher) throws IOException, InterruptedException {
        FilePath scriptPath = appdomeWorkspace.child("appdome-api-bash");
        String command = ComposeAppdomeCommand(appdomeWorkspace, agentWorkspace, env, launcher, listener);

        List<String> filteredCommandList = Stream.of(command.split(" "))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        // Add the APPDOME_CLIENT_HEADER environment variable to the subprocess
        env.put(APPDOME_HEADER_ENV_NAME, APPDOME_BUILDE2SECURE_VERSION);

        listener.getLogger().println("Launching Appdome engine");
        return launcher.launch()
                .cmds(filteredCommandList)
                .pwd(scriptPath)
                .envs(env)
                .stdout(listener.getLogger())
                .stderr(listener.getLogger())
                .quiet(true)
                .join();
    }

    private String ComposeAppdomeCommand(FilePath appdomeWorkspace, FilePath agentWorkspace, EnvVars env, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        //common:
        StringBuilder command = new StringBuilder("./appdome_api.sh");
        command.append(KEY_FLAG)
                .append(this.token).
                append(FUSION_SET_ID_FLAG)
                .append(platform.getFusionSetId());

        //concatenate the team id if it is not empty:
        if (!(Util.fixEmptyAndTrim(this.teamId) == null)) {
            command.append(TEAM_ID_FLAG)
                    .append(this.teamId);
        }

        String appPath = "";
        //concatenate the app path if it is not empty:
        if (!(Util.fixEmptyAndTrim(this.platform.getAppPath()) == null)) {
            appPath = DownloadFilesOrContinue(this.platform.getAppPath(), appdomeWorkspace, launcher);
        } else {
            appPath = DownloadFilesOrContinue(UseEnvironmentVariable(env, APP_PATH,
                    appPath, APP_FLAG.trim().substring(2)), appdomeWorkspace, launcher);
        }

        switch (platform.getPlatformType()) {
            case ANDROID:
                ComposeAndroidCommand(command, env, appdomeWorkspace, launcher);
                break;
            case IOS:
                ComposeIosCommand(command, env, appdomeWorkspace, launcher);
                break;
            default:
                return null;
        }

        if (appPath.isEmpty()) {
            throw new RuntimeException("App path was not provided.");
        } else {
            command.append(APP_FLAG)
                    .append(appPath);
        }

        if (this.buildWithLogs != null && this.buildWithLogs) {
            command.append(BUILD_WITH_LOGS);
        }

        if (this.buildToTest != null) {
            command.append(BUILD_TO_TEST)
                    .append(this.buildToTest.getSelectedVendor());
        }

        String basename = new File(appPath).getName();
        ArgumentListBuilder args;
        FilePath output_location;
        if (!(Util.fixEmptyAndTrim(this.outputLocation) == null)) {
            command.append(OUTPUT_FLAG)
                    .append(this.outputLocation);
            command.append(CERTIFIED_SECURE_FLAG)
                    .append(this.outputLocation.substring(0, this.outputLocation.lastIndexOf("/") + 1))
                    .append("Certified_Secure.pdf");

        } else {
            args = new ArgumentListBuilder("mkdir", "output");
            launcher.launch()
                    .cmds(args)
                    .pwd(agentWorkspace)
                    .quiet(true)
                    .join();

            output_location = agentWorkspace.child("output");
            command.append(OUTPUT_FLAG)
                    .append(output_location.getRemote())
                    .append(File.separator)
                    .append("Appdome_Protected_")
                    .append(basename);

            command.append(CERTIFIED_SECURE_FLAG)
                    .append(output_location.getRemote())
                    .append(File.separator)
                    .append("Certified_Secure.pdf");
        }

        if (!(Util.fixEmptyAndTrim(this.getSecondOutput()) == null)) {
            command.append(SECOND_OUTPUT).append(this.getSecondOutput());
        }

        return command.toString();
    }


    private String UseEnvironmentVariable(EnvVars env, String envName, String fieldValue, String filedName) {
        if (fieldValue == null || fieldValue.isEmpty() && (env.get(envName) != null && !(Util.fixEmptyAndTrim(env.get(envName)) == null))) {
            return env.get(envName, fieldValue);
        }
        throw new InputMismatchException("The field '" + filedName + "' was not provided correctly. " +
                "Kindly ensure that the environment variable '" + envName + "' has been correctly inserted.");
    }


    private void ComposeIosCommand(StringBuilder command, EnvVars env, FilePath appdomeWorkspace, Launcher launcher) throws IOException, InterruptedException {
        IosPlatform iosPlatform = ((IosPlatform) platform);


        switch (iosPlatform.getCertificateMethod().getSignType()) {
            case AUTO:
                AutoSign autoSign = (AutoSign) iosPlatform.getCertificateMethod();
                command.append(SIGN_ON_APPDOME_FLAG)
                        .append(KEYSTORE_FLAG)
                        .append(autoSign.getKeystorePath() == null
                                || autoSign.getKeystorePath().isEmpty()
                                ? DownloadFilesOrContinue(UseEnvironmentVariable(env, KEYSTORE_PATH_ENV, autoSign.getKeystorePath(),
                                KEYSTORE_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(autoSign.getKeystorePath(), appdomeWorkspace, launcher))
                        .append(KEYSTORE_PASS_FLAG)
                        .append(autoSign.getKeystorePassword())
                        .append(PROVISION_PROFILES_FLAG)
                        .append(autoSign.getProvisioningProfilesPath() == null
                                || autoSign.getProvisioningProfilesPath().isEmpty()
                                ? DownloadFilesOrContinue(UseEnvironmentVariable(env, MOBILE_PROVISION_PROFILE_PATHS_ENV,
                                autoSign.getProvisioningProfilesPath(),
                                PROVISION_PROFILES_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(autoSign.getProvisioningProfilesPath(), appdomeWorkspace, launcher))
                        .append(ENTITLEMENTS_FLAG)
                        .append(autoSign.getEntitlementsPath() == null
                                || autoSign.getEntitlementsPath().isEmpty()
                                ? DownloadFilesOrContinue(UseEnvironmentVariable(env, ENTITLEMENTS_PATHS_ENV, autoSign.getEntitlementsPath(),
                                ENTITLEMENTS_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(autoSign.getEntitlementsPath(), appdomeWorkspace, launcher));
                break;
            case PRIVATE:
                PrivateSign privateSign = (PrivateSign) iosPlatform.getCertificateMethod();
                command.append(PRIVATE_SIGN_FLAG)
                        .append(PROVISION_PROFILES_FLAG)
                        .append(privateSign.getProvisioningProfilesPath() == null
                                || privateSign.getProvisioningProfilesPath().isEmpty()
                                ? DownloadFilesOrContinue(UseEnvironmentVariable(env, MOBILE_PROVISION_PROFILE_PATHS_ENV,
                                privateSign.getProvisioningProfilesPath(),
                                PROVISION_PROFILES_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(privateSign.getProvisioningProfilesPath(), appdomeWorkspace, launcher));
                break;
            case AUTODEV:
                AutoDevSign autoDevSign = (AutoDevSign) iosPlatform.getCertificateMethod();
                command.append(AUTO_DEV_PRIVATE_SIGN_FLAG)
                        .append(PROVISION_PROFILES_FLAG).append(autoDevSign.getProvisioningProfilesPath() == null
                                || autoDevSign.getProvisioningProfilesPath().isEmpty()
                                ? DownloadFilesOrContinue(UseEnvironmentVariable(env, MOBILE_PROVISION_PROFILE_PATHS_ENV,
                                autoDevSign.getProvisioningProfilesPath(),
                                PROVISION_PROFILES_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(autoDevSign.getProvisioningProfilesPath(), appdomeWorkspace, launcher))
                        .append(ENTITLEMENTS_FLAG).append(autoDevSign.getEntitlementsPath() == null
                                || autoDevSign.getEntitlementsPath().isEmpty()
                                ? DownloadFilesOrContinue(UseEnvironmentVariable(env, ENTITLEMENTS_PATHS_ENV, autoDevSign.getEntitlementsPath(),
                                ENTITLEMENTS_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(autoDevSign.getEntitlementsPath(), appdomeWorkspace, launcher));
                break;
            case NONE:
            default:
                break;
        }
    }

    private void ComposeAndroidCommand(StringBuilder command, EnvVars env, FilePath appdomeWorkspace, Launcher launcher) throws IOException, InterruptedException {
        AndroidPlatform androidPlatform = ((AndroidPlatform) platform);

        switch (androidPlatform.getCertificateMethod().getSignType()) {
            case AUTO:
                io.jenkins.plugins.appdome.build.to.secure.platform
                        .android.certificate.method.AutoSign autoSign =
                        (io.jenkins.plugins.appdome.build.to.secure.platform
                                .android.certificate.method.AutoSign)
                                androidPlatform.getCertificateMethod();

                command.append(SIGN_ON_APPDOME_FLAG)
                        .append(KEYSTORE_FLAG)
                        .append(autoSign.getKeystorePath() == null || autoSign.getKeystorePath().isEmpty()
                                ? DownloadFilesOrContinue(UseEnvironmentVariable(env, KEYSTORE_PATH_ENV, autoSign.getKeystorePath(),
                                KEYSTORE_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(autoSign.getKeystorePath(), appdomeWorkspace, launcher))
                        .append(KEYSTORE_PASS_FLAG)
                        .append(autoSign.getKeystorePassword())
                        .append(KEYSOTRE_ALIAS_FLAG)
                        .append(autoSign.getKeystoreAlias())
                        .append(KEY_PASS_FLAG)
                        .append(autoSign.getKeyPass());

                if (autoSign.getIsEnableGoogleSign()) {
                    command.append(GOOGLE_PLAY_SIGN_FLAG);
                    command.append(FINGERPRINT_FLAG)
                            .append(autoSign.getGoogleSignFingerPrint());
                }
                break;
            case PRIVATE:
                io.jenkins.plugins.appdome.build.to.secure.platform
                        .android.certificate.method.PrivateSign privateSign =
                        (io.jenkins.plugins.appdome.build.to.secure.platform
                                .android.certificate.method.PrivateSign)
                                androidPlatform.getCertificateMethod();
                command.append(PRIVATE_SIGN_FLAG)
                        .append(FINGERPRINT_FLAG)
                        .append(privateSign.getFingerprint());
                if (privateSign.getGoogleSigning()) {
                    command.append(GOOGLE_PLAY_SIGN_FLAG);
                }
                break;
            case AUTODEV:
                io.jenkins.plugins.appdome.build.to.secure.platform
                        .android.certificate.method.AutoDevSign autoDev =
                        (io.jenkins.plugins.appdome.build.to.secure.platform
                                .android.certificate.method.AutoDevSign)
                                androidPlatform.getCertificateMethod();
                command.append(AUTO_DEV_PRIVATE_SIGN_FLAG)
                        .append(FINGERPRINT_FLAG)
                        .append(autoDev.getFingerprint());
                if (autoDev.getGoogleSigning()) {
                    command.append(GOOGLE_PLAY_SIGN_FLAG);
                }
                break;
            case NONE:
            default:
                break;
        }
    }

    public static boolean isHttpUrl(String urlString) {
        String regex = "^https?://.*$";
        return urlString.matches(regex);
    }

    private static String DownloadFilesOrContinue(String paths, FilePath agentWorkspace, Launcher launcher) throws IOException, InterruptedException {
        ArgumentListBuilder args;
        FilePath userFilesPath;
        StringBuilder pathsToFilesOnAgent = new StringBuilder();
        String[] splitPathFiles = paths.split(",");

        for (String singlePath : splitPathFiles) {
            if (!isHttpUrl(singlePath)) {
                pathsToFilesOnAgent.append(singlePath).append(',');
            } else {
                args = new ArgumentListBuilder("mkdir", "user_files");
                launcher.launch()
                        .cmds(args)
                        .pwd(agentWorkspace)
                        .quiet(true)
                        .join();
                userFilesPath = agentWorkspace.child("user_files");
                pathsToFilesOnAgent.append(DownloadFiles(userFilesPath, launcher, singlePath)).append(',');
            }
        }
        return pathsToFilesOnAgent.substring(0, pathsToFilesOnAgent.length() - 1).trim();
    }

    private static String DownloadFiles(FilePath userFilesPath, Launcher launcher, String url) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder("curl", "-LO", url);
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        String outputPath = userFilesPath.getRemote() + File.separator + fileName;
        launcher.launch()
                .cmds(args)
                .pwd(userFilesPath)
                .quiet(true)
                .join();
        return outputPath;
    }

    /**
     * Clones the Appdome API repository.
     * https://github.com/Appdome/appdome-api-bash.git
     *
     * @param listener         the TaskListener to use for logging
     * @param appdomeWorkspace the working directory of the build
     * @param launcher         used to launch commands.
     * @return the exit code of the process
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the process is interrupted
     */
    private int CloneAppdomeApi(TaskListener listener, FilePath appdomeWorkspace, Launcher launcher) throws IOException, InterruptedException {
        listener
                .getLogger()
                .println("Updating Appdome Engine...");

        ArgumentListBuilder gitCloneCommand = new ArgumentListBuilder("git", "clone", "https://github.com/Appdome/appdome-api-bash.git");
        return launcher.launch()
                .cmds(gitCloneCommand)
                .pwd(appdomeWorkspace)
                .quiet(true)
                .join();
    }

    /**
     * This method deletes the contents and the workspace directory of an Appdome workspace.
     *
     * @param listener         listener object to log messages
     * @param appdomeWorkspace the path to the Appdome workspace to delete
     * @throws IOException          : if there is an error accessing the file system
     * @throws InterruptedException if the current thread is interrupted by another thread while
     *                              it is waiting for the workspace deletion to complete.
     */
    private static void deleteAppdomeWorkspacce(TaskListener listener, FilePath appdomeWorkspace) throws
            IOException, InterruptedException {
        listener
                .getLogger()
                .print("Deleting temporary files." + System.lineSeparator());
        appdomeWorkspace.deleteSuffixesRecursive();
        appdomeWorkspace.deleteContents();
        appdomeWorkspace.deleteRecursive();
    }


    public Platform getPlatform() {
        return platform;
    }

    public DescriptorExtensionList<Platform, Descriptor<Platform>> getPlatformDescriptors() {
        return Jenkins.get().getDescriptorList(Platform.class);
    }

    public String getSecondOutput() {
        if (secondOutput != null) {
            return secondOutput.getSecondOutput();
        }
        return null;
    }

    @DataBoundSetter
    public void setSecondOutput(StringWarp secondOutput) {
        this.secondOutput = secondOutput;
    }


    @Symbol("AppdomeBuilder")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @POST
        public FormValidation doCheckToken(@QueryParameter Secret token) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (token != null && Util.fixEmptyAndTrim(token.getPlainText()) == null) {
                return FormValidation.error("Token is required");
            } else if (token != null && token.getPlainText().contains(" ")) {
                return FormValidation.error("White spaces are not allowed in Token.");
            }
            // Perform any additional validation here
            return FormValidation.ok();
        }

        public ListBoxModel doFillSelectedVendorItems() {
            return VendorManager.getInstance().getVendors();
        }


        @POST
        public FormValidation doCheckTeamId(@QueryParameter String teamId) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (teamId != null && Util.fixEmptyAndTrim(teamId) == null) {
                return FormValidation.warning("Empty Team ID for personal workspace.");
            } else if (teamId != null && teamId.contains(" ")) {
                return FormValidation.error("White spaces are not allowed in Team ID.");
            }
            // Perform any additional validation here
            return FormValidation.ok("Working on TeamID: " + teamId);
        }


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Appdome Build-2secure";
        }

    }

}
