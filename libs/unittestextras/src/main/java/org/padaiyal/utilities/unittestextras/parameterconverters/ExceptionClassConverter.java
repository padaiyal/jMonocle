package org.padaiyal.utilities.unittestextras.parameterconverters;

import java.util.Objects;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;

/**
 * Used to convert a given string into an exception class.
 */
public class ExceptionClassConverter implements ArgumentConverter {

  /**
   * Converts a given exception class string to the class object.
   *
   * @param expectedExceptionClassString String representation of the exception class.
   * @return                             The exception class object corresponding to the input
   *                                     string.
   * @throws ArgumentConversionException When an error occurs during conversion.
   */
  public static Class<? extends Exception> convertExceptionNameToClass(
      String expectedExceptionClassString
  ) throws ArgumentConversionException {
    // Input validation
    Objects.requireNonNull(expectedExceptionClassString);

    switch (expectedExceptionClassString) {
      case "NullPointerException.class": return NullPointerException.class;
      case "IllegalArgumentException.class": return IllegalArgumentException.class;
      default: throw new ArgumentConversionException(
          String.format(
              "Unable to parse expected exception from input string: %s.",
              expectedExceptionClassString
          )
      );
    }
  }

  /**
   * Converts a given exception class string to the class object.
   *
   * @param expectedExceptionClassString String representation of the exception class.
   * @param context                      The parameter context where the converted object will be
   *                                     used.
   * @return                             The exception class object corresponding to the input
   *                                     string.
   * @throws ArgumentConversionException When an error occurs during conversion.
  */
  @Override
  public Object convert(Object expectedExceptionClassString, ParameterContext context)
      throws ArgumentConversionException {
    return convertExceptionNameToClass(expectedExceptionClassString.toString());
  }
}
