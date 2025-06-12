package io.jenkins.plugins.appdome.build.to.secure;

import java.io.File;

import io.jenkins.plugins.appdome.build.to.secure.platform.android.Crashlytics;
import io.jenkins.plugins.appdome.build.to.secure.platform.android.Datadog;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.jenkins.plugins.appdome.build.to.secure.Tests.PLUGIN_TMP_OUTPUT;
import static org.junit.Assert.fail;

public class PipelineTest {
    private final static Logger logger = Logger.getLogger(PipelineTest.class.getName());

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private String token;
    private String teamId;
    private String signOption;
    private String appFilePath;
    private String keystoreFilePath;
    private String keystoreAlias;
    private String keystoreKeyPass;
    private String keystorePassword;
    private String certificateFilePath;
    private String certificatePassword;
    private String fusionSetId;
    private String signFingerprint;

    private String firebaseAppId;
    private String datadogKey;
    private List<StringWarp> entitlementsPath;
    private List<StringWarp> mobileProvisionProfilesPath;
    private BuildToTest buildToTest;
    private Boolean buildWithLogs;
    private Boolean workflowOutputLogs;
    private Boolean googlePlaySign;
    private String secondOutput;

    private String outputName;

    @Before
    public void setUp() throws Exception {
        createOutputLocation();
        logger.info("Loading environment variables...");
        loadEnvironmentVariables();

        logger.info("Loading system properties...");
        loadSystemProperties();

        checkAndSetNullValues();
        logger.info("Printing all variables...");
        printAllValues();  // Print all values after setup for visibility

        logger.info("Checking if files exist:");
        // Check if files exist for paths that are not empty
        checkFileExists(this.appFilePath, "App File Path");
        if (!Objects.equals(this.keystoreFilePath, "null")) {
            checkFileExists(this.keystoreFilePath, "Keystore File Path");
        }
        if (!Objects.equals(this.certificateFilePath, "null")) {
            checkFileExists(this.certificateFilePath, "Certificate File Path");
        }

        // Check if files exist for each entitlement and provision profile path
        if (!Objects.equals(this.entitlementsPath.get(0).getItem(), "null")) {
            checkFilesExist(this.entitlementsPath, "Entitlements Path");
        }
        if (!Objects.equals(this.mobileProvisionProfilesPath.get(0).getItem(), "null")) {
            checkFilesExist(this.mobileProvisionProfilesPath, "Mobile Provision Profiles Path");
        }
    }

    private void createOutputLocation() {
        File dir = new File(PLUGIN_TMP_OUTPUT);
        if (!dir.exists()) {
            dir.mkdirs(); // Create directories if they do not exist
        }
    }


    /**
     * Loads environment variables used across various tests.
     */
    private void loadEnvironmentVariables() {
        // Environment variables are typically more secure and can be used for sensitive data
        this.token = System.getenv("APPDOME_API_TOKEN");
        this.keystoreAlias = System.getenv("KEYSTORE_ALIAS");
        this.keystoreKeyPass = System.getenv("KEYSTORE_KEY_PASS");
        this.keystorePassword = System.getenv("KEYSTORE_PASSWORD");
        this.certificatePassword = System.getenv("P12_PASSWORD");
    }

    /**
     * Loads system properties, providing defaults where necessary to ensure tests have all necessary data.
     */
    private void loadSystemProperties() {
        this.teamId = System.getProperty("teamId", "default-teamId");
        this.signOption = System.getProperty("signOption", "default-signOption");
        this.appFilePath = System.getProperty("appFilePath", "default-appFilePath");
        this.keystoreFilePath = System.getProperty("keystoreFilePath", "default-keystoreFilePath");
        this.certificateFilePath = System.getProperty("certificateFilePath", "default-certificateFilePath");
        this.fusionSetId = System.getProperty("fusionSetId", "default-fusionSetId");
        this.signFingerprint = System.getProperty("signFingerprint", "default-signFingerprint");
        this.firebaseAppId = System.getProperty("firebaseAppId", "default-firebaseAppId");
        this.datadogKey = System.getProperty("datadogKey", "default-datadogKey");


        // Convert CSV from system properties to List<StringWarp> for entitlements and provisions
        String entitlementsCsv = System.getProperty("entitlementsPath", "default1,default2");
        this.entitlementsPath = convertCsvToListStringWarp(entitlementsCsv);

        String mobileProvisionsCsv = System.getProperty("mobileProvisionProfilesPath", "default1,default2");
        this.mobileProvisionProfilesPath = convertCsvToListStringWarp(mobileProvisionsCsv);

        // Mock object for BuildToTest - you might need to set this differently based on your test environment
        this.buildToTest = new BuildToTest(System.getProperty("buildToTest", "default-buildToTest"));

        this.buildWithLogs = Boolean.parseBoolean(System.getProperty("buildWithLogs", "false"));
        this.workflowOutputLogs = Boolean.parseBoolean(System.getProperty("workflowOutputLogs", "false"));

        this.googlePlaySign = Boolean.parseBoolean(System.getProperty("googlePlaySign", "false"));
        this.secondOutput = System.getProperty("secondOutput", "default-secondOutput");
        this.outputName = System.getProperty("outputName", "protected_app");
    }


    // Add the method to check all values and set to null if needed
    public void checkAndSetNullValues() {
        if (isNoneOrEmpty(this.token)) this.token = null;
        if (isNoneOrEmpty(this.teamId)) this.teamId = null;
        if (isNoneOrEmpty(this.signOption)) this.signOption = null;
        if (isNoneOrEmpty(this.appFilePath)) this.appFilePath = null;
        if (isNoneOrEmpty(this.keystoreFilePath)) this.keystoreFilePath = null;
        if (isNoneOrEmpty(this.keystoreAlias)) this.keystoreAlias = null;
        if (isNoneOrEmpty(this.keystoreKeyPass)) this.keystoreKeyPass = null;
        if (isNoneOrEmpty(this.keystorePassword)) this.keystorePassword = null;
        if (isNoneOrEmpty(this.certificateFilePath)) this.certificateFilePath = null;
        if (isNoneOrEmpty(this.certificatePassword)) this.certificatePassword = null;
        if (isNoneOrEmpty(this.fusionSetId)) this.fusionSetId = null;
        if (isNoneOrEmpty(this.signFingerprint)) this.signFingerprint = null;
        if (this.buildToTest != null && Objects.equals(this.buildToTest.getSelectedVendor().toLowerCase(), "none")) {
            this.buildToTest = null;
        }
        if (this.entitlementsPath != null && this.entitlementsPath.isEmpty()) this.entitlementsPath = null;
        if (this.mobileProvisionProfilesPath != null && this.mobileProvisionProfilesPath.isEmpty())
            this.mobileProvisionProfilesPath = null;
        if (this.buildWithLogs != null && !this.buildWithLogs) this.buildWithLogs = null;
        if (this.workflowOutputLogs != null && !this.workflowOutputLogs) this.workflowOutputLogs = null;

        if (this.googlePlaySign != null && !this.googlePlaySign) this.googlePlaySign = null;
        if (isNoneOrEmpty(this.secondOutput)) this.secondOutput = null;
        if (isNoneOrEmpty(this.outputName)) this.outputName = null;
        if (isNoneOrEmpty(this.firebaseAppId)) this.firebaseAppId = null;
        if (isNoneOrEmpty(this.datadogKey)) this.datadogKey = null;


    }

    // Helper method to check if a string is "None" or empty
    private boolean isNoneOrEmpty(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("none");
    }


    /**
     * Checks if a file exists at the given path.
     *
     * @param filePath    The path of the file to check.
     * @param description Description of the file for logging purposes.
     */
    private void checkFileExists(String filePath, String description) {
        if (filePath != null && !filePath.isEmpty()) {
            File file = new File(filePath);
            if (!file.exists()) {
                logger.severe(description + " does not exist: " + filePath);

                // Get the directory name and list the contents
                File dir = file.getParentFile();  // Get the parent directory
                if (dir != null && dir.exists() && dir.isDirectory()) {
                    String[] files = dir.list();
                    logger.info("Contents of directory " + dir.getAbsolutePath() + ": " + Arrays.toString(files));
                } else {
                    logger.warning("The parent directory does not exist or is not a directory.");
                }
                throw new IllegalArgumentException(description + " does not exist: " + filePath);

            } else {
                logger.info(description + " exists: " + filePath);
            }
        }
    }


    /**
     * Checks if a list of files exist.
     *
     * @param filePaths   List of file paths to check.
     * @param description Description of the file type being checked (for logging purposes).
     */
    private void checkFilesExist(List<StringWarp> filePaths, String description) {
        if (filePaths != null && !filePaths.isEmpty()) {
            for (StringWarp filePathWarp : filePaths) {
                String filePath = filePathWarp.getItem();
                checkFileExists(filePath, description);
            }
        } else {
            logger.info(description + " is empty or not provided.");
        }
    }


    /**
     * Converts a CSV string to a List of StringWarp objects.
     *
     * @param csv The comma-separated string to convert.
     * @return A list of StringWarp objects.
     */
    private List<StringWarp> convertCsvToListStringWarp(String csv) {
        return Arrays.stream(csv.split(","))
                .map(StringWarp::new)
                .collect(Collectors.toList());
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) { // Make sure there is a '.' in the filename
            return fileName.substring(dotIndex + 1).toLowerCase();
        } else {
            return null; // No extension found
        }
    }

    @Test
    public void workFlowTest() throws Exception {

        try {
            String platform = getFileExtension(this.appFilePath);
            if (platform == null) {
                throw new IllegalArgumentException("App file path does not have a valid extension.");
            }
            logger.info("The app extension is " + platform);


            // Platform-specific tests
            if (Objects.equals(platform, "ipa")) {
                logger.info("Goes to method performIosTests");
                performIosTests();
            } else {
                logger.info("Goes to method performAndroidTests");
                performAndroidTests(platform);
            }


        } catch (Exception e) {
            logger.severe("Error during workflow test: " + e.getMessage());
            fail("Test failed due to exception: " + e.getMessage());
        }
    }


    /**
     * Tests Android-specific functionality. Asserts that operations complete successfully.
     *
     * @param extension The file extension to check for specific configurations.
     */
    private void performAndroidTests(String extension) throws Exception {
        logger.info("performAndroidTests");
        StringWarp stringWarpSecondOutput = null;
        if (extension.equals("aab")) {
            if (secondOutput != null) {
                stringWarpSecondOutput = new StringWarp(secondOutput);
            }
        }
        if (this.workflowOutputLogs != null && this.workflowOutputLogs) {
            File file = new File(PLUGIN_TMP_OUTPUT + "workflow_output_logs.logs");
            try {
                if (file.createNewFile()) {
                    System.out.println("File created successfully: " + file.getName());
                } else {
                    System.out.println("File already exists: " + file.getName());
                }
            } catch (IOException e) {
                System.err.println("An error occurred while creating the file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        logger.info("signOption is " + signOption);
        Crashlytics crashlytics = null;
        if (this.firebaseAppId != null) {
            crashlytics = new Crashlytics(this.firebaseAppId);
        }
        Datadog datadog = null;
        if (this.datadogKey != null) {
            datadog = new Datadog(datadogKey);
        }
        switch (this.signOption) {
            case "SIGN_ON_APPDOME":
                logger.info("Android: sign on appdome");
                Tests.testAndroidAutoSignBuild(this.jenkins, this.token, this.teamId, this.appFilePath,
                        this.fusionSetId, this.keystoreFilePath, this.keystorePassword, this.keystoreAlias,
                        this.keystoreKeyPass, this.signFingerprint, stringWarpSecondOutput, this.buildToTest,
                        this.buildWithLogs, this.outputName, crashlytics, datadog, this.workflowOutputLogs, logger);
                break;
            case "PRIVATE_SIGNING":
                logger.info("Android: private sign");
                Tests.testAndroidPrivateSignBuild(this.jenkins, this.token, this.teamId, this.appFilePath,
                        this.fusionSetId, this.signFingerprint, stringWarpSecondOutput, this.buildToTest,
                        this.buildWithLogs, this.googlePlaySign, this.outputName, crashlytics, datadog, this.workflowOutputLogs, logger);
                break;
            case "AUTO_DEV_SIGNING":
                logger.info("Android: auto dev sign");
                Tests.testAndroidAutoDevSignBuild(this.jenkins, this.token, this.teamId, this.appFilePath,
                        this.fusionSetId, this.signFingerprint, stringWarpSecondOutput, this.buildToTest,
                        this.buildWithLogs, this.googlePlaySign, this.outputName, crashlytics, datadog, this.workflowOutputLogs, logger);
                break;
            default:
                logger.info("That's not a valid sign option.");
                fail("Invalid sign option provided: " + this.signOption);
                break;
        }
    }


    /**
     * Tests iOS-specific functionality. Asserts expected outcomes based on operations.
     */
    private void performIosTests() throws Exception {
        logger.info("Inside performIosTests");
        logger.info("signOption is " + signOption);
        switch (this.signOption) {
            case "SIGN_ON_APPDOME":
                logger.info("iOS: sign on appdome");
                Tests.testIosAutoSignBuild(this.jenkins, this.token, this.teamId, this.appFilePath,
                        this.fusionSetId, this.certificateFilePath, this.certificatePassword,
                        this.mobileProvisionProfilesPath, this.entitlementsPath, buildToTest, buildWithLogs, this.outputName, this.workflowOutputLogs, logger);
                break;
            case "PRIVATE_SIGNING":
                logger.info("iOS: private sign");
                Tests.testIosPrivateSignBuild(this.jenkins, this.token, this.teamId, this.appFilePath,
                        this.fusionSetId, this.mobileProvisionProfilesPath, buildToTest, buildWithLogs, this.outputName, this.workflowOutputLogs, logger);
                break;
            case "AUTO_DEV_SIGNING":
                logger.info("iOS: auto dev sign");
                Tests.testIosAutoDevPrivateSignBuild(this.jenkins, this.token, this.teamId, this.appFilePath,
                        this.fusionSetId, this.mobileProvisionProfilesPath, this.entitlementsPath, buildToTest, buildWithLogs, this.outputName, this.workflowOutputLogs, logger);
                break;
            default:
                logger.info("That's not a valid sign option.");
                break;
        }
    }

    /**
     * Prints all the values of the class properties for debugging.
     */
    private void printAllValues() {
        logger.info("Current Test Configuration:");
        logger.info("Token: " + (this.token != null ? this.token : "null"));
        logger.info("Team ID: " + (this.teamId != null ? this.teamId : "null"));
        logger.info("Sign Option: " + (this.signOption != null ? this.signOption : "null"));
        logger.info("App File Path: " + (this.appFilePath != null ? this.appFilePath : "null"));
        logger.info("Keystore File Path: " + (this.keystoreFilePath != null ? this.keystoreFilePath : "null"));
        logger.info("Keystore Alias: " + (this.keystoreAlias != null ? this.keystoreAlias : "null"));
        logger.info("Keystore Key Pass: " + (this.keystoreKeyPass != null ? this.keystoreKeyPass : "null"));
        logger.info("Keystore Password: " + (this.keystorePassword != null ? this.keystorePassword : "null"));
        logger.info("Certificate File Path: " + (this.certificateFilePath != null ? this.certificateFilePath : "null"));
        logger.info("Certificate Password: " + (this.certificatePassword != null ? this.certificatePassword : "null"));
        logger.info("Fusion Set ID: " + (this.fusionSetId != null ? this.fusionSetId : "null"));
        logger.info("Sign Fingerprint: " + (this.signFingerprint != null ? this.signFingerprint : "null"));

        // Safely handle potential nulls for lists and objects
        logger.info("Entitlements Path: " + (this.entitlementsPath != null ?
                this.entitlementsPath.stream().map(StringWarp::toString).toString() : "null"));
        logger.info("Mobile Provision Profiles Path: " + (this.mobileProvisionProfilesPath != null ?
                this.mobileProvisionProfilesPath.stream().map(StringWarp::toString).toString() : "null"));

        logger.info("Build To Test: " + (this.buildToTest != null ? this.buildToTest.getSelectedVendor() : "null"));
        logger.info("Build With Logs: " + (this.buildWithLogs != null ? this.buildWithLogs : "null"));
        logger.info("Workflow output logs: " + (this.workflowOutputLogs != null ? this.workflowOutputLogs : "null"));
        logger.info("Google Play Sign: " + (this.googlePlaySign != null ? this.googlePlaySign : "null"));
        logger.info("Second Output: " + (this.secondOutput != null ? this.secondOutput : "null"));
    }

}
