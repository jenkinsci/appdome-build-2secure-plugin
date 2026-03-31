package io.jenkins.plugins.appdome.build.to.secure;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.*;
import io.jenkins.plugins.appdome.build.to.secure.platform.Platform;
import io.jenkins.plugins.appdome.build.to.secure.platform.PlatformType;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.AndroidPlatform;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.TrustedSigningFingerprintsConfig;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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

    private StringWarp dynamicCertificate;

    private Boolean buildWithLogs;
    private Boolean workflowOutputLogs;
    private BuildToTest buildToTest;

    private boolean isAutoDevPrivateSign = false;

    @DataBoundConstructor
    public AppdomeBuilder(Secret token, String teamId, Platform platform, StringWarp secondOutput, StringWarp dynamicCertificate) {
        this.teamId = teamId;
        this.token = token;
        this.platform = platform;
        this.secondOutput = secondOutput;
        this.dynamicCertificate = dynamicCertificate;

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

    public Boolean getWorkflowOutputLogs() {
        return this.workflowOutputLogs;
    }

    @DataBoundSetter
    public void setWorkflowOutputLogs(Boolean workflowOutputLogs) {
        this.workflowOutputLogs = workflowOutputLogs;
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
                listener.getLogger().println("Executed Build successfully");
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
        ComposedCommandOutcome composed = composeAppdomeCommand(appdomeWorkspace, agentWorkspace, env, launcher, listener);
        List<String> filteredCommandList = Stream.of(composed.command.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                .filter(s -> !s.isEmpty()).map(s -> s.replaceAll("\"", ""))
                .collect(Collectors.toList());
        env.put(APPDOME_HEADER_ENV_NAME, APPDOME_BUILDE2SECURE_VERSION);
        logAppdomeConfiguration(listener, env, composed.resolvedAppPath);
        listener.getLogger().println("Launching Appdome engine");
        logAppdomeOutputArtifacts(
                listener,
                composed.resolvedOutputPath,
                composed.outputDirPrefix,
                composed.workflowLogsEnabled,
                composed.secondOutputLogged);
        return launcher.launch()
                .cmds(filteredCommandList)
                .pwd(scriptPath)
                .envs(env)
                .stdout(listener.getLogger())
                .stderr(listener.getLogger())
                .quiet(true)
                .join();
    }

    private static final class ComposedCommandOutcome {
        final String command;
        final String resolvedAppPath;
        final String resolvedOutputPath;
        final String outputDirPrefix;
        final boolean workflowLogsEnabled;
        final String secondOutputLogged;

        ComposedCommandOutcome(
                String command,
                String resolvedAppPath,
                String resolvedOutputPath,
                String outputDirPrefix,
                boolean workflowLogsEnabled,
                String secondOutputLogged) {
            this.command = command;
            this.resolvedAppPath = resolvedAppPath;
            this.resolvedOutputPath = resolvedOutputPath;
            this.outputDirPrefix = outputDirPrefix;
            this.workflowLogsEnabled = workflowLogsEnabled;
            this.secondOutputLogged = secondOutputLogged;
        }
    }

    private ComposedCommandOutcome composeAppdomeCommand(FilePath appdomeWorkspace, FilePath agentWorkspace, EnvVars env, Launcher launcher, TaskListener listener) throws Exception {
        //common:
        StringBuilder command = new StringBuilder("./appdome_api.sh");
        command.append(KEY_FLAG)
                .append(this.token)
                .append(FUSION_SET_ID_FLAG)
                .append(platform.getFusionSetId());

        //concatenate the team id if it is not empty:
        if (!(Util.fixEmptyAndTrim(this.teamId) == null)) {
            command.append(TEAM_ID_FLAG)
                    .append(this.teamId);
        }

        String appPath = "";
        //concatenate the app path if it is not empty:
        if (!(Util.fixEmptyAndTrim(this.platform.getAppPath()) == null)) {
            appPath = DownloadFilesOrContinue(env, this.platform.getAppPath(), appdomeWorkspace, launcher);
        } else {
            appPath = DownloadFilesOrContinue(env, UseEnvironmentVariable(env, APP_PATH,
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
                throw new IllegalStateException("Unsupported platform: " + platform.getPlatformType());
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
        FilePath output_location;

        final String resolvedOutputPath;
        final String outputDirPrefix;
        final boolean workflowLogsEnabled = this.workflowOutputLogs != null && this.workflowOutputLogs;
        String secondOutputLogged = null;

        if (!(Util.fixEmptyAndTrim(this.outputLocation) == null)) {
            String expandedOutputLocation = env.expand(this.outputLocation);
            if (Util.fixEmptyAndTrim(expandedOutputLocation) == null) {
                throw new InputMismatchException(
                        "Output location expanded to an empty value; check environment variables in the output path.");
            }
            resolvedOutputPath = checkExtension(expandedOutputLocation.trim(), basename, this.isAutoDevPrivateSign, false);

            command.append(OUTPUT_FLAG)
                    .append(resolvedOutputPath);
            outputDirPrefix = directoryPrefixWithFinalSeparator(resolvedOutputPath);
            command.append(CERTIFIED_SECURE_PDF_FLAG)
                    .append(outputDirPrefix)
                    .append("Certified_Secure.pdf");
            command.append(CERTIFIED_SECURE_JSON_FLAG)
                    .append(outputDirPrefix)
                    .append("Certified_Secure.json");
            command.append(DEOBFUSCATION_OUTPUT)
                    .append(outputDirPrefix)
                    .append("Deobfuscation_Mapping_Files.zip");
            if (workflowLogsEnabled) {
                command.append(WORKFLOW_OUTPUT_LOGS_FLAG)
                        .append(outputDirPrefix)
                        .append("workflow_output_logs.log");
            }

        } else {


            output_location = agentWorkspace.child("output");
            output_location.mkdirs();

            resolvedOutputPath = checkExtension(String.valueOf(output_location + "/"), "Appdome_Protected_" + basename, this.isAutoDevPrivateSign, false);


            command.append(OUTPUT_FLAG)
                    .append(resolvedOutputPath);

            outputDirPrefix = directoryPrefixWithFinalSeparator(resolvedOutputPath);

            command.append(CERTIFIED_SECURE_PDF_FLAG)
                    .append(output_location.getRemote())
                    .append(File.separator)
                    .append("Certified_Secure.pdf");

            command.append(CERTIFIED_SECURE_JSON_FLAG)
                    .append(output_location.getRemote())
                    .append(File.separator)
                    .append("Certified_Secure.json");

            command.append(DEOBFUSCATION_OUTPUT)
                    .append(output_location.getRemote())
                    .append(File.separator)
                    .append("Deobfuscation_Mapping_Files.zip");

            if (workflowLogsEnabled) {

                command.append(WORKFLOW_OUTPUT_LOGS_FLAG)
                        .append(output_location.getRemote())
                        .append(File.separator)
                        .append("workflow_output_logs.log");
            }
        }

        if (platform.getPlatformType() == PlatformType.ANDROID
                && !(Util.fixEmptyAndTrim(this.getSecondOutput()) == null)) {
            String secondOutputVar = env.expand(this.getSecondOutput());
            if (Util.fixEmptyAndTrim(secondOutputVar) == null) {
                throw new InputMismatchException(
                        "Second output expanded to an empty value; check environment variables in that path.");
            }
            secondOutputVar = secondOutputVar.trim();
            secondOutputVar = checkExtension(secondOutputVar, new File(secondOutputVar).getName(), false, true);
            secondOutputLogged = secondOutputVar;
            command.append(SECOND_OUTPUT).append(secondOutputVar);
        }


        if (!(Util.fixEmptyAndTrim(this.getDynamicCertificate()) == null)) {
            command.append(DYNAMIC_CERTIFICATE)
                    .append(DownloadFilesOrContinue(env, this.getDynamicCertificate(), appdomeWorkspace, launcher));
        }

        return new ComposedCommandOutcome(
                command.toString(), appPath, resolvedOutputPath, outputDirPrefix, workflowLogsEnabled, secondOutputLogged);
    }

    private void logAppdomeConfiguration(TaskListener listener, EnvVars env, String resolvedAppPath) {
        PrintStream log = listener.getLogger();
        log.println("--- Appdome Build2Secure configuration ---");
        log.println("Platform: " + platform.getPlatformType().name());
        String fusion = Util.fixEmptyAndTrim(platform.getFusionSetId());
        log.println("Fusion set ID: " + (fusion != null ? env.expand(fusion) : "(not set)"));
        String team = Util.fixEmptyAndTrim(this.teamId);
        log.println("Team ID: " + (team != null ? env.expand(team) : "(not set)"));
        log.println("Sign method: " + describeSignMethod());
        log.println("Application path (resolved on agent): " + resolvedAppPath);
        String outLoc = Util.fixEmptyAndTrim(this.outputLocation);
        log.println(
                "Output location (configured): "
                        + (outLoc != null ? env.expand(outLoc) : "(default: workspace/output/)"));
        log.println("Build with logs: " + (Boolean.TRUE.equals(this.buildWithLogs) ? "yes" : "no"));
        if (this.buildToTest != null) {
            log.println("Build to test vendor: " + this.buildToTest.getSelectedVendor());
        } else {
            log.println("Build to test: (not set)");
        }
        log.println("Workflow output logs: " + (Boolean.TRUE.equals(this.workflowOutputLogs) ? "yes" : "no"));
        String second = Util.fixEmptyAndTrim(this.getSecondOutput());
        log.println(
                "Second output (configured): "
                        + (second != null ? env.expand(second) : "(not set)"));
        log.println(
                "Dynamic certificate: "
                        + (Util.fixEmptyAndTrim(this.getDynamicCertificate()) != null ? "configured" : "(not set)"));
        if (platform instanceof AndroidPlatform) {
            logAndroidTrustedSigningFingerprints((AndroidPlatform) platform, listener.getLogger(), env);
        }
        log.println("---");
    }

    private void logAndroidTrustedSigningFingerprints(AndroidPlatform androidPlatform, PrintStream log, EnvVars env) {
        io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method.CertificateMethod cm =
                androidPlatform.getCertificateMethod();
        if (!(cm instanceof TrustedSigningFingerprintsConfig)) {
            return;
        }
        TrustedSigningFingerprintsConfig t = (TrustedSigningFingerprintsConfig) cm;
        if (Boolean.TRUE.equals(t.getTrustedSigningFingerprintsFile())) {
            String expanded = Util.fixEmptyAndTrim(
                    env.expand(t.getSigningFingerprintListPath() == null ? "" : t.getSigningFingerprintListPath()));
            log.println(
                    "Trusted signing fingerprints file: yes"
                            + (expanded != null ? " (" + expanded + ")" : " (path not set)"));
        } else {
            log.println("Trusted signing fingerprints file: no");
        }
    }

    private String describeSignMethod() {
        try {
            if (platform instanceof AndroidPlatform) {
                return ((AndroidPlatform) platform).getCertificateMethod().getSignType().name();
            }
            if (platform instanceof IosPlatform) {
                return ((IosPlatform) platform).getCertificateMethod().getSignType().name();
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        return "UNKNOWN";
    }

    private static void logAppdomeOutputArtifacts(
            TaskListener listener,
            String resolvedOutputPath,
            String outputDirPrefix,
            boolean workflowLogsEnabled,
            String secondOutputPath) {
        PrintStream log = listener.getLogger();
        log.println("Appdome output artifacts (expected paths on agent):");
        log.println("  Protected application: " + resolvedOutputPath);
        log.println("  Certified_Secure.pdf: " + outputDirPrefix + "Certified_Secure.pdf");
        log.println("  Certified_Secure.json: " + outputDirPrefix + "Certified_Secure.json");
        log.println("  Deobfuscation_Mapping_Files.zip: " + outputDirPrefix + "Deobfuscation_Mapping_Files.zip");
        if (workflowLogsEnabled) {
            log.println("  workflow_output_logs.log: " + outputDirPrefix + "workflow_output_logs.log");
        }
        if (!(Util.fixEmptyAndTrim(secondOutputPath) == null)) {
            log.println("  Second output: " + secondOutputPath);
        }
    }

    private static String directoryPrefixWithFinalSeparator(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int f = path.lastIndexOf('/');
        int b = path.lastIndexOf('\\');
        int i = Math.max(f, b);
        return i >= 0 ? path.substring(0, i + 1) : "";
    }

    /**
     * Extension the protected app artifact should use (.apk / .aab / .ipa from input app, .sh for AutoDev, .apk for second output).
     */
    private static String targetArtifactExtension(String basename, Boolean isThisAutoDevPrivate, Boolean isThisSecondOutput) {
        if (Boolean.TRUE.equals(isThisSecondOutput)) {
            return "apk";
        }
        if (Boolean.TRUE.equals(isThisAutoDevPrivate)) {
            return "sh";
        }
        int dot = basename.lastIndexOf('.');
        return (dot != -1) ? basename.substring(dot + 1) : "";
    }

    private String checkExtension(String outputLocation, String basename, Boolean isThisAutoDevPrivate, Boolean isThisSecondOutput) {
        final String path = outputLocation == null ? "" : outputLocation.trim();

        // Explicit output *file* (last path segment has a real extension): keep configured stem + directory;
        // normalize extension to match build output (.apk vs mistaken .zip, AutoDev -> .sh, etc.).
        boolean trailingSlash = path.endsWith("/") || path.endsWith(File.separator);
        if (!trailingSlash) {
            File outFileForKind = new File(path);
            String nameForKind = outFileForKind.getName().trim();
            int lastDot = nameForKind.lastIndexOf('.');
            if (lastDot > 0 && lastDot < nameForKind.length() - 1) {
                String userExt = nameForKind.substring(lastDot + 1).trim();
                if (!userExt.isEmpty()) {
                    String targetExt = targetArtifactExtension(basename, isThisAutoDevPrivate, isThisSecondOutput);
                    if (!targetExt.isEmpty() && !userExt.equalsIgnoreCase(targetExt)) {
                        String stem = nameForKind.substring(0, lastDot);
                        File parentFile = outFileForKind.getParentFile();
                        String normalizedName = stem + "." + targetExt;
                        return parentFile != null ? new File(parentFile, normalizedName).getPath() : normalizedName;
                    }
                    return path;
                }
            }
        }

        // App artifact: basename without extension for directory-style output naming
        int dotIndex = basename.lastIndexOf('.');
        String basenameWithoutExtension = (dotIndex != -1) ? basename.substring(0, dotIndex) : basename;

        String outputName = "";
        String workLocation;

        // Directory-style configured path: trailing slash, or parent + last segment as folder name
        if (trailingSlash) {
            workLocation = path;
        } else {
            File outFile = new File(path);
            outputName = outFile.getName().trim();
            String parent = outFile.getParent();
            workLocation = parent != null ? parent : "";
        }

        String dirSuffixExtension = targetArtifactExtension(basename, isThisAutoDevPrivate, isThisSecondOutput);

        // Under a directory: basename only when app has no extension; else basename.suffix
        String dirStyleFileName;
        if (dirSuffixExtension.isEmpty()) {
            dirStyleFileName = basenameWithoutExtension;
        } else {
            dirStyleFileName = basenameWithoutExtension + "." + dirSuffixExtension;
        }

        if (workLocation.endsWith("/")) {
            return workLocation + dirStyleFileName;
        }
        if (!outputName.isEmpty()) {
            return workLocation + "/" + outputName + "/" + dirStyleFileName;
        }
        return dirSuffixExtension.isEmpty() ? workLocation : workLocation + "." + dirSuffixExtension;
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
                                ? DownloadFilesOrContinue(env, UseEnvironmentVariable(env, KEYSTORE_PATH_ENV, autoSign.getKeystorePath(),
                                KEYSTORE_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(env, autoSign.getKeystorePath(), appdomeWorkspace, launcher))
                        .append(KEYSTORE_PASS_FLAG)
                        .append(autoSign.getKeystorePassword())
                        .append(PROVISION_PROFILES_FLAG)
                        .append(autoSign.getProvisioningProfilesPath() == null
                                || autoSign.getProvisioningProfilesPath().isEmpty()
                                ? DownloadFilesOrContinue(env, UseEnvironmentVariable(env, MOBILE_PROVISION_PROFILE_PATHS_ENV,
                                autoSign.getProvisioningProfilesPath(),
                                PROVISION_PROFILES_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(env, autoSign.getProvisioningProfilesPath(), appdomeWorkspace, launcher))
                        .append(ENTITLEMENTS_FLAG)
                        .append(autoSign.getEntitlementsPath() == null
                                || autoSign.getEntitlementsPath().isEmpty()
                                ? DownloadFilesOrContinue(env, UseEnvironmentVariable(env, ENTITLEMENTS_PATHS_ENV, autoSign.getEntitlementsPath(),
                                ENTITLEMENTS_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(env, autoSign.getEntitlementsPath(), appdomeWorkspace, launcher));
                break;
            case PRIVATE:
                PrivateSign privateSign = (PrivateSign) iosPlatform.getCertificateMethod();
                command.append(PRIVATE_SIGN_FLAG)
                        .append(PROVISION_PROFILES_FLAG)
                        .append(privateSign.getProvisioningProfilesPath() == null
                                || privateSign.getProvisioningProfilesPath().isEmpty()
                                ? DownloadFilesOrContinue(env, UseEnvironmentVariable(env, MOBILE_PROVISION_PROFILE_PATHS_ENV,
                                privateSign.getProvisioningProfilesPath(),
                                PROVISION_PROFILES_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(env, privateSign.getProvisioningProfilesPath(), appdomeWorkspace, launcher));
                break;
            case AUTODEV:
                isAutoDevPrivateSign = true;
                AutoDevSign autoDevSign = (AutoDevSign) iosPlatform.getCertificateMethod();
                command.append(AUTO_DEV_PRIVATE_SIGN_FLAG)
                        .append(PROVISION_PROFILES_FLAG).append(autoDevSign.getProvisioningProfilesPath() == null
                                || autoDevSign.getProvisioningProfilesPath().isEmpty()
                                ? DownloadFilesOrContinue(env, UseEnvironmentVariable(env, MOBILE_PROVISION_PROFILE_PATHS_ENV,
                                autoDevSign.getProvisioningProfilesPath(),
                                PROVISION_PROFILES_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(env, autoDevSign.getProvisioningProfilesPath(), appdomeWorkspace, launcher))
                        .append(ENTITLEMENTS_FLAG).append(autoDevSign.getEntitlementsPath() == null
                                || autoDevSign.getEntitlementsPath().isEmpty()
                                ? DownloadFilesOrContinue(env, UseEnvironmentVariable(env, ENTITLEMENTS_PATHS_ENV, autoDevSign.getEntitlementsPath(),
                                ENTITLEMENTS_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(env, autoDevSign.getEntitlementsPath(), appdomeWorkspace, launcher));
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

    private static boolean usesTrustedSigningFingerprintList(TrustedSigningFingerprintsConfig cfg) {
        return Boolean.TRUE.equals(cfg.getTrustedSigningFingerprintsFile());
    }

    private static String expandedTrustedSigningFingerprintListPath(EnvVars env, TrustedSigningFingerprintsConfig cfg) {
        return Util.fixEmptyAndTrim(
                env.expand(cfg.getSigningFingerprintListPath() == null ? "" : cfg.getSigningFingerprintListPath()));
    }

    private static void appendSigningFingerprintListIfPath(
            StringBuilder command,
            EnvVars env,
            String expandedListPath,
            FilePath appdomeWorkspace,
            Launcher launcher) throws Exception {
        if (expandedListPath != null) {
            command.append(SIGNING_FINGERPRINT_LIST_FLAG)
                    .append(DownloadFilesOrContinue(env, expandedListPath, appdomeWorkspace, launcher));
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
                                ? DownloadFilesOrContinue(env, UseEnvironmentVariable(env, KEYSTORE_PATH_ENV, autoSign.getKeystorePath(),
                                KEYSTORE_FLAG.trim().substring(2)), appdomeWorkspace, launcher)
                                : DownloadFilesOrContinue(env, autoSign.getKeystorePath(), appdomeWorkspace, launcher))
                        .append(KEYSTORE_PASS_FLAG)
                        .append(autoSign.getKeystorePassword())
                        .append(KEYSOTRE_ALIAS_FLAG)
                        .append(autoSign.getKeystoreAlias())
                        .append(KEY_PASS_FLAG)
                        .append(autoSign.getKeyPass());

                if (usesTrustedSigningFingerprintList(autoSign)) {
                    appendSigningFingerprintListIfPath(
                            command,
                            env,
                            expandedTrustedSigningFingerprintListPath(env, autoSign),
                            appdomeWorkspace,
                            launcher);
                } else if (autoSign.getIsEnableGoogleSign()) {
                    String signFingerPrint = autoSign.getGoogleSignFingerPrint();
                    command.append(GOOGLE_PLAY_SIGN_FLAG);
                    if (Util.fixEmptyAndTrim(signFingerPrint) != null) {
                        command.append(FINGERPRINT_FLAG)
                                .append(signFingerPrint);
                    }
                }
                break;
            case PRIVATE:
                io.jenkins.plugins.appdome.build.to.secure.platform
                        .android.certificate.method.PrivateSign privateSign =
                        (io.jenkins.plugins.appdome.build.to.secure.platform
                                .android.certificate.method.PrivateSign)
                                androidPlatform.getCertificateMethod();
                if (usesTrustedSigningFingerprintList(privateSign)) {
                    command.append(PRIVATE_SIGN_FLAG);
                    appendSigningFingerprintListIfPath(
                            command,
                            env,
                            expandedTrustedSigningFingerprintListPath(env, privateSign),
                            appdomeWorkspace,
                            launcher);
                } else {
                    command.append(PRIVATE_SIGN_FLAG)
                            .append(FINGERPRINT_FLAG)
                            .append(privateSign.getFingerprint());
                    if (privateSign.getGoogleSigning() != null ? privateSign.getGoogleSigning() : false) {
                        command.append(GOOGLE_PLAY_SIGN_FLAG);
                    }
                }
                break;
            case AUTODEV:
                this.isAutoDevPrivateSign = true;
                io.jenkins.plugins.appdome.build.to.secure.platform
                        .android.certificate.method.AutoDevSign autoDev =
                        (io.jenkins.plugins.appdome.build.to.secure.platform
                                .android.certificate.method.AutoDevSign)
                                androidPlatform.getCertificateMethod();
                if (usesTrustedSigningFingerprintList(autoDev)) {
                    command.append(AUTO_DEV_PRIVATE_SIGN_FLAG);
                    appendSigningFingerprintListIfPath(
                            command,
                            env,
                            expandedTrustedSigningFingerprintListPath(env, autoDev),
                            appdomeWorkspace,
                            launcher);
                } else {
                    command.append(AUTO_DEV_PRIVATE_SIGN_FLAG)
                            .append(FINGERPRINT_FLAG)
                            .append(autoDev.getFingerprint());
                    if (Boolean.TRUE.equals(autoDev.getGoogleSigning())) {
                        command.append(GOOGLE_PLAY_SIGN_FLAG);
                    }
                }
                break;
            case NONE:
            default:
                break;
        }

        if (androidPlatform.getIsCrashlytics()) {
            if (androidPlatform.getFirebaseAppId() != null && !androidPlatform.getFirebaseAppId().isEmpty()) {
                listener.getLogger().println("The Firebase app id inserted: " + androidPlatform.getFirebaseAppId());
                try {
                    installFirebaseCLI(env, appdomeWorkspace, launcher, listener);
                    listener.getLogger().println("Firebase CLI installed successfully");
                    command.append(FIREBASE_APP_ID).append(androidPlatform.getFirebaseAppId());
                } catch (Exception e) {
                    listener.getLogger().println("Failed to install Firebase CLI binary: " + e);
                    listener.getLogger().println("Continuing without it.");
                }
            } else {
                listener.getLogger().println("No Firebase App ID provided; upload to Firebase and Crashlytics will not proceed.");
            }
        }


        if (androidPlatform.getIsDatadog()) {
            if (androidPlatform.getDatadogKey() != null && !androidPlatform.getDatadogKey().isEmpty()) {
                listener.getLogger().println("The Datadog key inserted: " + androidPlatform.getDatadogKey());
                command.append(DATADOG_API_KEY).append(androidPlatform.getDatadogKey());
            }
        }
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Null value is expected and handled elsewhere")
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
        String newPath = env.get("PATH") + pathDelimiter + firebaseBinary.getParent().getRemote();
        env.put("PATH", newPath);
        listener.getLogger().println("PATH updated with Firebase CLI directory.");
    }

    public static boolean isHttpUrl(String urlString) {
        String regex = "^https?://.*$";
        return urlString.matches(regex);
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Null value is expected and handled elsewhere")
    private static String DownloadFilesOrContinue(EnvVars env, String paths, FilePath agentWorkspace, Launcher launcher) throws Exception {
        if (paths == null) {
            return "NULL";
        }
        ArgumentListBuilder args;
        FilePath userFilesPath;
        StringBuilder pathsToFilesOnAgent = new StringBuilder();
        String[] splitPathFiles = paths.split(",");

        for (String singlePath : splitPathFiles) {
            String resolvedPath = singlePath.trim();
            if (!isHttpUrl(resolvedPath)) {
                resolvedPath = env.expand(singlePath).trim();
            }
            if (!isHttpUrl(resolvedPath)) {
                pathsToFilesOnAgent.append(resolvedPath).append(',');
            } else {

                try {
                    userFilesPath = agentWorkspace.child("user_files");
                    userFilesPath.mkdirs();
                    pathsToFilesOnAgent.append(DownloadFiles(userFilesPath, launcher, resolvedPath)).append(',');

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

    /**
     * Second Output applies to Android only (e.g. Universal APK from AAB). Used by config UI visibility.
     */
    public boolean isSecondOutputSectionVisible() {
        return platform == null || platform.getPlatformType() == PlatformType.ANDROID;
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

    @DataBoundSetter
    public void setDynamicCertificate(StringWarp dynamicCertificate) {
        this.dynamicCertificate = dynamicCertificate;
    }

    public String getDynamicCertificate() {
        if (dynamicCertificate != null) {
            return dynamicCertificate.getDynamicCertificate();
        }
        return null;
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
