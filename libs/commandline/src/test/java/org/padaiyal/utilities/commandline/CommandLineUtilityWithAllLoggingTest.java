package org.padaiyal.utilities.commandline;

import java.io.IOException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;

/**
 * Test class for CommandLineUtilityTest with all logging levels enabled.
 */
public class CommandLineUtilityWithAllLoggingTest extends CommandLineUtilityTest {

  /**
   * Retrieves the required information from test.properties file.
   *
   * @throws IOException When there is an issue loading the properties file.
   */
  @BeforeAll
  static void prepare() throws IOException {
    loggingLevel = Level.ALL;
    Configurator.setAllLevels("", loggingLevel);
    CommandLineUtilityTest.prepare();
  }
}

