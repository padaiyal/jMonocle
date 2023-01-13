package org.padaiyal.utilities.commandline.exceptions;

import org.padaiyal.utilities.I18nUtility;
import org.padaiyal.utilities.commandline.abstractions.CommandLine;

/**
 * Represents an exception where a command line executable is not found.
 */
public class CommandLineNotFoundException extends Exception {

  /**
   * Thrown when a specified command line is not found.
   *
   * @param commandLine Type of command line which is not found.
   */
  public CommandLineNotFoundException(CommandLine commandLine) {
    super(
        I18nUtility.getFormattedString(
            "CommandLineUtility.exception.CommandLineNotFoundException",
            commandLine
        )
    );
  }
}
