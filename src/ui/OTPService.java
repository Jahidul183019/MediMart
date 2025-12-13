package services;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight OTP service for testing:
 * - Generates 6-digit OTPs
 * - Stores them in-memory with expiry (default 5 minutes)
 * - "Sends" by printing to console (replace with SMTP/SMS later)
 */
public class OTPService {
    private static final Map<String, OTPEntry> store = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    private static final long TTL_SECONDS = 60 * 5; // 5 minutes

    public static String generateOTP() {
        int x = 100000 + random.nextInt(900000);
        return String.valueOf(x);
    }

    private static void storeOTP(String key, String otp) {
        store.put(key, new OTPEntry(otp, Instant.now().getEpochSecond() + TTL_SECONDS));
    }

    public static boolean verifyOTP(String key, String otp) {
        OTPEntry entry = store.get(key);
        if (entry == null) return false;
        long now = Instant.now().getEpochSecond();
        if (now > entry.expiry) {
            store.remove(key);
            return false;
        }
        boolean ok = entry.otp.equals(otp);
        if (ok) store.remove(key); // single-use
        return ok;
    }

    /** Simulate send via email — replace with SMTP later */
    public static boolean sendOTPEmail(String email) {
        String otp = generateOTP();
        storeOTP("email:" + email.toLowerCase().trim(), otp);
        // For now: print to console (developer/testing)
        System.out.println("[OTPService] Sent OTP to email " + email + ": " + otp);
        return true;
    }

    /** Simulate send via phone — replace with SMS provider later */
    public static boolean sendOTPPhone(String phone) {
        String otp = generateOTP();
        storeOTP("phone:" + phone.trim(), otp);
        System.out.println("[OTPService] Sent OTP to phone " + phone + ": " + otp);
        return true;
    }

    /** Key builder used by UI when verifying */
    public static String keyForEmail(String email) {
        return "email:" + email.toLowerCase().trim();
    }

    public static String keyForPhone(String phone) {
        return "phone:" + phone.trim();
    }

    private static class OTPEntry {
        final String otp;
        final long expiry;
        OTPEntry(String otp, long expiry) { this.otp = otp; this.expiry = expiry; }
    }
}
