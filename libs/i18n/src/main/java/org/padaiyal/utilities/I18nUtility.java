package org.padaiyal.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for internationalizing strings.
 */
public final class I18nUtility {

  /**
   * Logger for I18nUtility class.
   */
  private static final Logger logger = LogManager.getLogger(I18nUtility.class);
  /**
   * Maps resource bundle name to its corresponding ResourceBundle object.
   */
  private static final Map<String, ResourceBundle> resourceBundles = Collections
      .synchronizedMap(new HashMap<>());

  /**
   * Utility class which doesn't need to be instantiated, hence the constructor is private.
   */
  private I18nUtility() {
  }

  /**
   * Retrieves the message string corresponding to the key specified. If multiple resource bundles
   * contain the same key, the first resource bundle (Decided by alphabetical order) in which the
   * key is present is used.
   *
   * @param key The key to retrieve the value for.
   * @return The value corresponding to the key specified.
   */
  public static String getString(String key) {
    Optional<String> value = resourceBundles.keySet()
        .stream()
        .sorted()
        .map(resourceBundleName -> {
          try {
            return getString(key, resourceBundleName);
          } catch (MissingResourceException e) {

            if (logger.isDebugEnabled()) {
              logger.debug(
                      String.format("Resource bundle %s does not contain property - %s",
                              resourceBundleName,
                              key
                      )
              );
            }
            return null;
          }
        }).filter(Objects::nonNull)
        .findFirst();

    if (value.isPresent()) {
      return value.get();
    } else {
      // Cannot internationalize as calls to getString()/getFormattedString()
      // will cause an infinite recursion
      throw new MissingResourceException(
          String.format("Can't find resource for bundle %s", key),
          I18nUtility.class.getName(),
          key
      );
    }
  }

  /**
   * Retrieves the value of the corresponding key from the provided resource bundle name.
   *
   * @param key                The key to retrieve the value for.
   * @param resourceBundleName The resource bundle to retrieve the value from.
   * @return The value of the corresponding key from the provided resource bundle.
   */
  public static String getString(String key, String resourceBundleName) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(resourceBundleName);

    ResourceBundle resourceBundle = resourceBundles.get(resourceBundleName);

    if (Objects.isNull(resourceBundle)) {
      throw new MissingResourceException(
          String.format("Resource bundle %s cannot be found", resourceBundleName),
          resourceBundleName,
          null
      );
    }
    return resourceBundle.getString(key);
  }

  /**
   * Retrieves the message string corresponding to the key specified with the provided values
   * inserted. If multiple resource bundles contain the same key, the first resource bundle (Decided
   * by the order in which the resource bundle is added) in which the key is present is used.
   *
   * @param key    The key to retrieve the value for.
   * @param values The values to format the retrieved string.
   * @return The formatted message from the provided key and values.
   */
  public static String getFormattedString(String key, Object... values) {
    // Cannot internationalize as calls to getFormattedString() will cause an infinite recursion
    if (logger.isDebugEnabled()) {
      logger.debug(
              String.format(
                      "Retrieving property %s with the following format values -  %s",
                      key,
                      Arrays.toString(values)
              )
      );
    }

    return String.format(
        getString(key),
        values
    );
  }

  /**
   * Retrieves the message string of the corresponding key from the specified resource bundle name
   * with the provided values.
   *
   * @param key                The key to retrieve the value for.
   * @param resourceBundleName The name of the resource Bundle to retrieve the string from.
   * @param values             The values to format the retrieved string.
   * @return The formatted message from the provided key and values.
   */
  public static String getFormattedStringFromResourceBundle(String key, String resourceBundleName,
      Object... values) {
    return String.format(
        getString(key, resourceBundleName),
        values
    );
  }

  /**
   * Add a specified resource bundle.
   *
   * @param cls                Class to retrieve the classpath from.
   * @param resourceBundleName Resource bundle to add.
   * @param locale             Locale of the resource bundle to add.
   */
  public static synchronized void addResourceBundle(Class<?> cls, String resourceBundleName,
      Locale locale) {
    // Cannot internationalize as the required I18N string cannot be found
    // without adding the relevant resource bundle.
    if (logger.isInfoEnabled()) {
      logger.info(
              String.format(
                      "Adding resource bundle - %s using the classpath %s",
                      resourceBundleName,
                      cls.getPackage().getName()
              )
      );
    }
    ResourceBundle resourceBundle = ResourceBundle.getBundle(
        String.format(
            "%s.%s",
            cls.getPackage().getName(),
            resourceBundleName
        ),
        locale
    );
    resourceBundles.put(resourceBundleName, resourceBundle);
  }

  /**
   * Remove a specified resource bundle.
   *
   * @param resourceBundleName Resource bundle to remove.
   */
  public static synchronized void removeResourceBundle(String resourceBundleName) {
    // Cannot internationalize as the required I18N string cannot be found
    // without adding the relevant resource bundle.
    if (logger.isDebugEnabled()) {
      logger.debug(
              String.format(
                      "Removing resource bundle - %s",
                      resourceBundleName
              )
      );
    }

    ResourceBundle removedResourceBundle = resourceBundles.remove(resourceBundleName);
    if (removedResourceBundle == null) {
      throw new MissingResourceException(
          String.format("Can't find resource bundle %s", resourceBundleName),
          I18nUtility.class.getName(),
          resourceBundleName
      );
    }
  }

  /**
   * Gets a list of the added resource bundle names.
   *
   * @return a list of the added resource bundle names.
   */
  public static synchronized List<String> getResourceBundlesNames() {
    return new ArrayList<>(resourceBundles.keySet());
  }
}
