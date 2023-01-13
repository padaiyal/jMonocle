package org.padaiyal.utilities.commandline.abstractions;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

/**
 * Represents the response of an executed command.
 */
public class Response {

  /**
   * Stores the command outputs from different standard output streams.
   */
  private final HashMap<StdType, String> output;
  /**
   * Timestamp denoting the start of the command execution.
   */
  private final Instant executionStartTimestamp;
  /**
   * Timestamp denoting the end of the command execution.
   */
  private final Instant executionEndTimestamp;
  /**
   * Time taken to execute the command.
   */
  private final Duration executionDuration;
  /**
   * Return code for the executed command.
   */
  private final int returnCode;

  /**
   * Constructor used to initialize a Response object.
   *
   * @param returnCode              Return code of the executed command.
   * @param output                  Standard outputs of the executed command.
   * @param executionStartTimestamp Start timestamp of the executed command.
   * @param executionEndTimestamp   End timestamp of the executed command.
   */
  public Response(int returnCode, HashMap<StdType, String> output,
      Instant executionStartTimestamp, Instant executionEndTimestamp) {
    this.output = new HashMap<>(output);
    this.returnCode = returnCode;
    this.executionStartTimestamp = executionStartTimestamp;
    this.executionEndTimestamp = executionEndTimestamp;
    this.executionDuration = Duration.between(executionStartTimestamp, executionEndTimestamp);
  }

  /**
   * Retrieves the return code of the executed command.
   *
   * @return Return code of the executed command.
   */
  public int getReturnCode() {
    return returnCode;
  }

  /**
   * Retrieves the specified type of output (STDOUT or STDERR) for the executed command.
   *
   * @param typeOfOutput  Type of command output to retrieve. Can be STDERR or STDOUT.
   * @return              The desired output of the executed command.
   */
  public String getOutput(StdType typeOfOutput) {
    return output.get(typeOfOutput);
  }

  /**
   * Used to get the timestamp corresponding to when the command execution began.
   *
   * @return Timestamp denoting the beginning of the command execution.
   */
  public Instant getExecutionStartTimestamp() {
    return executionStartTimestamp;
  }

  /**
   * Used to get the timestamp corresponding to when the command execution ended.
   *
   * @return Timestamp denoting the end of the command execution.
   */
  public Instant getExecutionEndTimestamp() {
    return executionEndTimestamp;
  }

  /**
   * Used to get the duration taken for the command to execute.
   *
   * @return Duration taken for the command to execute.
   */
  public Duration getExecutionDuration() {
    return executionDuration;
  }

}
