package org.padaiyal.utilities.unittestextras.annotations;

import java.util.Locale;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.padaiyal.utilities.I18nUtility;

/**
 * Provider class used to create the stream of ranged values.
 */
public class RangedArgumentsProvider implements ArgumentsProvider,
    AnnotationConsumer<RangedSource> {

  static {
    I18nUtility.addResourceBundle(
        RangedArgumentsProvider.class,
        RangedArgumentsProvider.class.getSimpleName(),
        Locale.US
    );
  }

  /**
   * Start of the range. It is inclusive.
   */
  private double start;

  /**
   * End of the range. It is exclusive.
   */
  private double end;

  /**
   * Step of the range.
   */
  private double step;

  /**
   * The numeric type to provide to the parameterized test.
   */
  private NumericType type;

  /**
   * Provides a stream of the specified range of numbers.
   *
   * @param extensionContext The context in which the test is being executed.
   * @return a stream if the specified range of numbers.
   */
  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
    long count = (long) Math.ceil((this.end - this.start) / this.step);
    return DoubleStream.iterate(this.start, nextElement -> nextElement + this.step)
        .parallel()
        .limit(count)
        .boxed()
        .map(element -> {
          if (this.type == NumericType.INTEGER) {
            return element.intValue();
          } else if (this.type == NumericType.FLOAT) {
            return element.floatValue();
          } else if (this.type == NumericType.LONG) {
            return element.longValue();
          }
          return element;
        }).map(Arguments::of);
  }

  /**
   * Assigns the values from the ranged annotation to the corresponding private attributes.
   *
   * @param rangedSource The ranged annotation to use.
   */
  @Override
  public void accept(RangedSource rangedSource) {
    if (rangedSource.start() > rangedSource.end() && rangedSource.step() > 0) {
      throw new IllegalArgumentException(
          I18nUtility.getFormattedString(
              "RangedArgumentsProvider.error.invalidEndAndStartOfIncreasingRange",
              rangedSource.start(),
              rangedSource.end(),
              rangedSource.step()
          )
      );
    }
    this.start = rangedSource.start();
    this.end = rangedSource.end();
    this.step = rangedSource.step();
    this.type = rangedSource.type();
  }
}
