package edu.dartmouth.isidro.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class RandomUtils {
  private static final SecureRandom RANDOM = new SecureRandom();

  /**
   * Generates and returns String representation of random 512-bit integer of base 32.
   *
   * @return String representation of random integer of base 32.
   */
  public static String generateRandom512BitBase32NumberString() {
    return generateRandomBase32NumberString(512);
  }

  /**
   * Generates and returns String representation of a given bit size integer of base 32.
   *
   * @return String representation of random integer of base 32.
   */
  public static String generateRandomBase32NumberString(final int bits) {
    return generateRandomNumberString(bits, 32);
  }

  /**
   * Generates 512-bit secured random integer.
   *
   * @param bits Integer number of bits for random number generation
   * @return 512-bit random integer.
   */
  public static BigInteger generateRandomNumber(final int bits) {
    return new BigInteger(bits, RANDOM);
  }

  /**
   * Generates and returns String representation of random integer of given base.
   *
   * @param bits Integer number of bits for random number generation
   * @param radix integer base which the String is.
   * @return String representation of random integer of given base.
   */
  public static String generateRandomNumberString(final int bits, final int radix) {
    return generateRandomNumber(bits).toString(radix);
  }
}
