package io.jenkins.plugins.appdome.build.to.secure.platform.android.certificate.method;

import hudson.Util;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

public final class TrustedSigningFingerprintsFormValidation {

    private TrustedSigningFingerprintsFormValidation() {}

    /**
     * Value of the trusted-signing checkbox in live Stapler {@code @QueryParameter} validation:
     * HTML checkboxes submit {@code "on"} when checked; other paths may send {@code "true"}.
     */
    static boolean isTrustedSigningFingerprintsFileChecked(String trustedSigningFingerprintsFile) {
        if (trustedSigningFingerprintsFile == null) {
            return false;
        }
        String v = Util.fixEmptyAndTrim(trustedSigningFingerprintsFile);
        if (v == null) {
            return false;
        }
        return "true".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v);
    }

    public static FormValidation validateListPath(
            String signingFingerprintListPath, String trustedSigningFingerprintsFile) {
        Jenkins.get().checkPermission(Jenkins.READ);
        if (!isTrustedSigningFingerprintsFileChecked(trustedSigningFingerprintsFile)) {
            return FormValidation.ok();
        }
        if (signingFingerprintListPath != null && Util.fixEmptyAndTrim(signingFingerprintListPath) == null) {
            return FormValidation.error("Path to the trusted signing fingerprints JSON file must be provided.");
        }
        if (signingFingerprintListPath != null && signingFingerprintListPath.contains(" ")) {
            return FormValidation.error("White spaces are not allowed in the path.");
        }
        return FormValidation.ok();
    }

    public static FormValidation validateFingerprintUnlessTrusted(
            String fingerprint, String trustedSigningFingerprintsFile) {
        Jenkins.get().checkPermission(Jenkins.READ);
        if (isTrustedSigningFingerprintsFileChecked(trustedSigningFingerprintsFile)) {
            return FormValidation.ok();
        }
        if (fingerprint != null && Util.fixEmptyAndTrim(fingerprint) == null) {
            return FormValidation.error("Fingerprint must be provided.");
        }
        if (fingerprint != null && fingerprint.contains(" ")) {
            return FormValidation.error("White spaces are not allowed in the fingerprint.");
        }
        return FormValidation.ok();
    }

    public static FormValidation validateGoogleFingerprintUnlessTrusted(
            String googleSignFingerPrint, String trustedSigningFingerprintsFile) {
        Jenkins.get().checkPermission(Jenkins.READ);
        if (isTrustedSigningFingerprintsFileChecked(trustedSigningFingerprintsFile)) {
            return FormValidation.ok();
        }
        if (googleSignFingerPrint != null && Util.fixEmptyAndTrim(googleSignFingerPrint) == null) {
            return FormValidation.error("If Google Sign is enabled, fingerprint must be provided.");
        }
        if (googleSignFingerPrint != null && googleSignFingerPrint.contains(" ")) {
            return FormValidation.error("White spaces are not allowed in the fingerprint.");
        }
        return FormValidation.ok();
    }
}
