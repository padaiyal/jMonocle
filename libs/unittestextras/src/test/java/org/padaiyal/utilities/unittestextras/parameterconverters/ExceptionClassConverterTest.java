package org.padaiyal.utilities.unittestextras.parameterconverters;

import java.util.IllegalFormatConversionException;
import java.util.MissingResourceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the functionality of ExceptionClassConverter.
 */
public class ExceptionClassConverterTest {

  /**
   * Tests org.padaiyal.popper.parameterconverters.ExceptionClassConverter
   * ::convertExceptionNameToClass() with invalid inputs.
   */
  @ParameterizedTest
  @CsvSource({", NullPointerException.class",
              "UnknownException, ArgumentConversionException.class"})
  public void testConvertExceptionNameToClassWithInvalidInputs(
        String exceptionStringToCovert,
        @ConvertWith(ExceptionClassConverter.class)
        Class<? extends Exception> expectedExceptionToBeThrown) {
    Assertions.assertThrows(
        expectedExceptionToBeThrown,
        () -> ExceptionClassConverter.convertExceptionNameToClass(exceptionStringToCovert));
  }

  /**
   * Tests org.padaiyal.popper.parameterconverters.ExceptionClassConverter
   * ::convertExceptionNameToClass() with valid inputs.
   */
  @Test
  public void testConvertExceptionNameToClassWithValidInputs() {
    Assertions.assertEquals(
        NullPointerException.class,
        ExceptionClassConverter.convertExceptionNameToClass("NullPointerException.class")
    );
    Assertions.assertEquals(
        IllegalArgumentException.class,
        ExceptionClassConverter.convertExceptionNameToClass("IllegalArgumentException.class")
    );
    Assertions.assertEquals(
            MissingResourceException.class,
            ExceptionClassConverter.convertExceptionNameToClass(
                    "MissingResourceException.class")
    );
    Assertions.assertEquals(
            IllegalFormatConversionException.class,
            ExceptionClassConverter.convertExceptionNameToClass(
                    "IllegalFormatConversionException.class")
    );
  }
}