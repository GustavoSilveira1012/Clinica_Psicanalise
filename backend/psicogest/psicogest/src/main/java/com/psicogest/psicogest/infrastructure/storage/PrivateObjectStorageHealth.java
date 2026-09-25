package com.psicogest.psicogest.infrastructure.storage;

/** Provider-observed capabilities; configuration flags alone are not proof. */
public record PrivateObjectStorageHealth(
        boolean reachable,
        boolean privateAccessVerified,
        boolean atRestEncryptionVerified,
        boolean retentionPolicyVerified
) {
    public boolean productionReady() {
        return reachable && privateAccessVerified
                && atRestEncryptionVerified && retentionPolicyVerified;
    }
}
