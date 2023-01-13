package org.padaiyal.utilities.commandline;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.padaiyal.utilities.PropertyUtility;

/**
 * Tests for StreamUtility.
 */
class StreamUtilityTest {

  /**
   * Tests StreamUtility::convertInputStreamToString() with valid input.
   *
   * @param testString            String to test.
   * @param maxStreamSizeInBytes  Maximum size of stream to process in bytes.
   * @throws IOException          If there is an issue converting the input stream to a string.
   */
  @ParameterizedTest
  @CsvFileSource(
      resources = "testConvertInputStreamToStringParameters.csv",
      numLinesToSkip = 1,
      maxCharsPerColumn = 9000
  )
  public void testConvertInputStreamToString(
      String testString,
      int maxStreamSizeInBytes
  ) throws IOException {
    String stringFromInputStream;
    if (maxStreamSizeInBytes == -1) {
      stringFromInputStream = StreamUtility.convertInputStreamToString(
          new ByteArrayInputStream(testString.getBytes())
      );
    } else {
      stringFromInputStream = StreamUtility.convertInputStreamToString(
          new ByteArrayInputStream(testString.getBytes()),
          maxStreamSizeInBytes
      );
    }
    Assertions.assertEquals(testString, stringFromInputStream);
  }

  /**
   * Tests StreamUtility::convertInputStreamToString() with insufficient maximum stream size.
   *
   * @param testString            String to test.
   * @param maxStreamSizeInBytes  Maximum size of stream to process in bytes.
   */
  @ParameterizedTest
  @CsvSource({
      "ZXCVBNM!@#$%, 5"
  })
  public void testConvertInputStreamToStringWithInsufficientMaxStreamSize(
      String testString,
      int maxStreamSizeInBytes
  ) {
    Assertions.assertThrows(
        IOException.class,
        () -> StreamUtility.convertInputStreamToString(
            new ByteArrayInputStream(testString.getBytes()),
            maxStreamSizeInBytes
        )
    );
  }

  /**
   * Tests StreamUtility::convertInputStreamToString() with a null input.
   */
  @Test
  public void testConvertInputStreamToStringWithNullInput() {
    Assertions.assertThrows(NullPointerException.class,
        () -> StreamUtility.convertInputStreamToString(null));
  }

  /**
   * Test silent failure when adding a property file throws an IOException.
   */
  @Test
  void testIoExceptionThrownWhenAddingPropertyFile() {
    try (
        MockedStatic<PropertyUtility> propertyUtilityMock
            = Mockito.mockStatic(PropertyUtility.class)
    ) {
      propertyUtilityMock.when(
          () -> PropertyUtility.addPropertyFile(
              ArgumentMatchers.any(Class.class),
              ArgumentMatchers.anyString()
          )
      ).thenThrow(IOException.class);

      Assertions.assertDoesNotThrow(StreamUtility::initializeDependantValues);
    }
  }
}
