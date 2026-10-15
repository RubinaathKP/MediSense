package com.medisense.safety;

public final class SafeRefusalResponse {

    public static final String DOCTOR_REFERRAL =
            "This question requires a qualified doctor's judgment. Please consult your healthcare provider before making any changes to your health routine.";

    public static final String EMERGENCY_REFERRAL =
            "This sounds like it may need immediate medical attention. Please call emergency services (112 in India) or go to your nearest emergency room immediately.";

    public static final String UNVERIFIED_OR_FLAGGED_OUTPUT =
            "We were unable to generate a verified response for this question from our trusted sources. Please consult your doctor.";

    private SafeRefusalResponse() {}
}
