package org.padaiyal.utilities.unittestextras.parameterconverters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;


/**
 * Converts a string into arrays of different types.
 */
public class ArrayConverter extends SimpleArgumentConverter {

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
   * @return An array representing the input string.
   * @throws ArgumentConversionException When the provided targetType is not supported.
   */
  public static Object[] convertStringToArray(Object arrayObject, Class<?> targetType) {
    if (arrayObject == null) {
      return null;

    }
    Objects.requireNonNull(targetType);
    if (!(arrayObject instanceof String)) {
      throw new ArgumentConversionException(
          String.format(
              "Conversion from %s to %s not supported.",
              arrayObject.getClass().getName(),
              targetType.getName()
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
          String.format(
              "Conversion from %s to %s not supported.",
              arrayObject.getClass().getName(),
              targetType.getName()
          )
      );
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
