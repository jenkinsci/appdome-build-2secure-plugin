package io.jenkins.plugins.appdome.build.to.secure;

import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PipelineTest {
    private final static Logger logger = Logger.getLogger(PipelineTest.class.getName());

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private AppdomeBuilderTest jenkinsPluginTest = new AppdomeBuilderTest(jenkins);
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

    private List<StringWarp> entitlementsPath;
    private List<StringWarp> mobileProvisionProfilesPath;
    private BuildToTest buildToTest;
    private Boolean buildWithLogs;
    private Boolean googlePlaySign;
    private String secondOutput;

    @Before
    public void setUp() throws Exception {
        loadEnvironmentVariables();
        loadSystemProperties();
        configureGlobalProperties();
    }

    private void configureGlobalProperties() {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars env = prop.getEnvVars();
        env.put("APPDOME_SERVER_BASE_URL", "https://qamaster.dev.appdome.com");
        this.jenkinsPluginTest.getJenkins().jenkins.getGlobalNodeProperties().add(prop);
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

        // Convert CSV from system properties to List<StringWarp> for entitlements and provisions
        String entitlementsCsv = System.getProperty("entitlementsPath", "default1,default2");
        this.entitlementsPath = convertCsvToListStringWarp(entitlementsCsv);

        String mobileProvisionsCsv = System.getProperty("mobileProvisionProfilesPath", "default1,default2");
        this.mobileProvisionProfilesPath = convertCsvToListStringWarp(mobileProvisionsCsv);

        // Mock object for BuildToTest - you might need to set this differently based on your test environment
        this.buildToTest = new BuildToTest(System.getProperty("buildToTest", "default-buildToTest"));

        this.buildWithLogs = Boolean.parseBoolean(System.getProperty("buildWithLogs", "false"));
        this.googlePlaySign = Boolean.parseBoolean(System.getProperty("googlePlaySign", "false"));
        this.secondOutput = System.getProperty("secondOutput", "default-secondOutput");
    }

    /**
     * Converts a CSV string to a List of StringWarp objects.
     *
     * @param csv The comma-separated string to convert.
     * @return A list of StringWarp objects.
     */
    private List<StringWarp> convertCsvToListStringWarp(String csv) {
        return Arrays.asList(csv.split(",")).stream()
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


            // Platform-specific tests
            if (Objects.equals(platform, "ipa")) {
                performIosTests();
            } else {
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
        StringWarp stringWarpSecondOutput = null;
        if (extension.equals("aab")) {
            if (secondOutput != null) {
                stringWarpSecondOutput = new StringWarp(secondOutput);
            }
            switch (this.signOption) {
                case "SIGN_ON_APPDOME":
                    logger.info("Android: sign on appdome");
                    this.jenkinsPluginTest.testAndroidAutoSignBuild(this.token, this.teamId, this.appFilePath,
                            this.fusionSetId, this.keystoreFilePath, this.keystorePassword, this.keystoreAlias,
                            this.keystoreKeyPass, this.signFingerprint, stringWarpSecondOutput, this.buildToTest,
                            this.buildWithLogs);
                    break;
                case "PRIVATE_SIGNING":
                    logger.info("Android: private sign");
                    this.jenkinsPluginTest.testAndroidPrivateSignBuild(this.token, this.teamId, this.appFilePath,
                            this.fusionSetId, this.signFingerprint, stringWarpSecondOutput, this.buildToTest,
                            this.buildWithLogs, this.googlePlaySign);
                    break;
                case "AUTO_DEV_SIGNING":
                    logger.info("Android: auto dev sign");
                    this.jenkinsPluginTest.testAndroidAutoDevSignBuild(this.token, this.teamId, this.appFilePath,
                            this.fusionSetId, this.signFingerprint, stringWarpSecondOutput, this.buildToTest,
                            this.buildWithLogs, this.googlePlaySign);
                    break;
                default:
                    logger.info("That's not a valid sign option.");
                    break;
            }
        }
    }


    /**
     * Tests iOS-specific functionality. Asserts expected outcomes based on operations.
     */
    private void performIosTests() throws Exception {
        switch (this.signOption) {
            case "SIGN_ON_APPDOME":
                logger.info("iOS: sign on appdome");
                this.jenkinsPluginTest.testIosAutoSignBuild(this.token, this.teamId, this.appFilePath,
                        this.fusionSetId, this.certificateFilePath, this.certificatePassword,
                        null, null, buildToTest, buildWithLogs);
                break;
            case "PRIVATE_SIGNING":
                logger.info("iOS: private sign");
                this.jenkinsPluginTest.testIosPrivateSignBuild(this.token, this.teamId, this.appFilePath,
                        this.fusionSetId, null, buildToTest, buildWithLogs);
                break;
            case "AUTO_DEV_SIGNING":
                logger.info("iOS: auto dev sign");
                this.jenkinsPluginTest.testIosAutoDevPrivateSignBuild(this.token, this.teamId, this.appFilePath,
                        this.fusionSetId, null, null, buildToTest, buildWithLogs);
                break;
            default:
                logger.info("That's not a valid sign option.");
                break;
        }
    }
}
