package org.padaiyal.utilities.filesystem.abstractions;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Abstracts the object that stores information about the events to watch for a specified path.
 */
public class WatchServiceRegistrationInfo {

  /**
   * The path to monitor.
   */
  private final Path path;

  /**
   * The watchService associated to this path.
   */
  private final WatchService watchService;

  /**
   * The maximum depth to monitor.
   */
  private final int maxDepth;

  /**
   * The events to monitor.
   */
  private final WatchEvent.Kind<?>[] eventsToWatch;

  /**
   * The callback to invoke when the desired event is triggered.
   */
  private final BiConsumer<Path, WatchEvent<?>> consumer;


  /**
   * This class abstracts all the information needed to track watch service registrations.
   *
   * @param path          Registered path.
   * @param watchService  Watch service with which the path has been registered.
   * @param maxDepth      Max directory depth to monitor for events.
   * @param consumer      The callback to invoke when a desired event is triggered.
   * @param eventsToWatch Events registered with the watch service.
   */
  public WatchServiceRegistrationInfo(
      Path path, WatchService watchService,
      int maxDepth,
      BiConsumer<Path, WatchEvent<?>> consumer,
      WatchEvent.Kind<?>... eventsToWatch) {
    this.path = path;
    this.watchService = watchService;
    this.maxDepth = maxDepth;
    this.consumer = consumer;
    this.eventsToWatch = Arrays.copyOf(eventsToWatch, eventsToWatch.length);;

  }

  /**
   * Returns the registered path.
   *
   * @return Registered path.
   */
  @SuppressWarnings("unused")
  public Path getPath() {

    return path;
  }

  /**
   * Returns the watch service with which the path has been registered.
   *
   * @return Watch service with which the path has been registered.
   */
  public WatchService getWatchService() {

    return watchService;
  }

  /**
   * The max directory depth to monitor for events.
   *
   * @return Max directory depth to monitor for events.
   */
  public int getMaxDepth() {

    return maxDepth;
  }

  /**
   * Gets the BiConsumer associated to this path.
   *
   * @return The BiConsumer associated to this path.
   */
  public BiConsumer<Path, WatchEvent<?>> getConsumer() {
    return consumer;
  }

  /**
   * The events registered with the watch service.
   *
   * @return Events registered with the watch service.
   */
  @SuppressWarnings("unused")
  public WatchEvent.Kind<?>[] getEventsToWatch() {

    return eventsToWatch;
  }
}
