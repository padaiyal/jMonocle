package org.padaiyal.utilities.vaidhiyar.abstractions;

import java.io.IOException;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeAll;

/**
 * Test class for CpuLoadGeneratorTest with all logging levels enabled.
 */
public class CpuLoadGeneratorWithAllLoggingTest extends CpuLoadGeneratorTest {

  /**
   * Sets the desired log levels.
   */
  @BeforeAll
  static void prepare() throws IOException {
    loggingLevel = Level.ALL;
    CpuLoadGeneratorTest.prepare();
  }
}

