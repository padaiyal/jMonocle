package org.padaiyal.utilities.commandline;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.padaiyal.utilities.I18nUtility;
import org.padaiyal.utilities.PropertyUtility;
import org.padaiyal.utilities.commandline.abstractions.Command;
import org.padaiyal.utilities.commandline.abstractions.CommandLine;
import org.padaiyal.utilities.commandline.abstractions.OperatingSystem;
import org.padaiyal.utilities.commandline.abstractions.Response;
import org.padaiyal.utilities.commandline.abstractions.StdType;
import org.padaiyal.utilities.commandline.exceptions.CommandLineNotFoundException;

/**
 * Used to execute commands in a specified or auto-detected terminal/shell.
 */
public final class CommandLineUtility {

  /**
   * Used to log information and errors for this class.
   */
  private static final Logger logger = LogManager.getLogger(CommandLineUtility.class);
  /**
   * Retrieves and stores the operating system of the machine in which this code is executing.
   */
  private static final OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
  /**
   * Used to store the path to all identified command line executables in this machine.
   */
  private static final HashMap<CommandLine, Path> commandLines = new HashMap<>();
  /**
   * Used to prevent simultaneous initialization of property file, commandLines etc.
   */
  private static final ReentrantLock dependantValuesInitializationLock = new ReentrantLock();

  /**
   * Flag to denote if dependant values have been initialized.
   */
  private static boolean areDependantValuesInitialized = false;

  static {
    initializeDependantValues();
  }

  /**
   * Empty private constructor as this utility class is not meant to be used as an instance.
   */
  private CommandLineUtility() {
  }

  /**
   * Returns the current value of areDependantValuesInitialized.
   *
   * @return true if dependent values are initialized, else false.
   */
  public static Boolean areDependantValuesInitialized() {
    return areDependantValuesInitialized;
  }

  /**
   * Try acquiring the dependant values initialization lock.
   *
   * @return true if acquiring the lock was successful, else false.
   */
  static boolean tryAcquiringDependantValuesInitializationLock() {
    return dependantValuesInitializationLock.tryLock();
  }

  /**
   * Check if the dependant values initialization lock is locked or available.
   *
   * @return true if the lock is locked, else false.
   */
  static boolean isDependantValuesInitializationLockLocked() {
    return dependantValuesInitializationLock.isLocked();
  }

  /**
   * Initialize static variables needed for this utility.
   */
  public static void initializeDependantValues() {
    boolean isLockAttemptSuccessful;
    if (!isDependantValuesInitializationLockLocked()) {
      isLockAttemptSuccessful = tryAcquiringDependantValuesInitializationLock();
      if (isLockAttemptSuccessful) {
        try {
          PropertyUtility.addPropertyFile(
              CommandLineUtility.class,
              CommandLineUtility.class.getSimpleName() + ".properties"
          );

          I18nUtility.addResourceBundle(
              CommandLineUtility.class,
              CommandLineUtility.class.getSimpleName(),
              Locale.US
          );

          for (CommandLine commandLine : operatingSystem
              .getSupportedTypeOfCommandLines()) {
            try {
              String commandLinePath = commandLine.getCommandLineLocation(operatingSystem);
              commandLines.put(
                  commandLine,
                  Paths.get(commandLinePath)
              );
            } catch (
            CommandLineNotFoundException
                | InterruptedException
                | TimeoutException
                | IOException e
            ) {
              logger.warn(e);
            }
          }
          areDependantValuesInitialized = true;
        } catch (IOException e) {
          logger.error(e);
        } finally {
          dependantValuesInitializationLock.unlock();
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(
                  I18nUtility.getString(
                          "CommandLineUtility.dependantValuesInitializationLock.unableToAcquire"
                  )
          );
        }
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(
                I18nUtility.getString(
                        "CommandLineUtility.dependantValuesInitializationLock.alreadyLocked"
                )
        );
      }
    }
  }

  /**
   * Executes the specified command.
   *
   * @param splitCommand          Command to execute.
   * @param timeOutDuration       Time out for the command execution.
   * @return                      The response of the command.
   * @throws IOException          Thrown if there is an issue executing the command.
   * @throws InterruptedException Thrown if the execution of the command is interrupted.
   * @throws TimeoutException     Thrown f the command execution exceeds specified timeout.
   */
  public static synchronized Response executeCommand(
      String[] splitCommand,
      Duration timeOutDuration
  ) throws IOException, InterruptedException, TimeoutException {
    Objects.requireNonNull(
        splitCommand,
        I18nUtility.getFormattedString(
            "CommandLineUtility.input.validation.nonNull",
            "Command"
        )
    );
    Objects.requireNonNull(
        timeOutDuration,
        I18nUtility.getFormattedString(
            "CommandLineUtility.input.validation.nonNull",
            "Duration"
        )
    );

    if (logger.isInfoEnabled()) {
      logger.info(
              I18nUtility.getString("CommandLineUtility.executing"),
              Arrays.toString(splitCommand)
      );
    }

    ProcessBuilder processBuilder = new ProcessBuilder(splitCommand);
    final Instant executionStartTimestamp = Instant.now();
    Process process = processBuilder.start();
    boolean timedOut = !process.waitFor(timeOutDuration.getSeconds(), TimeUnit.SECONDS);
    if (timedOut) {
      Duration executionDuration = Duration.between(executionStartTimestamp, Instant.now());
      throw new TimeoutException(
          I18nUtility.getFormattedString("CommandLineUtility.exception.TimeoutException",
              Arrays.toString(splitCommand),
              executionDuration.getSeconds(),
              timeOutDuration.getSeconds()
          )
      );
    }
    HashMap<StdType, String> output = new HashMap<>();
    final int returnCode = process.waitFor();
    final Instant executionEndTimestamp = Instant.now();
    output.put(StdType.STDOUT,
        StreamUtility.convertInputStreamToString(process.getInputStream()));
    output.put(StdType.STDERR,
        StreamUtility.convertInputStreamToString(process.getErrorStream()));
    process.destroy();
    return new Response(returnCode, output, executionStartTimestamp, executionEndTimestamp);
  }

  /**
   * Executes the specified command.
   *
   * @param command               Command to execute.
   * @param commandLine           Type of command line to use to execute the command.
   * @param timeOutDuration       Time out for the command execution.
   * @return                      The response of the command.
   * @throws IOException          Thrown if there is an issue executing the command.
   * @throws InterruptedException Thrown if the execution of the command is interrupted.
   * @throws TimeoutException     Thrown if the command execution exceeds specified timeout.
   */
  public static synchronized Response executeCommand(
      String command,
      CommandLine commandLine,
      Duration timeOutDuration
  ) throws IOException, InterruptedException, TimeoutException {
    Objects.requireNonNull(
        command,
        I18nUtility.getFormattedString(
            "CommandLineUtility.input.validation.nonNull",
            "Command"
        )
    );
    Objects.requireNonNull(
        timeOutDuration,
        I18nUtility.getFormattedString(
            "CommandLineUtility.input.validation.nonNull",
            "Duration"
        )
    );

    String[] splitCommand;
    splitCommand = new String[]{
        // Path to command line
        commandLines.get(commandLine)
            .toAbsolutePath()
            .toString(),
        // Command line switch
        CommandLine.getCommandLineSwitch(commandLine),
        // Command
        command.replaceAll("\"", "\\\"")
      };

    return executeCommand(splitCommand, timeOutDuration);
  }

  /**
   * Executes the specified command.
   *
   * @param command                       Command to execute.
   * @param timeOutDuration               Time out for the command execution.
   * @return                              The response of the command.
   * @throws IOException                  Thrown if there is an issue executing the command.
   * @throws InterruptedException         Thrown if the execution of the command is interrupted.
   * @throws TimeoutException             Thrown if the command execution exceeds specified timeout.
   * @throws CommandLineNotFoundException Thrown if the command line expected for the 
   *                                      operating system is not available.
   */
  public static Response executeCommand(
      Command command,
      Duration timeOutDuration
  ) throws IOException, InterruptedException, TimeoutException, CommandLineNotFoundException {
    CommandLine[] supportedCommandLines
        = operatingSystem.getSupportedTypeOfCommandLines();
    Response response = null;
    int index = 0;
    boolean validResponse = false;
    while (!validResponse) {
      response = executeCommand(command, supportedCommandLines[index], timeOutDuration);
      index++;
      validResponse = true;
    }

    return response;
  }

  /**
   * Executes the specified command.
   *
   * @param command                       Command to execute.
   * @return                              The response of the command.
   * @throws IOException                  Thrown if there is an issue executing the command.
   * @throws InterruptedException         Thrown if the execution of the command is interrupted.
   * @throws TimeoutException             Thrown if the command execution exceeds specified timeout.
   * @throws CommandLineNotFoundException Thrown if the command line expected for the 
   *                                      operating system is not available.
   */
  public static Response executeCommand(Command command)
      throws IOException,
      InterruptedException,
      TimeoutException, 
      CommandLineNotFoundException {
    return executeCommand(
        command,
        Duration.ofSeconds(
            PropertyUtility.getTypedProperty(
                Long.class,
                "CommandLineUtility.timeout.seconds"
            )
        )
    );
  }

  /**
   * Executes the specified command.
   *
   * @param commandString                 Command string to execute.
   * @param timeOutDuration               Command time out duration.
   * @return                              The response of the command.
   * @throws IOException                  Thrown if there is an issue executing the command.
   * @throws InterruptedException         Thrown if the execution of the command is interrupted.
   * @throws TimeoutException             Thrown if the command execution exceeds specified timeout.
   * @throws CommandLineNotFoundException Thrown if the command line expected for the 
   *                                      operating system is not available.
   */
  public static Response executeCommand(String commandString, Duration timeOutDuration)
      throws IOException,
      InterruptedException,
      TimeoutException,
      CommandLineNotFoundException {
    Command command = new Command();
    Arrays.stream(operatingSystem.getSupportedTypeOfCommandLines())
        .forEach(supportedTypeOfCommandLine
            -> command.setCommand(supportedTypeOfCommandLine, commandString));
    return executeCommand(command, timeOutDuration);
  }

  /**
   * Executes the specified command.
   *
   * @param commandString                 Command string to execute.
   * @return                              The response of the command.
   * @throws IOException                  Thrown if there is an issue executing the command.
   * @throws InterruptedException         Thrown if the execution of the command is interrupted.
   * @throws TimeoutException             Thrown if the command execution exceeds specified timeout.
   * @throws CommandLineNotFoundException Thrown if the command line expected for the 
   *                                      operating system is not available.
   */
  public static Response executeCommand(String commandString)
      throws IOException,
      InterruptedException,
      TimeoutException,
      CommandLineNotFoundException {
    return executeCommand(
        commandString,
        Duration.ofSeconds(
            PropertyUtility.getTypedProperty(
                Long.class,
                "CommandLineUtility.timeout.seconds"
            )
        )
    );
  }

  /**
   * Executes the specified command.
   *
   * @param command                       Command string to execute.
   * @param commandLine                   The type of command line with which the command needs to
   *                                      be executed.
   * @param timeOutDuration               Time out for the command execution.
   * @return                              The response of the command.
   * @throws IOException                  Thrown if there is an issue executing the command.
   * @throws InterruptedException         Thrown if the execution of the command is interrupted.
   * @throws TimeoutException             Thrown if the command execution exceeds specified timeout.
   * @throws CommandLineNotFoundException Thrown if the specified command line is not available.
   */
  public static Response executeCommand(
      Command command,
      CommandLine commandLine,
      Duration timeOutDuration
  ) throws IOException, 
      InterruptedException, 
      CommandLineNotFoundException, 
      TimeoutException {
    Objects.requireNonNull(
        command,
        I18nUtility.getFormattedString(
            "CommandLineUtility.input.validation.nonNull",
            "Command"
        )
    );
    Objects.requireNonNull(
        commandLine,
        I18nUtility.getFormattedString(
            "CommandLineUtility.input.validation.nonNull",
            "TypeOfCommandLine"
        )
    );
    Objects.requireNonNull(
        timeOutDuration,
        I18nUtility.getFormattedString(
            "CommandLineUtility.input.validation.nonNull",
            "Duration"
        )
    );

    if (commandLines.containsKey(commandLine)) {
      return executeCommand(
          command.getCommand(commandLine),
          commandLine,
          timeOutDuration
      );
    } else {
      throw new CommandLineNotFoundException(commandLine);
    }
  }

  /**
   * Executes the specified command.
   *
   * @param command                       Command to execute.
   * @param commandLine                   The type of command line in which the command needs to
   *                                      be executed in.
   * @return                              The response of the command.
   * @throws IOException                  Thrown if there is an issue executing the command.
   * @throws InterruptedException         Thrown if the execution of the command is interrupted.
   * @throws TimeoutException             Thrown f the command execution exceeds specified timeout.
   * @throws CommandLineNotFoundException Thrown if the specified command line is not available.
   */
  public static Response executeCommand(
      Command command,
      CommandLine commandLine
  ) throws IOException, 
      InterruptedException, 
      TimeoutException, 
      CommandLineNotFoundException {
    return executeCommand(
        command,
        commandLine,
        Duration.ofSeconds(
            PropertyUtility.getTypedProperty(
                Long.class,
                "CommandLineUtility.timeout.seconds"
            )
        )
    );
  }

}
