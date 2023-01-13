package org.padaiyal.utilities.commandline.abstractions;

import java.util.HashMap;
import org.padaiyal.utilities.commandline.exceptions.CommandLineNotFoundException;

/**
 * Represents the command that needs to be executed along with the equivalent commands for
 * different operating systems.
 */
public class Command {

  /**
   * Stores all equivalent commands with respect to support command lines.
   */
  private final HashMap<CommandLine, String> commands;

  /**
   * Abstraction storing equivalent commands for multiple command lines.
   */
  public Command() {
    commands = new HashMap<>();
  }

  /**
   * Set the command to execute for the specified type of command line.
   *
   * @param commandLine Type of command line.
   * @param command     Command to execute in the specified type of command line.
   */
  public void setCommand(CommandLine commandLine, String command) {
    commands.put(commandLine, command);
  }

  /**
   * Get the command to execute for the specified type of command line.
   *
   * @param commandLine                   Type of command line.
   * @return                              Command to execute in the specified command line type.
   * @throws CommandLineNotFoundException Thrown if no command is set for the specified command line
   *                                      type.
   */
  public String getCommand(CommandLine commandLine)
      throws CommandLineNotFoundException {
    if (commands.containsKey(commandLine)) {
      return commands.get(commandLine);
    } else {
      throw new CommandLineNotFoundException(commandLine);
    }
  }
}
