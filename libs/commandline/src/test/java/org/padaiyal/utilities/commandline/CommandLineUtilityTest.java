package org.padaiyal.utilities.commandline;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.padaiyal.utilities.I18nUtility;
import org.padaiyal.utilities.PropertyUtility;
import org.padaiyal.utilities.commandline.abstractions.Command;
import org.padaiyal.utilities.commandline.abstractions.CommandLine;
import org.padaiyal.utilities.commandline.abstractions.OperatingSystem;
import org.padaiyal.utilities.commandline.abstractions.Response;
import org.padaiyal.utilities.commandline.exceptions.CommandLineNotFoundException;

/**
 * Tests for CommandLineUtility.
 */
class CommandLineUtilityTest {

  /**
   * Normal test command.
   */
  private static Command terminatingCommand;
  /**
   * Non terminating test command.
   */
  private static Command nonTerminatingCommand;
  /**
   * Test command timeout duration.
   */
  private static Duration timeoutDuration;
  /**
   * Operating system of test machine.
   */
  private static OperatingSystem operatingSystem;
  /**
   * Type of command line to test.
   */
  private static CommandLine commandLine;
  /**
   * Logger object.
   */
  private final Logger logger = LogManager.getLogger(CommandLineUtilityTest.class);

  /**
   * Prepares the static variables used for the tests.
   *
   * @throws IOException If there is an issue adding the property file.
   */
  @BeforeAll
  static void prepare() throws IOException {
    PropertyUtility.addPropertyFile(
        CommandLineUtilityTest.class,
        CommandLineUtilityTest.class.getSimpleName() + ".properties"
    );

    I18nUtility.addResourceBundle(
        CommandLineUtilityTest.class,
        CommandLineUtilityTest.class.getSimpleName(),
        Locale.US
    );

    terminatingCommand = new Command();
    nonTerminatingCommand = new Command();
    Stream.of(
        CommandLine.BASH,
        CommandLine.CMD,
        CommandLine.POWERSHELL,
        CommandLine.ZSH
    ).forEach(commandLine -> {
      terminatingCommand.setCommand(
          commandLine,
          PropertyUtility.getProperty(
              "CommandLineUtilityTest.terminatingCommand." + commandLine.name()
          )
      );
      nonTerminatingCommand.setCommand(
          commandLine,
          PropertyUtility.getProperty(
              "CommandLineUtilityTest.nonTerminatingCommand." + commandLine.name()
          )
      );
    });

    timeoutDuration = Duration.ofSeconds(
        PropertyUtility.getTypedProperty(
            Long.class,
            "CommandLineUtilityTest.timeout.seconds"
        )
    );

    operatingSystem = OperatingSystem.getOperatingSystem();

    switch (operatingSystem) {
      case WINDOWS:
        commandLine = CommandLine.CMD;
        break;
      case LINUX:
        commandLine = CommandLine.BASH;
        break;
      case MAC_OS_X:
        commandLine = CommandLine.ZSH;
        break;
      default:
        commandLine = null;
    }
  }

  /**
   * Tests CommandLineUtility::executeCommand(String).
   *
   * @throws IOException                  If there is an issue executing the command.
   * @throws InterruptedException         If the command execution is interrupted.
   * @throws TimeoutException             If the command execution times out.
   * @throws CommandLineNotFoundException If the command line to be used to execute the command
   *                                      is not found.
   */
  @Test
  void testExecuteCommandString()
      throws IOException,
      InterruptedException,
      TimeoutException,
      CommandLineNotFoundException {

    // Test with a terminating command.
    String commandString = terminatingCommand.getCommand(commandLine);
    Response response = CommandLineUtility.executeCommand(commandString);
    Assertions.assertEquals(0, response.getReturnCode());
    Assertions.assertNotNull(response.getExecutionDuration());
    Assertions.assertNotNull(response.getExecutionStartTimestamp());
    Assertions.assertNotNull(response.getExecutionEndTimestamp());

    // Test with non terminating command.
    String nonTerminatingCommandString = nonTerminatingCommand.getCommand(commandLine);
    Assertions.assertThrows(
        TimeoutException.class,
        () -> CommandLineUtility.executeCommand(nonTerminatingCommandString)
    );

    // Test with a null command.
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((String) null)
    );
  }

  /**
   * Tests CommandLineUtility::executeCommand(String, Duration).
   *
   * @throws IOException                  If there is an issue executing the command.
   * @throws InterruptedException         If the command execution is interrupted.
   * @throws TimeoutException             If the command execution times out.
   * @throws CommandLineNotFoundException If the command line to be used to execute the command
   *                                      is not found.
   */
  @Test
  void testExecuteCommandStringWithTimeout()
      throws IOException,
      InterruptedException,
      TimeoutException,
      CommandLineNotFoundException {

    // Test with terminating command.
    String commandString = terminatingCommand.getCommand(commandLine);
    Response response = CommandLineUtility.executeCommand(commandString, timeoutDuration);
    Assertions.assertEquals(0, response.getReturnCode());

    // Test with non terminating command.
    String nonTerminatingCommandString = nonTerminatingCommand.getCommand(commandLine);
    Assertions.assertThrows(
        TimeoutException.class,
        () -> CommandLineUtility.executeCommand(nonTerminatingCommandString, timeoutDuration)
    );

    // Test with null inputs.
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((String) null, null)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand(commandString, null)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((String) null, timeoutDuration)
    );
  }

  /**
   * Tests CommandLineUtility::executeCommand(Command).
   *
   * @throws IOException                  If there is an issue executing the command.
   * @throws InterruptedException         If the command execution is interrupted.
   * @throws TimeoutException             If the command execution times out.
   * @throws CommandLineNotFoundException If the command line to be used to execute the command
   *                                      is not found.
   */
  @Test
  void testExecuteCommand()
      throws IOException,
      InterruptedException,
      TimeoutException,
      CommandLineNotFoundException {
    // Test with terminating command.
    Response response = CommandLineUtility.executeCommand(terminatingCommand);
    Assertions.assertEquals(0, response.getReturnCode());

    // Test with a null input.
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((Command) null)
    );
  }

  /**
   * Tests CommandLineUtility::executeCommand(Command, Duration).
   *
   * @throws IOException                  If there is an issue executing the command.
   * @throws InterruptedException         If the command execution is interrupted.
   * @throws TimeoutException             If the command execution times out.
   * @throws CommandLineNotFoundException If the command line to be used to execute the command
   *                                      is not found.
   */
  @Test
  void testExecuteCommandWithTimeout()
      throws IOException,
      InterruptedException,
      TimeoutException,
      CommandLineNotFoundException {

    // Test with terminating command.
    Response response = CommandLineUtility.executeCommand(terminatingCommand, timeoutDuration);
    Assertions.assertEquals(0, response.getReturnCode());

    // Test with non terminating command.
    Assertions.assertThrows(
        TimeoutException.class,
        () -> CommandLineUtility.executeCommand(nonTerminatingCommand, timeoutDuration)
    );

    // Test with null inputs.
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((Command) null, (Duration) null)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand(terminatingCommand, (Duration) null)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((Command) null, timeoutDuration)
    );
  }

  /**
   * Tests CommandLineUtility::executeCommand(Command, CommandLine).
   *
   * @throws IOException                  If there is an issue executing the command.
   * @throws InterruptedException         If the command execution is interrupted.
   * @throws TimeoutException             If the command execution times out.
   * @throws CommandLineNotFoundException If the command line to be used to execute the command
   *                                      is not found.
   */
  @Test
  void testExecuteCommandWithSpecificCommandLine()
      throws IOException,
      InterruptedException,
      TimeoutException,
      CommandLineNotFoundException {
    // Test with terminating command.
    Response response = CommandLineUtility.executeCommand(terminatingCommand, commandLine);
    Assertions.assertEquals(0, response.getReturnCode());

    // Test with null inputs.
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand(null, (CommandLine) null)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand(terminatingCommand, (CommandLine) null)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand(null, commandLine)
    );
  }

  /**
   * Tests CommandLineUtility::executeCommand(Command, CommandLine, Duration).
   *
   * @throws IOException                  If there is an issue executing the command.
   * @throws InterruptedException         If the command execution is interrupted.
   * @throws TimeoutException             If the command execution times out.
   * @throws CommandLineNotFoundException If the command line to be used to execute the command
   *                                      is not found.
   */
  @Test
  void testExecuteCommandWithSpecificCommandLineAndTimeout()
      throws IOException,
      InterruptedException,
      TimeoutException,
      CommandLineNotFoundException {
    // Test with terminating command.
    Response response = CommandLineUtility.executeCommand(
        terminatingCommand,
        commandLine,
        timeoutDuration
    );
    Assertions.assertEquals(0, response.getReturnCode());

    // Test with non terminating command.
    Assertions.assertThrows(
        TimeoutException.class,
        () -> CommandLineUtility.executeCommand(
            nonTerminatingCommand,
            commandLine,
            timeoutDuration
        )
    );

    // Test with null inputs.
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((Command) null, null, null)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((Command) null, commandLine, timeoutDuration)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand(terminatingCommand, null, timeoutDuration)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand(terminatingCommand, commandLine, null)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((Command) null, null, timeoutDuration)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand(terminatingCommand, null, null)
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> CommandLineUtility.executeCommand((Command) null, commandLine, null)
    );
  }

  /**
   * Test executing a command using a Command object with no command string supplied for the
   * specified command line.
   */
  @Test
  void testExecuteCommandForUnspecifiedCommandLine() {
    Assertions.assertThrows(
        CommandLineNotFoundException.class,
        () -> {
          Command command = new Command();
          CommandLineUtility.executeCommand(command, CommandLine.BASH);
        }
    );
  }

  /**
   * Given an operating system, return a set of unsupported command lines.
   *
   * @param operatingSystem An operating system.
   * @return                A set of unsupported command lines.
   */
  private Set<CommandLine> getUnsupportedTypeOfCommandLines(OperatingSystem operatingSystem) {
    Set<CommandLine> availableCommandLines = new HashSet<>(
        Arrays.asList(
            CommandLine.values()
        )
    );
    HashSet<CommandLine> supportedCommandLines = new HashSet<>(
        Arrays.asList(
            operatingSystem.getSupportedTypeOfCommandLines()
        )
    );
    HashSet<CommandLine> unsupportedCommandLines = new HashSet<>(
        availableCommandLines);
    unsupportedCommandLines.removeAll(supportedCommandLines);

    return unsupportedCommandLines;
  }

  /**
   * Test executing commands against unsupported command lines.
   */
  @Test
  void testExecuteCommandForInvalidCommandLine() {
    Set<CommandLine> unsupportedCommandLines = getUnsupportedTypeOfCommandLines(operatingSystem);

    if (logger.isInfoEnabled()) {
      logger.info(
              I18nUtility.getString("CommandLineUtilityTest.unsupportedCommandLines"),
              operatingSystem,
              unsupportedCommandLines
      );
    }

    for (CommandLine unsupportedCommandLine : unsupportedCommandLines) {
      Assertions.assertThrows(
          CommandLineNotFoundException.class,
          () -> CommandLineUtility.executeCommand(
              terminatingCommand,
              unsupportedCommandLine,
              Duration.ofSeconds(5)
          )
      );
    }
  }

  /**
   * Test initializing dependant values when the lock cannot be acquired.
   */
  @Test
  void testInitializingDependantValuesWhenLockCannotBeAcquired() {
    try (
        MockedStatic<CommandLineUtility> commandLineUtilityMock
            = Mockito.mockStatic(CommandLineUtility.class)
    ) {
      commandLineUtilityMock.when(CommandLineUtility::tryAcquiringDependantValuesInitializationLock)
          .thenReturn(false);
      commandLineUtilityMock.when(CommandLineUtility::initializeDependantValues)
          .thenCallRealMethod();

      CommandLineUtility.initializeDependantValues();

      Assertions.assertFalse(CommandLineUtility.areDependantValuesInitialized());
    }

    CommandLineUtility.initializeDependantValues();
    Assertions.assertTrue(CommandLineUtility.areDependantValuesInitialized());
  }

  /**
   * Test initializing dependant values when the lock is locked.
   */
  @Test
  void testInitializingDependantValuesWhenLockIsAlreadyLocked() {
    try (
        MockedStatic<CommandLineUtility> commandLineUtilityMock
            = Mockito.mockStatic(CommandLineUtility.class)
    ) {
      commandLineUtilityMock.when(CommandLineUtility::isDependantValuesInitializationLockLocked)
          .thenReturn(true);
      commandLineUtilityMock.when(CommandLineUtility::initializeDependantValues)
          .thenCallRealMethod();

      CommandLineUtility.initializeDependantValues();

      Assertions.assertFalse(CommandLineUtility.areDependantValuesInitialized());
    }

    CommandLineUtility.initializeDependantValues();
    Assertions.assertTrue(CommandLineUtility.areDependantValuesInitialized());
  }

  /**
   * Test silent failure when adding a property file throws an IOException.
   */
  @Test
  void testIoExceptionThrownWhenAddingPropertyFile() {
    try (
        MockedStatic<PropertyUtility> propertyUtilityMock
            = Mockito.mockStatic(PropertyUtility.class)
    ) {
      propertyUtilityMock.when(
          () -> PropertyUtility.addPropertyFile(
              ArgumentMatchers.any(Class.class),
              ArgumentMatchers.anyString()
          )
      ).thenThrow(IOException.class);

      Assertions.assertDoesNotThrow(CommandLineUtility::initializeDependantValues);
    }
  }
}
