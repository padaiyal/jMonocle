package org.padaiyal.utilities.commandline;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.padaiyal.utilities.I18nUtility;
import org.padaiyal.utilities.PropertyUtility;

/**
 * Utility to manipulate streams.
 */
public final class StreamUtility {

  /**
   * Used to log information and errors for this class.
   */
  private static final Logger logger = LogManager.getLogger(StreamUtility.class);

  static {
    initializeDependantValues();
  }

  /**
   * Maximum size of stream to process in bytes.
   */
  private static final long maxStreamSizeInBytes = PropertyUtility.getTypedProperty(
      Long.class,
      "StreamUtility.maxStreamSize.bytes"
  );
  /**
   * Size of buffer to use to read data from the input stream.
   */
  private static final int bufferSize = PropertyUtility.getTypedProperty(
      Integer.class,
      "StreamUtility.bufferSize.bytes"
  );

  /**
   * Empty private constructor as this utility class is not meant to be used as an instance.
   */
  private StreamUtility() {

  }

  /**
   * Initialize static variables needed for this utility.
   */
  public static void initializeDependantValues() {
    try {
      PropertyUtility.addPropertyFile(
          StreamUtility.class,
          StreamUtility.class.getSimpleName() + ".properties"
      );

      I18nUtility.addResourceBundle(
          StreamUtility.class,
          StreamUtility.class.getSimpleName(),
          Locale.US
      );
    } catch (IOException e) {
      logger.error(e);
    }
  }

  /**
   * Extracts the information from a given InputStream as a string.
   *
   * @param inputStream           The InputStream to parse.
   * @param maxStreamSizeInBytes  Maximum size of stream to process in bytes.
   * @return                      The string extracted from the provided InputStream.
   * @throws IOException          Thrown when there is an issue reading from the stream.
   */
  public static String convertInputStreamToString(
      InputStream inputStream,
      long maxStreamSizeInBytes
  ) throws IOException {
    byte[] buffer = new byte[bufferSize];
    long dataReadFromStreamInBytes = 0;
    StringBuilder resultBuffer = new StringBuilder();
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      if ((dataReadFromStreamInBytes + bytesRead) > maxStreamSizeInBytes) {
        throw new IOException(
            I18nUtility.getFormattedString(
                "StreamUtility.error.exceededMaximumDataToReadFromStream",
                dataReadFromStreamInBytes + bytesRead,
                maxStreamSizeInBytes
            )
        );
      } else {
        if (bufferSize > bytesRead) {
          resultBuffer.append(new String(Arrays.copyOf(buffer, bytesRead)));
        } else {
          resultBuffer.append(new String(buffer));
        }
      }
    }
    return resultBuffer.toString();
  }

  /**
   * Extracts the information from a given InputStream as a string.
   *
   * @param inputStream   The InputStream to parse.
   * @return              The string extracted from the provided InputStream.
   * @throws IOException  Thrown when there is an issue reading from the stream.
   */
  public static String convertInputStreamToString(InputStream inputStream)
      throws IOException {
    return convertInputStreamToString(inputStream, maxStreamSizeInBytes);
  }
}

