package org.padaiyal.utilities.unittestextras.parameterconverters;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests functionality of ArrayConverter.
 */
public class ArrayConverterTest {

  /**
   * Test convert String to Array of specific type.
   *
   * @param inputString  The string to convert.
   * @param expectedSize The expected size of the output array.
   * @param arrayType    The type of the elements in the array.
   */
  @ParameterizedTest
  @CsvSource({
      "'1,2,3,4',4, [Ljava.lang.String;",
      "'hello, world',2, [Ljava.lang.String;",
      "'1,2,3,4',4, [Ljava.lang.Integer;",
      "'1,2,3,4',4, [Ljava.lang.Double;",
      "'1,2,3,4',4, [Ljava.lang.Float;",
      "'1,2,3,4',4, [Ljava.lang.Long;",
      ",0, [Ljava.lang.Long;"
  })
  public void testArrayConverter(String inputString, int expectedSize, Class<?> arrayType) {

    Object[] convertedArray = ArrayConverter.convertStringToArray(
        inputString,
        arrayType
    );

    if (inputString == null) {
      //noinspection ConstantConditions
      Assertions.assertNull(convertedArray);
      return;
    }
    String componentType = arrayType.getTypeName().replaceAll("[\\[\\]]", "");

    Assertions.assertEquals(convertedArray.length, expectedSize);

    Arrays.stream(convertedArray)
        .parallel()
        .forEach(element -> Assertions.assertEquals(componentType, element.getClass().getName())
        );

  }

  /**
   * Tests convert Strings to String arrays with valid inputs.
   *
   * @param stringInput       The string input to test.
   * @param targetType        The target type to convert to.
   * @param expectedException The expected exception thrown.
   */
  @ParameterizedTest
  @CsvSource({
      "'',, java.lang.NullPointerException",
      "0,java.lang.String, org.junit.jupiter.params.converter.ArgumentConversionException",
      "'hello,world',[Ljava.lang.Integer;, java.lang.NumberFormatException",
      "'',org.padaiyal.utilities.unittestextras.parameterconverters.ArrayConverterTest,"
          + " org.junit.jupiter.params.converter.ArgumentConversionException"
  })
  public void testArrayConverterWithInvalidInput(
      String stringInput,
      Class<?> targetType,
      Class<? extends Exception> expectedException
  ) {
    Assertions.assertThrows(
        expectedException,
        () -> ArrayConverter.convertStringToArray(stringInput, targetType)
    );
  }

  /**
   * Test convert from Integer to Integer array.
   */
  @Test
  public void testArrayConverterWithInvalidInputString() {
    Assertions.assertThrows(
        ArgumentConversionException.class,
        () -> ArrayConverter.convertStringToArray(0, Integer[].class)
    );
  }

  /**
   * Test convert String to String array during test input.
   *
   * @param actualArray       The converted String array.
   * @param expectedArraySize The expected size of the converted String array.
   */
  @ParameterizedTest
  @CsvSource({
      "'1,2,3,4',4",
      "'hello,world',2"
  })
  public void testStringArrayConverterWithParameters(
      @ConvertWith(ArrayConverter.class) String[] actualArray,
      int expectedArraySize
  ) {
    Assertions.assertEquals(expectedArraySize, actualArray.length);
  }

}
