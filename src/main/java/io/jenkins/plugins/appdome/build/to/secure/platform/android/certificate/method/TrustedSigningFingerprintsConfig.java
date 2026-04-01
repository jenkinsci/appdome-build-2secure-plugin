package io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method;

/**
 * Android signing methods that support an optional trusted signing fingerprints JSON file
 * ({@code --signing_fingerprint_list}).
 */
public interface TrustedSigningFingerprintsConfig {

    Boolean getTrustedSigningFingerprintsFile();

    String getSigningFingerprintListPath();
}
