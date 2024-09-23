package io.jenkins.plugins.appdome.build.to.secure;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.*;
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

import java.io.*;
import java.net.URL;
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

    private boolean isAutoDevPrivateSign = false;

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
        return this.outputLocation;
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
        listener.getLogger().println("Appdome Build2Secure " + APPDOME_BUILDE2SECURE_VERSION);
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

    private int ExecuteAppdomeApi(TaskListener listener, FilePath appdomeWorkspace, FilePath agentWorkspace, EnvVars env, Launcher launcher) throws Exception {
        FilePath scriptPath = appdomeWorkspace.child("appdome-api-bash");
        String command = ComposeAppdomeCommand(appdomeWorkspace, agentWorkspace, env, launcher, listener);
        List<String> filteredCommandList = Stream.of(command.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                .filter(s -> !s.isEmpty()).map(s -> s.replaceAll("\"", ""))
                .collect(Collectors.toList());
        // Add the APPDOME_CLIENT_HEADER environment variable to the subprocess
        env.put(APPDOME_HEADER_ENV_NAME, APPDOME_BUILDE2SECURE_VERSION);
        String debugMode = env.get("ACTIONS_STEP_DEBUG");
        if ("true".equalsIgnoreCase(debugMode)) {
            listener.getLogger().println("[debug] command : " + command);
        }
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

    private String ComposeAppdomeCommand(FilePath appdomeWorkspace, FilePath agentWorkspace, EnvVars env, Launcher launcher, TaskListener listener) throws Exception {
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
                ComposeAndroidCommand(command, env, appdomeWorkspace, launcher, listener);
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
                    .append("\"" + appPath + "\"");
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
            setOutputLocation(checkExtension(this.outputLocation, basename, this.isAutoDevPrivateSign, false));

            command.append(OUTPUT_FLAG)
                    .append(getOutputLocation());
            command.append(CERTIFIED_SECURE_FLAG)
                    .append(getOutputLocation().substring(0, this.outputLocation.lastIndexOf("/") + 1))
                    .append("Certified_Secure.pdf");
            command.append(DEOBFUSCATION_OUTPUT)
                    .append(getOutputLocation().substring(0, this.outputLocation.lastIndexOf("/") + 1))
                    .append("Deobfuscation_Mapping_Files.zip");

        } else {


            output_location = agentWorkspace.child("output");
            output_location.mkdirs();

            setOutputLocation(checkExtension(String.valueOf(output_location + "/"), "Appdome_Protected_" + basename, this.isAutoDevPrivateSign, false));


            command.append(OUTPUT_FLAG)
                    .append(getOutputLocation());

            command.append(CERTIFIED_SECURE_FLAG)
                    .append(output_location.getRemote())
                    .append(File.separator)
                    .append("Certified_Secure.pdf");

            command.append(DEOBFUSCATION_OUTPUT)
                    .append(output_location.getRemote())
                    .append(File.separator)
                    .append("Deobfuscation_Mapping_Files.zip");
        }

        if (!(Util.fixEmptyAndTrim(this.getSecondOutput()) == null)) {
            String secondOutputVar = this.getSecondOutput();
            secondOutputVar = checkExtension(secondOutputVar, new File(secondOutputVar).getName(), false, true);
            command.append(SECOND_OUTPUT).append(secondOutputVar);

        }

        return command.toString();
    }

    private String checkExtension(String outputLocation, String basename, Boolean isThisAutoDevPrivate, Boolean isThisSecondOutput) {
        int dotIndex = basename.lastIndexOf('.');

        // Extract the extension from basename, if present
        String extensionFromBaseName = (dotIndex != -1) ? basename.substring(dotIndex + 1) : "";

        // Extract the basename without the extension
        String basenameWithoutExtension = (dotIndex != -1) ? basename.substring(0, dotIndex) : basename;

        String extension = extensionFromBaseName;
        String outputName = "";


        // Check if outputLocation ends with a known extension and if so, remove that extension from outputLocation
        if (outputLocation.endsWith(".ipa") || outputLocation.endsWith(".aab") || outputLocation.endsWith(".apk") || outputLocation.endsWith(".sh")) {
            dotIndex = outputLocation.lastIndexOf('.');
            outputLocation = outputLocation.substring(0, dotIndex); // Remove the extension from outputLocation
        } else if (!outputLocation.endsWith("/")) {
            outputName = new File(outputLocation).getName().toString();
            outputLocation = new File(outputLocation).getParent().toString();
        }

        // Overwrite the extension based on the provided booleans
        if (isThisSecondOutput) {
            extension = "apk";
        } else if (isThisAutoDevPrivate) {
            extension = "sh";
        }

        // Construct the final output location string
        String finalOutputLocation;
        if (outputLocation.endsWith("/")) {
            finalOutputLocation = outputLocation + basenameWithoutExtension + "." + extension;
        } else {
            if (!outputName.isEmpty()) {
                finalOutputLocation = outputLocation + "/" + outputName + "/" + basenameWithoutExtension + "." + extension;
            } else {
                finalOutputLocation = outputLocation + "." + extension;

            }
        }

        return finalOutputLocation;
    }


    private String UseEnvironmentVariable(EnvVars env, String envName, String fieldValue, String filedName) {
        if (fieldValue == null || fieldValue.isEmpty() && (env.get(envName) != null && !(Util.fixEmptyAndTrim(env.get(envName)) == null))) {
            return env.get(envName, fieldValue);
        }
        if (filedName.equals("entitlements")) {
            //Do nothing
            return null;
        }
        throw new InputMismatchException("The field '" + filedName + "' was not provided correctly. " +
                "Kindly ensure that the environment variable '" + envName + "' has been correctly inserted.");
    }


    private void ComposeIosCommand(StringBuilder command, EnvVars env, FilePath appdomeWorkspace, Launcher launcher) throws Exception {
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
                isAutoDevPrivateSign = true;
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
        cleanCommand(command);
    }

    /**
     * Cleans the provided command represented by a StringBuilder by removing any flags immediately followed by "NULL".
     * This method processes the command by checking each segment, and selectively modifying the original StringBuilder
     * to exclude the unwanted flags and "NULL" values.
     *
     * @param command The StringBuilder containing the command string to be cleaned directly.
     */
    public static void cleanCommand(StringBuilder command) {
        String[] parts = command.toString().split(" ");
        command.setLength(0); // Clear the original StringBuilder

        for (int i = 0; i < parts.length; i++) {
            if (i < parts.length - 1 && parts[i + 1].equals("NULL")) {
                i++; // Skip the flag and the "NULL"
            } else {
                command.append(parts[i]).append(" ");
            }
        }

        if (command.length() > 0) { // Remove the trailing space if present
            command.setLength(command.length() - 1);
        }
    }

    private void ComposeAndroidCommand(StringBuilder command, EnvVars env, FilePath appdomeWorkspace, Launcher launcher, TaskListener listener) throws Exception {
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
                this.isAutoDevPrivateSign = true;
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

        if (androidPlatform.getIsCrashlytics()) {

            if (!androidPlatform.getFirebaseAppId().isEmpty() &&
                    !androidPlatform.getGoogleCredFile().isEmpty()) {

                listener.getLogger().println("The Firebase app id inserted: " + androidPlatform.getFirebaseAppId());
                try {
                    installFirebaseCLI(env, appdomeWorkspace, launcher, listener);
                } catch (Exception e) {
                    listener.getLogger().println("Failed to install Firebase CLI binary: " + e);
                    listener.getLogger().println("continue without it.");
                }
                listener.getLogger().println("Firebase CLI installed successfully");
                command.append(FIREBASE_APP_ID).append(androidPlatform.getFirebaseAppId());
                String google_Cred_file = DownloadFilesOrContinue(androidPlatform.getGoogleCredFile(),
                        appdomeWorkspace, launcher);
                if (google_Cred_file.isEmpty()) {
                    listener.getLogger().println("Could not download or find the google Credentials json file. will continue without it.");
                } else {
                    env.put("GOOGLE_APPLICATION_CREDENTIALS", google_Cred_file);
                }
            }
        }


    }

    private void installFirebaseCLI(EnvVars env, FilePath workspace, Launcher launcher, TaskListener listener) throws Exception {
        listener.getLogger().println("Installing Firebase CLI...");
        boolean isUnix = launcher.isUnix();
        String firebaseBinaryName = isUnix ? "firebase" : "firebase.exe";
        FilePath firebaseBinary = workspace.child(firebaseBinaryName);

        if (!firebaseBinary.exists()) {
            String downloadUrl = "https://firebase.tools/bin/win/latest";
            if (isUnix) {
                downloadUrl = System.getProperty("os.name").toLowerCase().contains("linux")
                        ? "https://firebase.tools/bin/linux/latest"
                        : "https://firebase.tools/bin/macos/latest";
            }

            listener.getLogger().println("Downloading Firebase CLI from " + downloadUrl);

            try (InputStream in = new URL(downloadUrl).openStream(); OutputStream out = firebaseBinary.write()) {
                IOUtils.copy(in, out);
                listener.getLogger().println("Firebase CLI downloaded successfully.");
            } catch (IOException e) {
                throw new Exception("Failed to download Firebase CLI binary.", e);
            }

            if (isUnix) {
                firebaseBinary.chmod(0755);
                listener.getLogger().println("Execute permissions set for Firebase CLI.");
            }
        } else {
            listener.getLogger().println("Firebase CLI already exists in workspace.");
        }

        String pathDelimiter = isUnix ? ":" : ";";

        // Check if PATH is null and handle it
        String currentPath = env.get("PATH");
        if (currentPath == null) {
            currentPath = "";  // If PATH is null, initialize it to an empty string
        }

        String newPath = currentPath + pathDelimiter + firebaseBinary.getParent().getRemote();
        env.put("PATH", newPath);
        listener.getLogger().println("PATH updated with Firebase CLI directory.");

        verifyFirebaseCLI(env, workspace, launcher, listener, firebaseBinary);
    }


    private void verifyFirebaseCLI(EnvVars env, FilePath workspace, Launcher launcher, TaskListener listener, FilePath firebaseBinary) throws Exception {
        try {
            int result = launcher.launch()
                    .cmds(firebaseBinary.getRemote(), "--version")
                    .envs(env)
                    .stdout(listener.getLogger())
                    .stderr(listener.getLogger())
                    .quiet(true)
                    .pwd(workspace)
                    .join();
            if (result != 0) {
                throw new Exception("Firebase CLI verification failed.");
            }
            listener.getLogger().println("Firebase CLI verified successfully.");
        } catch (IOException | InterruptedException e) {
            throw new Exception("Error verifying Firebase CLI installation.", e);
        }
    }


    public static boolean isHttpUrl(String urlString) {
        String regex = "^https?://.*$";
        return urlString.matches(regex);
    }

    private static String DownloadFilesOrContinue(String paths, FilePath agentWorkspace, Launcher launcher) throws Exception {
        if (paths == null) {
            return "NULL";
        }
        ArgumentListBuilder args;
        FilePath userFilesPath;
        StringBuilder pathsToFilesOnAgent = new StringBuilder();
        String[] splitPathFiles = paths.split(",");

        for (String singlePath : splitPathFiles) {
            if (!isHttpUrl(singlePath)) {
                pathsToFilesOnAgent.append(singlePath).append(',');
            } else {

                try {
                    userFilesPath = agentWorkspace.child("user_files");
                    userFilesPath.mkdirs();
                    pathsToFilesOnAgent.append(DownloadFiles(userFilesPath, launcher, singlePath)).append(',');

                } catch (IOException | InterruptedException e) {
                    // Handle exceptions
                    throw new RuntimeException("Could not create or process files in the 'user_files' folder", e);
                }
            }
        }
        return pathsToFilesOnAgent.substring(0, pathsToFilesOnAgent.length() - 1).trim();
    }

    private static String DownloadFiles(FilePath userFilesPath, Launcher launcher, String url) throws IOException, InterruptedException {
        String fileName = getFileNameFromUrl(url);
        FilePath outputPath = userFilesPath.child(fileName);
        if (!userFilesPath.exists()) {
            userFilesPath.mkdirs();
        }
        System.out.println("Output Path: " + outputPath.getRemote());
        ArgumentListBuilder args = new ArgumentListBuilder("curl", "-LO", url);
        launcher.launch()
                .cmds(args)
                .pwd(userFilesPath)
                .quiet(true)
                .join();


        return outputPath.getRemote();
    }

    private static String getFileNameFromUrl(String url) {
        String decodedUrl = url.split("\\?")[0];
        int lastSlashIndex = decodedUrl.lastIndexOf('/');
        return decodedUrl.substring(lastSlashIndex + 1);
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
