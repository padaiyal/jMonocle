package org.padaiyal.utilities;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.padaiyal.utilities.unittestextras.parameterconverters.ExceptionClassConverter;

/**
 * Test class for I18nUtility.
 */
class I18nUtilityTest {

  /**
   * The locale of the property files.
   */
  private static Locale locale = null;
  /**
   * The name of the default resource bundle.
   */
  private static String defaultResourceBundle = null;
  /**
   * The logging level to set for the code to test.
   */
  protected static Level loggingLevel = Level.OFF;

  /**
   * Retrieves the required information from test.properties file.
   *
   * @throws IOException When there is an issue loading the properties file.
   */
  @BeforeAll
  static void setUpClass() throws IOException {
    Configurator.setAllLevels("", loggingLevel);
    PropertyUtility.addPropertyFile(I18nUtilityTest.class, "test.properties");
    String language = PropertyUtility.getProperty("common.language");
    String region = PropertyUtility.getProperty("common.region");
    locale = new Locale(language, region);
    defaultResourceBundle = PropertyUtility.getProperty("I18NUtilityTest.resourcebundle.default");
    I18nUtility.addResourceBundle(I18nUtilityTest.class, defaultResourceBundle, locale);
  }

  /**
   * Removes all added resource bundle except for the default resource bundle.
   */
  @AfterEach
  void tearDownTest() {
    I18nUtility.getResourceBundlesNames()
        .stream()
        .filter(resourceBundleName ->
            !resourceBundleName.equals(defaultResourceBundle)
        )
        .forEach(I18nUtility::removeResourceBundle);
  }

  /**
   * Test the org.padaiyal.utilities.I18nUtility::getString(java.lang.String) with valid keys.
   *
   * @param key The key to test.
   * @param message The expected value associated to the key.
   */
  @ParameterizedTest
  @CsvSource({
      "com.sample.message, Hello All!!!",
      "test2.message, test message 2",
      // Same key in multiple resource bundles. Uses Test1_Resource_Bundle
      "generic.message, Right message"
  })
  void testGetStringFromAllResourceBundlesWithValidInputs(String key, String message) {
    String[] resourceBundleNames = {"Test2_Resource_Bundle", "Test1_Resource_Bundle"};
    Arrays.stream(resourceBundleNames)
        .forEach(
            resourceBundleName ->
                I18nUtility.addResourceBundle(I18nUtilityTest.class, resourceBundleName, locale)
        );
    String actualMessage = I18nUtility.getString(key);

    Assertions.assertEquals(message, actualMessage);
  }

  /**
   * Test the org.padaiyal.utilities.I18nUtility::getString(java.lang.String) with invalid keys.
   *
   * @param key The key to test.
   * @param exceptionTypeClass The expected exception type.
   */
  @ParameterizedTest
  @CsvSource({
      "com.sample.message.invalid, MissingResourceException.class",
      ", NullPointerException.class",
  })
  void testGetStringFromAllResourceBundlesWithInvalidInputs(
      String key,
      @ConvertWith(ExceptionClassConverter.class) Class<? extends Exception> exceptionTypeClass
  ) {
    I18nUtility.addResourceBundle(I18nUtilityTest.class, "Test2_Resource_Bundle", locale);
    Assertions.assertThrows(
        exceptionTypeClass,
        () -> I18nUtility.getString(key)
    );
  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility::getString(java.lang.String, java.lang.String) with
   * valid keys and resource bundles.
   *
   * @param resourceBundleName The resource bundle to retrieve the key from.
   * @param key                The key to test.
   * @param message            The expected value corresponding to the provided key.
   */
  @ParameterizedTest
  @CsvSource({
      "I18N_Resource_Bundle, com.sample.message, Hello All!!!",
  })
  void testGetStringFromOneResourceBundleWithValidInputs(String resourceBundleName, String key,
      String message) {

    Assertions.assertEquals(message, I18nUtility.getString(key, resourceBundleName));

  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility::getString(java.lang.String, java.lang.String) with
   * invalid keys and resource bundles.
   *
   * @param resourceBundleName The resource bundle to retrieve the key from.
   * @param key                The key to test.
   * @param exceptionType      The expected exception type.
   */
  @ParameterizedTest
  @CsvSource({
      // Invalid resource bundle
      "I18N_Resource_Bundle_invalid, com.sample.message, MissingResourceException.class",
      // Key not in resource
      "I18N_Resource_Bundle, test message 2, MissingResourceException.class",
      // Invalid key
      "I18N_Resource_Bundle, non.existing.key, MissingResourceException.class",
      // Null key
      "I18N_Resource_Bundle, , NullPointerException.class",
      // Null Resource Bundle
      "I18N_Resource_Bundle, , NullPointerException.class",

  })
  void testGetStringFromOneResourceBundleWithInvalidInputs(String resourceBundleName, String key,
      @ConvertWith(ExceptionClassConverter.class) Class<? extends Exception> exceptionType) {

    Assertions.assertThrows(exceptionType,
        () -> I18nUtility.getString(key, resourceBundleName)
    );

  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility::getFormattedString(java.lang.String,
   * java.lang.Object...) with valid keys and values.
   *
   * @param key              The key to test.
   * @param value            The value to format the message.
   * @param formattedMessage The expected formatted message.
   */
  @ParameterizedTest
  @CsvSource({
      "com.sample.formattedmessage, value1, Hello value1!!!",
      "generic.formatted.message, Thor, This is a formatted message Thor.",
      "com.sample.formattedmessage, , Hello null!!!",
  })
  void testGetFormattedStringFromAllResourceBundlesWithValidInputs(String key, String value,
      String formattedMessage) {
    String[] resourceBundleNames = {"Test2_Resource_Bundle", "Test1_Resource_Bundle"};
    Arrays.stream(resourceBundleNames)
        .forEach(
            resourceBundleName ->
                I18nUtility.addResourceBundle(I18nUtilityTest.class, resourceBundleName, locale)
        );
    Assertions.assertEquals(
        formattedMessage,
        I18nUtility.getFormattedString(key, value)
    );
  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility::getFormattedString(java.lang.String,
   * java.lang.Object...) with invalid keys and values.
   *
   * @param key           The key to test.
   * @param value         The value to format the message.
   * @param exceptionType The expected exception type.
   */
  @ParameterizedTest
  @CsvSource({
      // Valid key with invalid value.
      "com.sample.formattedmessage1, value1, IllegalFormatConversionException.class",
      // Invalid key
      "com.sample.formattedmessageinvalid, value1, MissingResourceException.class",

  })
  void testGetFormattedStringFromAllResourceBundlesWithInvalidInputs(String key, String value,
      @ConvertWith(ExceptionClassConverter.class) Class<? extends Exception> exceptionType) {
    Assertions.assertThrows(
        exceptionType,
        () -> I18nUtility.getFormattedString(key, value)
    );
  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility
   * ::getFormattedStringFromResourceBundle(java.lang.String,java.lang.String,java.lang.Object...)
   * with valid keys and resource bundles.
   *
   * @param resourceBundleName The resource bundle to retrieve the key from.
   * @param key                The key to test.
   * @param value              The value to format the message.
   * @param formattedMessage   The expected formatted message.
   */
  @ParameterizedTest
  @CsvSource({
      // Valid key and valid value.
      "I18N_Resource_Bundle, com.sample.formattedmessage, value1, Hello value1!!!",
      // Valid key with null value type.
      "I18N_Resource_Bundle, com.sample.formattedmessage1, , Hello null!!!",
  })
  void testGetFormattedStringFromOneResourceBundleWithValidInputs(String resourceBundleName,
      String key,
      String value,
      String formattedMessage) {

    Assertions.assertEquals(
        formattedMessage,
        I18nUtility.getFormattedStringFromResourceBundle(key, resourceBundleName, value)
    );

  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility
   * ::getFormattedStringFromResourceBundle(java.lang.String,java.lang.String,java.lang.Object...)
   * with invalid keys and resource bundles.
   *
   * @param resourceBundleName The resource bundle to retrieve the key from.
   * @param key                The key to test.
   * @param value              The value to format the message.
   * @param exceptionType      The expected exception type.
   */
  @ParameterizedTest
  @CsvSource({
      // Valid key with invalid value type.
      "I18N_Resource_Bundle,com.sample.formattedmessage1,st,IllegalFormatConversionException.class",
      // Invalid key
      "I18N_Resource_Bundle,com.sample.formattedmessageinvalid,str,MissingResourceException.class",
      // Invalid resource bundle.
      "I18N_Resource_Bundle_invalid, com.sample.message, value1, MissingResourceException.class",
  })
  void testGetFormattedStringFromOneResourceBundleWithInvalidInputs(String resourceBundleName,
      String key,
      String value,
      @ConvertWith(ExceptionClassConverter.class) Class<? extends Exception> exceptionType) {

    Assertions.assertThrows(
        exceptionType,
        () -> I18nUtility.getFormattedStringFromResourceBundle(key, resourceBundleName, value)
    );
  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility::addResourceBundle(java.lang.Class, java.lang.String,
   * java.util.Locale) with valid resource bundles.
   *
   * @param resourceBundleName The resource bundle to add.
   * @param key                The key in the resource bundle.
   * @param value              The value associated to the provided key.
   */
  @ParameterizedTest
  @CsvSource({
      "Test1_Resource_Bundle, test1.message, test message 1",
      "Test2_Resource_Bundle, test2.message, test message 2",
  })
  void testAddResourceBundleWithValidInputs(String resourceBundleName, String key, String value) {
    I18nUtility.addResourceBundle(I18nUtilityTest.class, resourceBundleName, locale);
    String valueRetrieved = I18nUtility.getString(key);
    Assertions.assertEquals(value, valueRetrieved);

  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility::addResourceBundle(java.lang.Class, java.lang.String,
   * java.util.Locale) with invalid resource bundles.
   *
   * @param resourceBundleName The resource bundle to add.
   * @param exceptionType      The expected exception type.
   */
  @ParameterizedTest
  @CsvSource({
      "Test1_Resource_Bundle_invalid, MissingResourceException.class",
      ", MissingResourceException.class"
  })
  void testAddResourceBundleWithInvalidInputs(String resourceBundleName,
      @ConvertWith(ExceptionClassConverter.class) Class<? extends Exception> exceptionType) {

    Assertions.assertThrows(
        exceptionType,
        () -> I18nUtility.addResourceBundle(I18nUtilityTest.class, resourceBundleName, locale));

  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility::removeResourceBundle(java.lang.String) with a valid
   * resource bundle.
   */
  @Test
  void testRemoveResourceBundle() {

    I18nUtility.addResourceBundle(I18nUtilityTest.class, "Test2_Resource_Bundle", locale);
    I18nUtility.getString("test2.message");
    I18nUtility.removeResourceBundle("Test2_Resource_Bundle");

    Assertions
        .assertThrows(MissingResourceException.class, () -> I18nUtility.getString("test2.message"));
  }

  /**
   * Tests org.padaiyal.utilities.I18nUtility::removeResourceBundle(java.lang.String) with an
   * invalid resource bundle.
   *
   * @param resourceBundleName The resource bundle to remove.
   */
  @ParameterizedTest
  @CsvSource({
      "Test1_Resource_Bundle_invalid",
      " ,"
  })
  void testRemoveInvalidResourceBundle(String resourceBundleName) {
    Assertions.assertThrows(MissingResourceException.class,
        () -> I18nUtility.removeResourceBundle(resourceBundleName));
  }
}
