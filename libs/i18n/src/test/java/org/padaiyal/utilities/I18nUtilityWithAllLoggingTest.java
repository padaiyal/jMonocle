package org.padaiyal.utilities;

import java.io.IOException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;

/**
 * Test class for I18nUtility with all logging levels enabled.
 */
public class I18nUtilityWithAllLoggingTest extends I18nUtilityTest {

  /**
   * Enables all the logging levels and runs the tests again.
   *
   * @throws IOException When there is an issue loading the properties file.
   */
  @BeforeAll
  static void setUpClass() throws IOException {
    loggingLevel = Level.ALL;
    Configurator.setAllLevels("", loggingLevel);
    I18nUtilityTest.setUpClass();
  }
}
