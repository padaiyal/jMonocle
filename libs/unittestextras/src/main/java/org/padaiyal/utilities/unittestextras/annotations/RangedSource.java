package org.padaiyal.utilities.unittestextras.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Annotation used for providing a range of numbers to a parameterized test.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(RangedArgumentsProvider.class)
public @interface RangedSource {

  /**
   * The starting value in the range. It defaults to 0 and it is inclusive.
   *
   * @return The minimum value in the range.
   */
  double start() default 0;

  /**
   * The end value in the range. It defaults to Double.MAX_VALUE and it is exclusive.
   *
   * @return The maximum value in the range.
   */
  double end() default Double.MAX_VALUE;

  /**
   * The step used in the range. It defaults to 1.
   *
   * @return The step used in the range.
   */
  double step() default 1;

  /**
   * The expected numeric type to provide to the parameterized test.
   *
   * @return The expected numeric type to provide to the parameterized test.
   */
  NumericType type() default NumericType.INTEGER;
}

