package org.padaiyal.utilities.vaidhiyar;

import java.io.IOException;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeAll;

/**
 * Test class for JvmUtilityTest with all logging levels enabled.
 */
public class JvmUtilityWithAllLoggingTest extends JvmUtilityTest {

  /**
   * Sets the desired log levels.
   */
  @BeforeAll
  public static void setUp() throws IOException {
    loggingLevel = Level.ALL;
    JvmUtilityTest.setUp();
  }
}
