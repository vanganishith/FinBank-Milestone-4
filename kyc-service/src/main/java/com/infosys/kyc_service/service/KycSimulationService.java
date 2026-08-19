package com.infosys.kyc_service.service;

import org.springframework.stereotype.Service;
import java.util.Base64;
import java.util.regex.Pattern;

@Service
public class KycSimulationService {

    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^\\d{12}$");

    /**
     * Hard format validation for the document reference number.
     * AADHAAR must be exactly 12 digits. Other document types are left permissive.
     */
    public boolean validateDocumentNumber(String documentType, String documentReference) {
        if (documentReference == null) return false;
        String ref = documentReference.trim().replaceAll("\\s", "");
        if ("AADHAAR".equalsIgnoreCase(documentType)) {
            return AADHAAR_PATTERN.matcher(ref).matches();
        }
        return ref.length() >= 4; // PAN / PASSPORT etc. — basic presence check
    }

    /**
     * SIMULATED document OCR — this project does not perform real computer vision.
     * Deterministically derives a plausible extracted name from the reference string
     * so results are explainable and reproducible, not random.
     */
    public String simulateOcr(String documentReference, String customerName) {
        if (documentReference == null || documentReference.trim().length() < 4) {
            return null; // OCR "failed" — unreadable document
        }
        return customerName; // simulate a clean read matching the customer's name on file
    }

    /**
     * SIMULATED face match — compares the uploaded document photo against the live
     * selfie capture. No real facial recognition is performed; instead we derive a
     * deterministic "similarity" score from the actual image bytes of both photos,
     * so identical or near-identical photos score high, and unrelated photos score
     * lower — reproducible per pair, not random.
     */
    public int simulateFaceMatchScore(String documentImageBase64, String selfieImageBase64) {
        if (documentImageBase64 == null || selfieImageBase64 == null) return 0;

        byte[] docBytes = decode(documentImageBase64);
        byte[] selfieBytes = decode(selfieImageBase64);
        if (docBytes.length == 0 || selfieBytes.length == 0) return 0;

        // Sample a fixed number of evenly-spaced bytes from each image and compare
        // average intensity — a crude stand-in for "how visually similar are these".
        int samples = 200;
        long docSum = 0, selfieSum = 0;
        for (int i = 0; i < samples; i++) {
            int docIdx = (int) ((long) i * docBytes.length / samples);
            int selfieIdx = (int) ((long) i * selfieBytes.length / samples);
            docSum += (docBytes[docIdx] & 0xFF);
            selfieSum += (selfieBytes[selfieIdx] & 0xFF);
        }
        double docAvg = docSum / (double) samples;
        double selfieAvg = selfieSum / (double) samples;

        double diff = Math.abs(docAvg - selfieAvg); // 0 (identical-ish) .. 255 (very different)
        double similarity = Math.max(0, 100 - (diff * 100.0 / 255.0));

        // Blend in a small deterministic factor from image sizes so two genuinely
        // different photos of the same rough brightness don't always look identical.
        double sizeRatio = Math.min(docBytes.length, selfieBytes.length)
                / (double) Math.max(docBytes.length, selfieBytes.length);
        double score = (similarity * 0.7) + (sizeRatio * 100 * 0.3);

        return (int) Math.round(Math.max(0, Math.min(100, score)));
    }

    private byte[] decode(String base64) {
        try {
            String cleaned = base64.contains(",") ? base64.substring(base64.indexOf(",") + 1) : base64;
            return Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }

    /** SIMULATED liveness check — passes unless the reference looks obviously invalid. */
    public boolean simulateLiveness(String documentReference) {
        return documentReference != null && !documentReference.toLowerCase().contains("test-fail");
    }

    /** Combines the above into a single risk score, 0 (safest) to 100 (riskiest). */
    public int calculateRiskScore(boolean ocrVerified, boolean documentNumberValid, int faceMatchScore, boolean livenessPassed) {
        int score = 0;
        if (!documentNumberValid) score += 60; // wrong/malformed ID number is the biggest red flag
        if (!ocrVerified) score += 20;
        if (faceMatchScore < 60) score += 40;
        else if (faceMatchScore < 80) score += 15;
        if (!livenessPassed) score += 40;
        return Math.min(score, 100);
    }
}