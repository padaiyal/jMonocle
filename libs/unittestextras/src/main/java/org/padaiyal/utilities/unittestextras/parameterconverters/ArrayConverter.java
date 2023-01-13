package org.padaiyal.utilities.unittestextras.parameterconverters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.padaiyal.utilities.I18nUtility;


/**
 * Converts a string into arrays of different types.
 */
public class ArrayConverter extends SimpleArgumentConverter {

  /**
   * Initializes all dependant values.
   */
  public static Runnable dependantValuesInitializer = () -> I18nUtility.addResourceBundle(
      ArrayConverter.class,
      ArrayConverter.class.getSimpleName(),
      Locale.US
  );

  static {
    dependantValuesInitializer.run();
  }


  /**
   * Private constructor.
   */
  private ArrayConverter() {

  }

  /**
   * Converts a string into an array of specified types.
   *
   * @param arrayObject The string to convert.
   * @param targetType  The type of array to convert it to.
   * @throws ArgumentConversionException When the provided targetType is not supported.
   */
  public static Object[] convertStringToArray(Object arrayObject, Class<?> targetType) {
    if (arrayObject == null) {
      return null;

    }
    Objects.requireNonNull(targetType);
    if (!(arrayObject instanceof String)) {
      throw new ArgumentConversionException(
          I18nUtility.getFormattedString(
              "ArrayConverter.error.conversionNotSupported",
              arrayObject.getClass().getName(), targetType.getName()
          )
      );
    }
    Object[] result = ((String) arrayObject).split(",");
    if (String[].class.isAssignableFrom(targetType)) {
      return result;
    }

    HashMap<Class<?>, Function<String, ?>> map = new LinkedHashMap<>();
    map.put(Boolean[].class, Boolean::valueOf);
    map.put(Byte[].class, Byte::parseByte);
    map.put(Short[].class, Short::parseShort);
    map.put(Integer[].class, Integer::parseInt);
    map.put(Long[].class, Long::parseLong);
    map.put(Float[].class, Float::parseFloat);
    map.put(Double[].class, Double::parseDouble);

    if (!map.containsKey(targetType)) {
      throw new ArgumentConversionException(
          I18nUtility.getFormattedString(
              "ArrayConverter.error.conversionNotSupported",
              arrayObject.getClass().getName(),
              targetType.getName()
          ));
    }

    return Arrays.stream(result)
        .parallel()
        .map(element -> map.get(targetType).apply(element.toString()))
        .toArray();
  }


  /**
   * Converts a string into an array of specified types.
   *
   * @param source     The string to convert.
   * @param targetType The type of array to convert it to.
   * @throws ArgumentConversionException When the provided targetType is not supported.
   */
  @Override
  protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
    return convertStringToArray(source, targetType);
  }

}
