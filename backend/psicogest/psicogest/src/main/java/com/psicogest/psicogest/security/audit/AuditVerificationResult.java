package com.psicogest.psicogest.security.audit;

public final class AuditVerificationResult {

    private final boolean success;

    private final int verifiedCount;

    /*
     * Preenchido apenas quando success == false.
     */
    private final Long failedAtSequence;

    private final String reason;

    private AuditVerificationResult(
            boolean success,
            int verifiedCount,
            Long failedAtSequence,
            String reason
    ) {

        this.success = success;
        this.verifiedCount = verifiedCount;
        this.failedAtSequence = failedAtSequence;
        this.reason = reason;
    }

    public static AuditVerificationResult success(
            int verifiedCount
    ) {

        return new AuditVerificationResult(
                true,
                verifiedCount,
                null,
                null
        );
    }

    public static AuditVerificationResult failure(
            long failedAtSequence,
            String reason
    ) {

        return new AuditVerificationResult(
                false,
                0,
                failedAtSequence,
                reason
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public int getVerifiedCount() {
        return verifiedCount;
    }

    public Long getFailedAtSequence() {
        return failedAtSequence;
    }

    public String getReason() {
        return reason;
    }
}
