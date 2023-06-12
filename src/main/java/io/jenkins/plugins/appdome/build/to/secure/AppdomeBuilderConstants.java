package io.jenkins.plugins.appdome.build.to.secure;

public interface AppdomeBuilderConstants {
    /**
     * Environment variables
     **/
    String APP_PATH = "APP_PATH";
    String KEYSTORE_PATH_ENV = "KEYSTORE_PATH";
    String MOBILE_PROVISION_PROFILE_PATHS_ENV = "MOBILE_PROVISION_PROFILE_PATHS";
    String ENTITLEMENTS_PATHS_ENV = "ENTITLEMENTS_PATHS";
    String APPDOME_HEADER_ENV_NAME = "APPDOME_CLIENT_HEADER";
    String APPDOME_BUILDE2SECURE_VERSION = "Jenkins/1.0";

    /**
     * FLAGS
     **/

    String KEY_FLAG = " --api_key ";
    String FUSION_SET_ID_FLAG = " --fusion_set_id ";
    String TEAM_ID_FLAG = " --team_id ";
    String APP_FLAG = " --app ";
    String OUTPUT_FLAG = " --output ";
    String SIGN_ON_APPDOME_FLAG = " --sign_on_appdome ";
    String KEYSTORE_FLAG = " --keystore ";
    String KEYSTORE_PASS_FLAG = " --keystore_pass ";
    String KEYSOTRE_ALIAS_FLAG = " --keystore_alias ";
    String KEY_PASS_FLAG = " --key_pass ";
    String PROVISION_PROFILES_FLAG = " --provisioning_profiles ";
    String ENTITLEMENTS_FLAG = " --entitlements ";
    String PRIVATE_SIGN_FLAG = " --private_signing ";
    String AUTO_DEV_PRIVATE_SIGN_FLAG = " --auto_dev_private_signing ";
    String GOOGLE_PLAY_SIGN_FLAG = " --google_play_signing ";
    String FINGERPRINT_FLAG = " --signing_fingerprint ";
    String CERTIFIED_SECURE_FLAG = " --certificate_output ";
    String BUILD_WITH_LOGS = " --build_logs ";


}
