package edu.dartmouth.isidro.util;

import java.util.Formatter;
import java.util.Locale;

public final class TextUtils {
  /**
   * Provides a user friendly and safe wrapper for producing a formatted string based on the default
   * locale given a format string and a number of Object arguments. The size of args must match the
   * number of String placeholders in the format template.
   *
   * @param format String format template.
   * @param args List of Object arguments to format into the template.
   * @return Formatted String based on template and args.
   */
  public static String formatText(final String format, final Object... args) {
    try (final Formatter formatter = new Formatter(Locale.getDefault())) {
      formatter.format(format, args);
      return formatter.toString();
    }
  }
}
