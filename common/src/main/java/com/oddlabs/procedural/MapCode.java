package com.oddlabs.procedural;

import java.math.BigInteger;
import org.jspecify.annotations.NullMarked;

/**
 * Procedural island map code encoding and decoding.
 */
@NullMarked
public final class MapCode {
    public static final String CHAR_TO_WORD = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final String LOWER_CASE_CHARS = "abcdefghjklmnpqrstuvwxyz";

    private MapCode() {
    }

    private static int computeShifting() {
        return (int) (Math.log(CHAR_TO_WORD.length()) / Math.log(2));
    }

    public static BigInteger parseBits(String str) {
        byte[] index = new byte[1];
        BigInteger result = BigInteger.ZERO;
        int shifting = computeShifting();
        int accumShifting = 0;
        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            index[0] = (byte) CHAR_TO_WORD.indexOf(c);
            if (index[0] == -1) {
                throw new IllegalArgumentException("Invalid map code character: " + c);
            }
            BigInteger indexBig = new BigInteger(index);
            result = result.or(indexBig.shiftLeft(accumShifting));
            accumShifting += shifting;
        }
        return result;
    }

    public static String createString(BigInteger val) {
        StringBuilder result = new StringBuilder();
        int shifting = computeShifting();
        BigInteger filter = new BigInteger(new byte[]{(byte) (CHAR_TO_WORD.length() - 1)});
        for (int accumShifting = 0; accumShifting < val.bitLength(); accumShifting += shifting) {
            int charIndex = val.shiftRight(accumShifting).and(filter).intValue();
            result.insert(0, CHAR_TO_WORD.charAt(charIndex));
        }
        return result.toString();
    }
}
