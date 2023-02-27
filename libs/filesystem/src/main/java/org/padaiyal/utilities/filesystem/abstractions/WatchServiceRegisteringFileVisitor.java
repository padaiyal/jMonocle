package org.padaiyal.utilities.filesystem.abstractions;

import static org.padaiyal.utilities.filesystem.FileSystemUtility.registerWatchServiceForNotRegisteredDirectoryPathAfterFileVisitor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Class service for visiting registered files.
 */
public class WatchServiceRegisteringFileVisitor extends SimpleFileVisitor<Path> {

  /**
   * The path to monitor.
   */
  private final Path basePath;

  /**
   * The events to monitor.
   */
  private final WatchEvent.Kind<?>[] eventsToWatch;

  /**
   * The maximum depth to monitor.
   */
  private final int maxDepth;

  /**
   * The number of successful file visits.
   */
  private long successfulFileVisitsCount = 0;

  /**
   * The number of successful directory visits.
   */
  private long successfulDirectoryVisitsCount = 0;

  /**
   * The number of failed visits.
   */
  private long failedVisitsCount = 0;

  /**
   * The callback to invoke when a desired event is triggered.
   */
  private final BiConsumer<Path, WatchEvent<?>> consumer;

  /**
   * The WatchServiceRegisteringFileVisitor is a SimpleFileVisitor implementation that visits all
   * children files/folders of a specified path upto a specified max depth and registers them for a
   * WatchService to generate events for the specified watch event kinds.
   *
   * @param basePath      The path to walk.
   * @param maxDepth      The maximum depth to walk.
   * @param eventsToWatch Events to register with the watch service.
   * @param consumer      The callback to invoke when a desired event is triggered.
   */
  public WatchServiceRegisteringFileVisitor(
      Path basePath, 
      int maxDepth,
      WatchEvent.Kind<?>[] eventsToWatch,
      BiConsumer<Path, WatchEvent<?>> consumer
  ) {
    this.basePath = basePath;
    this.eventsToWatch = Arrays.copyOf(eventsToWatch, eventsToWatch.length);
    this.maxDepth = maxDepth;
    this.consumer = consumer;
  }

  /**
   * Called when a file is visited.
   *
   * @param childPath Path of the visited file.
   * @param attrs     Attributes of the visited file.
   * @return The result of the visit, indicates the next traversal action.
   * @throws IOException Thrown if walking through a path fails.
   */
  @Override
  public FileVisitResult visitFile(Path childPath, BasicFileAttributes attrs) throws IOException {
    if (Files.isDirectory(childPath)) {
      int newMaxDepth = getNewMaxDepth(childPath);
      registerWatchServiceForNotRegisteredDirectoryPathAfterFileVisitor(
          childPath, newMaxDepth, eventsToWatch, consumer);
      successfulDirectoryVisitsCount++;
    } else {
      successfulFileVisitsCount++;
    }
    return FileVisitResult.CONTINUE;
  }


  /**
   * Called when a file visit fails.
   *
   * @param childPath   Path of the file whose visit failed.
   * @param ioException Exception thrown during file visit failure.
   * @return The result of the visit, indicates the next traversal action.
   */
  @Override
  public FileVisitResult visitFileFailed(Path childPath, IOException ioException) {
    failedVisitsCount++;
    return FileVisitResult.SKIP_SUBTREE;
  }

  /**
   * Gets the max depth for the specified path relative to the original max depth of the base path.
   *
   * @param childPath Path for which the max depth needs to be computed.
   * @return New max depth of the input child path.
   */
  private int getNewMaxDepth(Path childPath) {
    Path relativizedChildPath = basePath.relativize(childPath);
    int newMaxDepth = maxDepth;
    if (!relativizedChildPath.toString().equals("")) {
      newMaxDepth -= relativizedChildPath.getNameCount();
    }
    return newMaxDepth;
  }

  /**
   * Called after a directory is visited.
   *
   * @param childPath   Path of the visited file.
   * @param ioException Exception thrown during directory visit failure.
   * @return The result of the visit, indicates the next traversal action.
   * @throws IOException Thrown if walking through a path fails.
   */
  @Override
  public FileVisitResult postVisitDirectory(Path childPath, IOException ioException)
      throws IOException {
    super.postVisitDirectory(childPath, ioException);

    int newMaxDepth = getNewMaxDepth(childPath);
    registerWatchServiceForNotRegisteredDirectoryPathAfterFileVisitor(
        childPath, newMaxDepth, eventsToWatch, consumer);
    return FileVisitResult.CONTINUE;
  }

  /**
   * Returns the number of successful file visits so far by this visitor.
   *
   * @return Number of successful file visits.
   */
  public long getSuccessfulFileVisitsCount() {
    return successfulFileVisitsCount;
  }

  /**
   * Returns the number of successful directory visits so far by this visitor.
   *
   * @return Number of successful directory visits.
   */
  public long getSuccessfulDirectoryVisitsCount() {
    return successfulDirectoryVisitsCount;
  }

  /**
   * Returns the number of failed file visits so far by this visitor.
   *
   * @return Number of failed file visits.
   */
  public long getFailedVisitsCount() {
    return failedVisitsCount;
  }

}

