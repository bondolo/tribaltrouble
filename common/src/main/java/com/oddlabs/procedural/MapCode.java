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

    public static final int MAX_PLAYERS = 6;
    public static final String SEED_CARDINALITY = "40000";
    public static final int SLIDER_CARDINALITY = 11;
    public static final int TERRAIN_TYPE_CARDINALITY = 2;
    public static final int SIZE_CARDINALITY = 3;
    public static final int DIFFICULTY_CARDINALITY = 4;
    public static final int RACE_CARDINALITY = 2;
    public static final int TEAM_CARDINALITY = 6;
    public static final BigInteger MAX_VALUE;

    static {
        BigInteger max = BigInteger.ONE;
        max = max.multiply(new BigInteger(SEED_CARDINALITY));
        max = max.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[]{TERRAIN_TYPE_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[]{SIZE_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[]{RACE_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[]{TEAM_CARDINALITY}));
        for (int i = 1; i < MAX_PLAYERS; i++) {
            max = max.multiply(new BigInteger(new byte[]{DIFFICULTY_CARDINALITY}));
            max = max.multiply(new BigInteger(new byte[]{RACE_CARDINALITY}));
            max = max.multiply(new BigInteger(new byte[]{TEAM_CARDINALITY}));
        }
        MAX_VALUE = max;
    }

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

    /**
     * Encodes procedural map parameters into a map code string.
     *
     * @param params parameters to encode
     * @return encoded map code string
     */
    public static String encode(MapParameters params) {
        BigInteger max_val = BigInteger.ONE;
        BigInteger result = BigInteger.ZERO;
        result = result.add((new BigInteger("" + params.seed())).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(SEED_CARDINALITY));

        result = result.add((new BigInteger(new byte[]{(byte) params.hills()})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));

        result = result.add((new BigInteger(new byte[]{(byte) params.vegetation()})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));

        result = result.add((new BigInteger(new byte[]{(byte) params.supplies()})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));

        result = result.add((new BigInteger(new byte[]{(byte) params.terrainType()})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[]{TERRAIN_TYPE_CARDINALITY}));

        result = result.add((new BigInteger(new byte[]{(byte) params.size()})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[]{SIZE_CARDINALITY}));

        result = result.add((new BigInteger(new byte[]{(byte) params.player0Race()})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[]{RACE_CARDINALITY}));

        result = result.add((new BigInteger(new byte[]{(byte) params.player0Team()})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[]{TEAM_CARDINALITY}));

        for (int i = 0; i < MAX_PLAYERS - 1; i++) {
            MapParameters.SlotSetting setting = (i < params.otherSlots().size())
                    ? params.otherSlots().get(i)
                    : new MapParameters.SlotSetting(0, 0, 0);

            result = result.add((new BigInteger(new byte[]{(byte) setting.difficulty()})).multiply(max_val));
            max_val = max_val.multiply(new BigInteger(new byte[]{DIFFICULTY_CARDINALITY}));

            result = result.add((new BigInteger(new byte[]{(byte) setting.race()})).multiply(max_val));
            max_val = max_val.multiply(new BigInteger(new byte[]{RACE_CARDINALITY}));

            result = result.add((new BigInteger(new byte[]{(byte) setting.team()})).multiply(max_val));
            max_val = max_val.multiply(new BigInteger(new byte[]{TEAM_CARDINALITY}));
        }

        return createString(result);
    }

    /**
     * Decodes a map code string into procedural map parameters.
     *
     * @param mapCode the string code to decode
     * @return decoded map parameters
     */
    public static MapParameters decode(String mapCode) {
        return decode(parseBits(mapCode.toUpperCase()));
    }

    /**
     * Decodes raw bit-packed integer into procedural map parameters.
     *
     * @param bits raw BigInteger bits
     * @return decoded map parameters
     */
    public static MapParameters decode(BigInteger bits) {
        BigInteger result = bits;
        BigInteger max_val = MAX_VALUE;

        MapParameters.SlotSetting[] otherSlots = new MapParameters.SlotSetting[MAX_PLAYERS - 1];

        for (int i = MAX_PLAYERS - 1; i >= 1; i--) {
            result = result.mod(max_val);
            max_val = max_val.divide(new BigInteger(new byte[]{TEAM_CARDINALITY}));
            int team = result.divide(max_val).intValue();

            result = result.mod(max_val);
            max_val = max_val.divide(new BigInteger(new byte[]{RACE_CARDINALITY}));
            int race = result.divide(max_val).intValue();

            result = result.mod(max_val);
            max_val = max_val.divide(new BigInteger(new byte[]{DIFFICULTY_CARDINALITY}));
            int difficulty = result.divide(max_val).intValue();

            otherSlots[i - 1] = new MapParameters.SlotSetting(difficulty, race, team);
        }

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[]{TEAM_CARDINALITY}));
        int player0Team = result.divide(max_val).intValue();

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[]{RACE_CARDINALITY}));
        int player0Race = result.divide(max_val).intValue();

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[]{SIZE_CARDINALITY}));
        int size = result.divide(max_val).intValue();

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[]{TERRAIN_TYPE_CARDINALITY}));
        int terrainType = result.divide(max_val).intValue();

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
        int supplies = result.divide(max_val).intValue();

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
        int vegetation = result.divide(max_val).intValue();

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
        int hills = result.divide(max_val).intValue();

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(SEED_CARDINALITY));
        int seed = result.divide(max_val).intValue();

        return new MapParameters(
                seed,
                hills,
                vegetation,
                supplies,
                terrainType,
                size,
                player0Race,
                player0Team,
                java.util.List.of(otherSlots)
        );
    }
}
