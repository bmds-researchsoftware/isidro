package utils

import java.math.BigInteger
import java.security.SecureRandom

object RandomUtils {
  private val RANDOM = new SecureRandom

  /**
   * Generates and returns String representation of random 512-bit integer of base 32.
   *
   * @return String representation of random integer of base 32.
   */
  def generateRandom512BitBase32NumberString = generateRandomBase32NumberString(512)

  /**
   * Generates and returns String representation of a given bit size integer of base 32.
   *
   * @return String representation of random integer of base 32.
   */
  def generateRandomBase32NumberString(bits: Int) = generateRandomNumberString(bits, 32)

  /**
   * Generates 512-bit secured random integer.
   *
   * @param bits Integer number of bits for random number generation
   * @return 512-bit random integer.
   */
  def generateRandomNumber(bits: Int) = new BigInteger(bits, RANDOM)

  /**
   * Generates and returns String representation of random integer of given base.
   *
   * @param bits Integer number of bits for random number generation
   * @param radix integer base which the String is.
   * @return String representation of random integer of given base.
   */
  def generateRandomNumberString(bits: Int, radix: Int) = generateRandomNumber(bits).toString(radix)
}
