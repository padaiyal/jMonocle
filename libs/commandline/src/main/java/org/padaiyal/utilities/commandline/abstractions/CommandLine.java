package org.padaiyal.utilities.commandline.abstractions;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import org.padaiyal.utilities.PropertyUtility;
import org.padaiyal.utilities.commandline.CommandLineUtility;
import org.padaiyal.utilities.commandline.exceptions.CommandLineNotFoundException;

/**
 * Enum to represent the type of command line to use to execute commands.
 */
public enum CommandLine {
  /** BASH. */
  BASH,
  /** DOS COMMANDLINE. */
  CMD,
  /** C SHELL. */
  CSH,
  /** K SHELL. */
  KSH,
  /** POWERSHELL. */
  POWERSHELL,
  /** SHELL. */
  SH,
  /** TC SHELL. */
  TCSH,
  /** Z SHELL. */
  ZSH;

  /**
   * Returns the switch to use to execute a command using a specified command line.
   *
   * @param commandLine       Type of command line for which a template is to be retrieved and
   *                          returned.
   * @return                  The switch to use to execute a command in the specified command line.
   */
  public static String getCommandLineSwitch(CommandLine commandLine) {
    return PropertyUtility.getProperty("CommandLine.switch." + commandLine.toString());
  }

  /**
   * Given the operating system of this machine, this method gets the path of the command line
   * executable denoted by this enum object.
   *
   * @param operatingSystem               Operating system of the machine in which this code is
   *                                      executed.
   * @return                              The path to the command line represented by this enum
   *                                      object.
   * @throws InterruptedException         If the process of identifying the command line
   *                                      executable is interrupted.
   * @throws TimeoutException             If the process of identifying the command line
   *                                      executable times out.
   * @throws IOException                  If there is an issue with identifying the command line
   *                                      executable.
   * @throws CommandLineNotFoundException If the command line executable cannot be found.
   */
  public String getCommandLineLocation(OperatingSystem operatingSystem)
      throws InterruptedException,
      TimeoutException,
      IOException,
      CommandLineNotFoundException {
    String[] splitCommand = new String[]{
        operatingSystem.getCommandStringForDetectingCommandLines(),
        this.toString().toLowerCase()
      };
    Response response = CommandLineUtility.executeCommand(
        splitCommand,
        Duration.ofSeconds(
            PropertyUtility.getTypedProperty(Long.class, "CommandLineUtility.timeout.seconds")
        )
    );
    String commandLinePath = null;
    if (response.getReturnCode() == 0) {
      // If multiple command line paths are found, choose the first one.
      commandLinePath = Arrays.stream(
          response.getOutput(StdType.STDOUT)
              .trim()
              .split(PropertyUtility.getProperty("CommandLineUtility.path.separator.regex"))
      )
          .findFirst()
          .orElse(null);
    }
    if (commandLinePath != null) {
      commandLinePath = commandLinePath.trim();
    } else {
      throw new CommandLineNotFoundException(this);
    }
    return commandLinePath;
  }
}
