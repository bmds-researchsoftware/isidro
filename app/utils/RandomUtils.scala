package utils

import java.math.BigInteger
import java.security.SecureRandom

object RandomUtils {
  private val RANDOM = new SecureRandom

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
