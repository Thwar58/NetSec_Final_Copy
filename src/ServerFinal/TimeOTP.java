/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ServerFinal;

import csc3055.codec.Base32;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author burli
 */
public class TimeOTP {

    private SecretKey skey;
    Mac anHMac;
    private long baseTime;
    private final int TIME_DURATION = 30; // 30 second time slot.

    public TimeOTP() throws NoSuchAlgorithmException, InvalidKeyException {
        {
            Mac hmac = Mac.getInstance("HmacSHA1");
            skey = KeyGenerator.getInstance("HmacSHA1").generateKey();
            hmac.init(skey);
            this.anHMac = hmac;
            baseTime = 0;
        }
    }

    /**
     * This constructor creates a new TOTP object with the given key and an
     * intial starting value of baseTime.
     *
     * @param baseTime the base time.
     * @param key the base-64 encoded key.
     *
     * @throws NoSuchAlgorithmException when the hmacAlgo is unknown.
     * @throws InvalidKeyException when the key parameter is not a valid key.
     */
    public TimeOTP(long baseTime, String key) throws
            NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA1");
        skey = new SecretKeySpec(Base64.getDecoder().decode(key), "HmacSHA1");
        hmac.init(skey);
        anHMac = hmac;
        this.baseTime = baseTime;
    }

    public String get64Key() {
        return Base64.getEncoder().encodeToString(skey.getEncoded());
    }

    public String get32Key() {
        return Base32.encodeToString(skey.getEncoded(), true);
    }

    public void setSkey(SecretKey skey) {
        this.skey = skey;
    }

    public void setMac(Mac HMac) {
        this.anHMac = HMac;
    }

    /**
     * Generates a one-time password using the necessary HMAC. The OTP is
     * returned as a string of 6 digits (potentially with leading zeros).
     *
     * @param ctr the value of the counter to use in generateing the OTP.
     * @return a 6 digit OTP.
     */
    public String generateOTP(long ctr) {
        int otp = 0;

        // Generate the digest.
        byte[] digest = anHMac.doFinal(longToBytes(ctr));

        // Calculate the offset by looking at the low-order nibble of the
        // high order byte of the digest.
        int offset = (int) (digest[digest.length - 1] & 0x0F);

        // Look at the contiguous 4 bytes starting at the offset and
        // reduce the result mod 10^6 so that we end up with a 6 digit
        // code.
        otp = ((digest[offset] & 0x7F) << 24
                | (digest[offset + 1] & 0xFF) << 16
                | (digest[offset + 2] & 0xFF) << 8
                | (digest[offset + 3] & 0xFF)) % 1000000;

        return String.format("%06d", otp);
    }

    /**
     * This method converts a long value into an 8-byte value.
     *
     * @param num the number to conver to bytes.
     * @return an array of 8 bytes representing the number num.
     */
    private byte[] longToBytes(long num) {
        byte[] res = new byte[8];

        // Decompose the a long type into byte components.
        for (int i = 7; i >= 0; i--) {
            res[i] = (byte) (num & 0xFF);
            num >>= 8;
        }

        return res;
    }

    /**
     * This method verifies the OTP by determining if the OTP is within a window
     * of acceptable durationCounts.
     *
     * @param otp the one-time password to verify.
     * @return true if the otp passwords is valid (with in the window) and false
     * otherwise.
     */
    public boolean verify(String otp) {
        // Current token window.
        long durationCount = (long) Math.floor(
                (Instant.now().getEpochSecond() - baseTime) / TIME_DURATION);

        // previous token window to account for transmission delay.
        long durationCountWindow = (long) Math.floor(
                (Instant.now().getEpochSecond() - baseTime - TIME_DURATION)
                / TIME_DURATION);

        return (generateOTP(durationCount).equals(otp)
                || generateOTP(durationCountWindow).equals(otp));
    }

    /**
     * This method generates the next OTP for the client.
     *
     * @return the OTP as a string.
     */
    public String nextOTP() {
        long durationCount = (long) Math.floor(
                (Instant.now().getEpochSecond() - baseTime) / TIME_DURATION);
        String otp = generateOTP(durationCount);
        return otp;
    }

}
