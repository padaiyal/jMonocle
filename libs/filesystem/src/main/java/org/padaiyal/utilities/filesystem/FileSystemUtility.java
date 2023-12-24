package org.padaiyal.utilities.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.padaiyal.utilities.I18nUtility;
import org.padaiyal.utilities.PropertyUtility;
import org.padaiyal.utilities.filesystem.abstractions.WatchServiceRegisteringFileVisitor;
import org.padaiyal.utilities.filesystem.abstractions.WatchServiceRegistrationInfo;

/**
 * File utility library for creating, modifying, deleting and monitoring files and directories.
 */
public class FileSystemUtility {

  /**
   * Logger object to log information and errors.
   */
  private static final Logger logger = LogManager.getLogger(FileSystemUtility.class);

  /**
   * Map of paths to the WatchRegistrationInfo, used to store events to track for each path.
   */
  private static final ConcurrentHashMap<String, WatchServiceRegistrationInfo>
      pathToWatchServiceRegistrationInfoMap = new ConcurrentHashMap<>();

  /**
   * Map that stores the path that triggered an event and the event type they generated.
   */
  private static final ConcurrentHashMap<String, Queue<Kind<?>>> pathToTriggeredEventMap =
      new ConcurrentHashMap<>();

  /**
   * Thread object that visits all files to look for events.
   */
  private static Thread backgroundWatchServiceRegisteringThread;

  /**
   * Runnable used by the backgroundWatchServiceRegisteringThread.
   */
  public static final Runnable backgroundWatchServiceRegisteringThreadRunnable = () -> {

    while (Boolean.parseBoolean(
            PropertyUtility.getProperty(
                "FileSystemUtility.backgroundThread.watchService.switch"))) {
      HashMap<String, WatchServiceRegistrationInfo>
          pathToWatchServiceRegistrationInfoMapCopy =
          new HashMap<>(pathToWatchServiceRegistrationInfoMap);

      // Iterate through all registered watch services and check if any events are
      // generated
      pathToWatchServiceRegistrationInfoMapCopy.keySet()
          .stream()
          .sorted()
          .forEach(
              (subPath) -> {

                WatchServiceRegistrationInfo subPathWatchServiceInfo =
                        pathToWatchServiceRegistrationInfoMapCopy
                                .get(subPath);

                if (subPathWatchServiceInfo == null) {
                  logger.warn("WatchServiceRegistrationInfo is null, subPath = ({}), "
                                  + "pathToWatchServiceRegistrationInfoMapCopy = ({})", subPath,
                          pathToWatchServiceRegistrationInfoMapCopy);
                } else {
                  WatchService subPathWatchService = subPathWatchServiceInfo
                          .getWatchService();
                  try {
                    WatchKey watchKey = subPathWatchService.poll();

                    if (watchKey != null) {
                      List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                      /* Iterate through all watch events and add another watch service
                      for folders created within the maxDepth level. */
                      for (WatchEvent<?> event : watchEvents) {

                        Path changePath =
                                Paths.get(subPath).resolve((Path) event.context());

                        String changePathString = changePath.toAbsolutePath().toString();

                        if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {

                          if (Files.isDirectory(changePath.toAbsolutePath())) {

                            Path basePath = changePath.toAbsolutePath();
                            while (!pathToWatchServiceRegistrationInfoMapCopy.containsKey(
                                    basePath.toString())) {
                              basePath = basePath.getParent();
                            }

                            int maxDepthForBasePath =
                                    pathToWatchServiceRegistrationInfoMapCopy
                                            .get(basePath.toString())
                                            .getMaxDepth();

                            if (basePath.relativize(changePath).getNameCount()
                                    <= maxDepthForBasePath) {
                              registerWatchServiceForNotRegisteredDirectoryPath(
                                      changePath,
                                      maxDepthForBasePath - 1,
                                      subPathWatchServiceInfo.getConsumer(),
                                      subPathWatchServiceInfo.getEventsToWatch());

                            } else {
                              String messageKey =
                                      "FileSystemUtility.skipRegistrationPathPastMaxDepth";
                              logger.debug(() ->
                                      I18nUtility.getFormattedString(
                                              messageKey,
                                              changePath,
                                              subPathWatchServiceInfo.getMaxDepth()));
                            }
                          } else {
                            String messageKey =
                                    "FileSystemUtility.skippingRegistrationPathIsADirectory";
                            logger.debug(() ->
                                    I18nUtility.getFormattedString(
                                            messageKey,
                                            changePath));
                          }
                        } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
                          /* If a directory is deleted, deregister it if it has previously
                          been registered to a watch service */
                          if (pathToWatchServiceRegistrationInfoMap.containsKey(
                                  changePathString)) {
                            deRegisterWatchServiceForDirectory(changePath);
                          }
                        }

                        pathToTriggeredEventMap.putIfAbsent(
                                changePathString,
                                new LinkedBlockingQueue<>()
                        );
                        Queue<Kind<?>> watchServiceEventsQueue =
                                pathToTriggeredEventMap.get(changePathString);
                        watchServiceEventsQueue.add(event.kind());

                        subPathWatchServiceInfo.getConsumer().accept(changePath, event);
                      }
                      watchKey.reset();
                    }
                  } catch (ClosedWatchServiceException | IOException e) {
                    logger.warn(e);
                  }
                }
              });
    }
  };

  static {
    initializeDependantValues();
  }

  /**
   * Private constructor.
   */
  private FileSystemUtility() {
  }

  /**
   * Initialize static values.
   */
  public static void initializeDependantValues() {
    I18nUtility.addResourceBundle(FileSystemUtility.class,
        FileSystemUtility.class.getSimpleName(),
        Locale.US
    );
    try {
      PropertyUtility.addPropertyFile(
          FileSystemUtility.class,
          "FileSystemUtility.properties"
      );
    } catch (IOException e) {
      logger.warn(e);
    }
  }

  /**
   * Creates a specific number of folders with the specified name and prefix/suffix.
   *
   * @param path     The path where the folders have to be created.
   * @param name     The base name of the folders to be created.
   * @param isPrefix If true append the number to the prefix of the folder name, else append it as
   *                 the suffix.
   * @param count    The number of folders to be created.
   */
  public static void createDirectories(Path path, String name, boolean isPrefix, int count) {
    if (count < 0) {
      throw new IllegalArgumentException(
          I18nUtility.getFormattedString("FileSystemUtility.negativeFileCount", count)
      );
    }
    IntStream.range(1, count + 1)
        .parallel()
        .forEach(
            x -> {
              String folderName =
                  isPrefix ? Integer.toString(x).concat(name) : name.concat(Integer.toString(x));
              Path folderPath = path.resolve(folderName);
              try {
                Files.createDirectories(folderPath);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  /**
   * Sets permissions to a specified file/folder and all its contents if needed.
   * It supports only POSIX OS (Linux, MAC OS).
   *
   * @param path           The path of the folder/file whose permission needs to be changed.
   * @param permissions    The new permission to set to the folder/file specified.
   * @param setRecursively If true and a folder is specified, it sets the specified permissions to
   *                       all the contents in it, recursively. If false and a folder is specified,
   *                       It only sets the specified permission to the folder specified and not any
   *                       of its contents. If a file is specified, irrespective, this parameter
   *                       doesn't have any effect on it.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  public static void setPermissions(
      Path path, Set<PosixFilePermission> permissions, boolean setRecursively) throws IOException {

    if (setRecursively) {
      try {
        setPosixPermissions(path, permissions);
      } catch (AccessDeniedException e) { // Exception is thrown if the user doesn't have read
        // permission.
        Files.setPosixFilePermissions(path, permissions);
        setPosixPermissions(path, permissions);
      }
    } else {
      Files.setPosixFilePermissions(path, permissions);
    }
  }

  /**
   * Helper method for setPermissions which walks the given path & sets the Posix
   * file permissions for all the contents of path.
   *
   * @param path           The path of the folder/file whose permission needs to be changed.
   * @param permissions    The new permission to set to the folder/file specified.
   **/
  private static void setPosixPermissions(Path path, Set<PosixFilePermission> permissions)
          throws IOException {
    try (Stream<Path> paths = Files.walk(path)) {
      paths.parallel()
              .forEach(node -> {
                try {
                  Files.setPosixFilePermissions(node, permissions);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    }
  }

  /**
   * Lists the immediate folder contents/file that match the specified pattern.
   *
   * @param path          The path to find file name matches in.
   * @param patternString The pattern to match the file name with.
   * @return List of Path objects of the files/folders whose name matches the specified pattern.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  public static List<Path> listMatches(Path path, String patternString) throws IOException {
    return getContentMatches(path, patternString, false);
  }

  /**
   * Lists the recursive folder contents/file that match the specified pattern.
   *
   * @param path          The path to recursively find file name matches in.
   * @param patternString The pattern to match the file name with.
   * @return List of Path objects of the files/folder contents whose name matches the specified
   *     pattern.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  public static List<Path> treeMatches(Path path, String patternString) throws IOException {
    return getContentMatches(path, patternString, true);
  }

  /**
   * Lists the recursive/immediate folder contents/file that match the specified pattern.
   *
   * @param path          The path to recursively find file name matches in.
   * @param patternString The pattern to match the file name with.
   * @param searchTree    If a directory is specified and this parameter is true, it searches
   *                      recursively, else it only searches the immediate contents of the folder.
   *                      This parameter is not applicable if a file path is specified.
   * @return List of Path objects of the files/folder contents whose name matches the specified
   *     pattern.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  private static List<Path> getContentMatches(Path path, String patternString, boolean searchTree)
          throws IOException {
    List<Path> matchList = new ArrayList<>();
    List<Path> contents = getFiles(path, searchTree);

    if (Files.isDirectory(path)) {
      matchList.addAll(
          contents
              .parallelStream()
              .filter(
                  content ->
                      matches(patternString, 0, content.getFileName().toString()))
              .collect(Collectors.toList()));
    } else if (matches(patternString, 0, path.getFileName().toString())) {
      matchList.add(path);
    }
    return matchList;
  }

  /**
   * Returns the stream of the paths based on the given searchTree value and path.
   *
   * @param path          The path to recursively find file name matches in.
   * @param searchTree    If a directory is specified and this parameter is true, it searches
   *                      recursively, else it only searches the immediate contents of the folder.
   *                      This parameter is not applicable if a file path is specified.
   * @return              Stream of the paths based on the given searchTree value and path.
   * @throws IOException  When there is an issue accessing a file or directory.
   */
  private static List<Path> getFiles(Path path, boolean searchTree) throws IOException {
    if (searchTree) {
      try (Stream<Path> stream = Files.walk(path)) {
        return stream.collect(Collectors.toList());
      }
    } else {
      try (Stream<Path> stream = Files.list(path)) {
        return stream.collect(Collectors.toList());
      }
    }
  }

  /**
   * Deletes the specified folder/file.
   *
   * @param path The folder/file to delete.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  public static void deleteRecursively(Path path) throws IOException {
    Objects.requireNonNull(path);
    if (Files.isDirectory(path)) {
      List<Path> dirTreeList;
      try (Stream<Path> stream = Files.list(path)) {
        dirTreeList = stream.collect(Collectors.toList());
      }
      for (Path walkPath : dirTreeList) {
        if (Files.isDirectory(walkPath)) {
          FileSystemUtility.deleteRecursively(walkPath);
        } else {
          Files.delete(walkPath);
        }
      }
    }
    Files.delete(path);
  }

  /**
   * Deletes contents specified by the path input if the name matches the provided pattern.
   *
   * @param path    The path to perform the matched deletion in.
   * @param pattern The pattern to match the file/folder name with.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  public static void deleteIfMatches(Path path, String pattern)
      throws IOException {
    if (matches(pattern, 0, path.getFileName().toString())) {
      FileSystemUtility.deleteRecursively(path);
    } else {
      List<Path> matchedPaths = FileSystemUtility.treeMatches(path, pattern);
      for (Path matchedPath : matchedPaths) {
        try {
          FileSystemUtility.deleteRecursively(matchedPath);
        } catch (NoSuchFileException e) {
          logger.warn(e);
        }
      }
    }
  }

  /**
   * Changes the permission of all the contents in the path if the name matches the provided
   * pattern. It supports only POSIX OS (Linux, MAC OS).
   *
   * @param path           The path to perform the matched permission change in.
   * @param permissions    The permissions to set the matched folders/files with.
   * @param pattern        The pattern to match the file/folder name with.
   * @param setRecursively If a directory is specified and this parameter is true, it sets the
   *                       specified permissions recursively, else it checks the specified
   *                       folder/file name and sets the permissions if it matches the specified
   *                       pattern. This parameter is not applicable if a file path is specified.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  public static void setPermissionsIfMatches(
      Path path, Set<PosixFilePermission> permissions, String pattern, boolean setRecursively)
      throws IOException {
    List<Path> paths = FileSystemUtility.treeMatches(path, pattern);
    paths.parallelStream()
        .forEach(content -> {
          try {
            FileSystemUtility.setPermissions(content, permissions, setRecursively);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }

  /**
   * Sets the attribute of the contents specified by the path input if the name matches the provided
   * pattern.
   *
   * @param path           The path to set the attribute in.
   * @param attributeName  The name of the attribute to set.
   * @param attributeValue The new value of the specified attribute.
   * @param pattern        The pattern to match the file/folder name with.
   * @param setRecursively If a directory is specified and this parameter is true, it matches
   *                       recursively and sets the specified attribute, else it checks the
   *                       specified folder/file name and sets the attribute if it matches the
   *                       specified pattern. This parameter is not applicable if a file path is
   *                       specified.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  public static void setAttributeIfMatches(
      Path path, String attributeName, Object attributeValue, String pattern,
      boolean setRecursively) throws IOException {
    List<Path> contents = setRecursively
        ?
        FileSystemUtility.treeMatches(path, pattern) : FileSystemUtility.listMatches(path, pattern);
    for (Path content : contents) {
      Files.setAttribute(content, attributeName, attributeValue);
    }
  }

  /**
   * Checks if one path is a sub path of the other.
   *
   * @param parentPath       Path to check for sub paths.
   * @param potentialSubPath Path to check if it's a sub path of the parentPath path.
   * @return true if potentialSubPath is a sub path of parentPath.
   */
  public static boolean isSubPath(Path parentPath, Path potentialSubPath) {
    // Input validation
    Objects.requireNonNull(parentPath);
    Objects.requireNonNull(potentialSubPath);

    if (Files.exists(parentPath) && !Files.isDirectory(parentPath)) {
      throw new IllegalArgumentException(
          I18nUtility.getFormattedString("FileSystemUtility.pathNotADirectory", parentPath));
    }
    Path parentPathAbsolute = parentPath.toAbsolutePath();
    Path potentialSubPathAbsolute = potentialSubPath.toAbsolutePath();

    if (parentPathAbsolute.equals(potentialSubPathAbsolute)) {
      return true;
    }

    String parentPathString = parentPathAbsolute + File.separator;
    String potentialSubPathString = potentialSubPathAbsolute.toString();

    return potentialSubPathString.startsWith(parentPathString);
  }

  /**
   * Get the closest existing parent folder for the input path.
   *
   * @param path Input path.
   * @return The closest existing parent folder for the input path.
   */
  public static Path getClosestExistingParent(Path path) {
    // Input validation
    Objects.requireNonNull(path);

    Path tempPath = path.toAbsolutePath();
    while (tempPath != null && !Files.exists(tempPath)) {
      tempPath = tempPath.getParent();
    }
    return tempPath;
  }

  /**
   * Registers a specified currentPath for a specific set of events with WatchService after File
   * Visitor.
   *
   * @param currentPath       Path to register and generate events.
   * @param maxDepth          Maximum depth to register WatchService.
   * @param eventTypesToWatch Type of events for which events have to be generated.
   * @param consumer          The callback to invoke when a desired event is triggered.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  public static void registerWatchServiceForNotRegisteredDirectoryPathAfterFileVisitor(
      Path currentPath, int maxDepth, Kind<?>[] eventTypesToWatch,
      BiConsumer<Path, WatchEvent<?>> consumer) throws IOException {
    Path tempPath = currentPath.toAbsolutePath();
    if (!pathToWatchServiceRegistrationInfoMap.containsKey(tempPath.toString())
        && Files.isDirectory(tempPath)) {
      WatchService subPathWatchService = FileSystems.getDefault().newWatchService();
      WatchServiceRegistrationInfo watchServiceRegistrationInfo =
          new WatchServiceRegistrationInfo(
              tempPath, subPathWatchService, maxDepth, consumer, eventTypesToWatch);
      pathToWatchServiceRegistrationInfoMap.put(
          watchServiceRegistrationInfo.getPath().toString(), watchServiceRegistrationInfo);

      currentPath.register(
          watchServiceRegistrationInfo.getWatchService(),
          watchServiceRegistrationInfo.getEventsToWatch()
      );

      logger.debug(() ->
          I18nUtility.getFormattedString(
              "FileSystemUtility.registeredWatchServiceMessage", tempPath.toString()));

    } else {
      logger.debug(() ->
          I18nUtility.getFormattedString(
              "FileSystemUtility.skippedWatchServiceRegistrationMessage",
              tempPath,
              pathToWatchServiceRegistrationInfoMap.containsKey(tempPath.toString()),
              Files.isDirectory(tempPath)));
    }
  }

  /**
   * Registers a specified directory path to a WatchService to keep track of specific type of events
   * within that path.
   *
   * <p>Limitations: - Actual event timestamp is not available - Large maxDepth can cause OOM and
   * other performance issues.
   *
   * @param path          Directory path to register with the WatchService.
   * @param maxDepth      Maximum children depth from specified path to register and trigger
   *                      events.
   * @param consumer      The callback to invoke when a desired event is triggered.
   * @param eventsToWatch Type of events to register for trigger.
   * @throws IOException Thrown if registering a WatchService or walking through a path or modifying
   *                     it fails.
   */
  public static void registerWatchServiceForNotRegisteredDirectoryPath(
      Path path,
      int maxDepth,
      BiConsumer<Path, WatchEvent<?>> consumer,
      Kind<?>... eventsToWatch)
      throws IOException {
    // Input validation
    Objects.requireNonNull(path);
    Objects.requireNonNull(consumer);
    Objects.requireNonNull(eventsToWatch);
    if (!Files.exists(path)) {
      throw new IllegalArgumentException(
          I18nUtility.getFormattedString("FileSystemUtility.pathDoesNotExist", path));
    }
    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException(
          I18nUtility.getFormattedString("FileSystemUtility.pathNotADirectory", path));
    }
    if (maxDepth < 0) {
      throw new IllegalArgumentException(
          I18nUtility.getFormattedString("FileSystemUtility.negativeMaxDepth", maxDepth));
    }
    if (eventsToWatch.length == 0) {
      throw new IllegalArgumentException(
          I18nUtility.getFormattedString("FileSystemUtility.noEventsToWatch"));
    }
    if (eventsToWatch.length > 4) {
      throw new IllegalArgumentException(
          I18nUtility.getFormattedString(
              "FileSystemUtility.moreThanMaxEventsToWatch", Arrays.toString(eventsToWatch)));
    }

    Path tempPath = path.toAbsolutePath();
    logger.info(() ->
        I18nUtility.getFormattedString(
            "FileSystemUtility.registeredWatchServiceMessage", tempPath));

    // Register all children recursively
    WatchServiceRegisteringFileVisitor watchServiceRegisteringFileVisitor =
        new WatchServiceRegisteringFileVisitor(tempPath, maxDepth, eventsToWatch, consumer);

    Files.walkFileTree(
        tempPath,
        new HashSet<>(Collections.singletonList(FileVisitOption.FOLLOW_LINKS)),
        maxDepth,
        watchServiceRegisteringFileVisitor);

    logger.info(() ->
        I18nUtility.getFormattedString(
            "FileSystemUtility.successfulFolderVisits",
            watchServiceRegisteringFileVisitor.getSuccessfulDirectoryVisitsCount()));
    logger.info(() ->
        I18nUtility.getFormattedString(
            "FileSystemUtility.successfulFileVisits",
            watchServiceRegisteringFileVisitor.getSuccessfulFileVisitsCount()));
    logger.info(() ->
        I18nUtility.getFormattedString(
            "FileSystemUtility.failedVisits",
            watchServiceRegisteringFileVisitor.getFailedVisitsCount()));

    // Invoke bg thread only once, not repeatedly when the function is called multiple times
    if (backgroundWatchServiceRegisteringThread == null) {
      backgroundWatchServiceRegisteringThread =
          new Thread(backgroundWatchServiceRegisteringThreadRunnable);
      backgroundWatchServiceRegisteringThread.start();
    }
  }

  /**
   * Remove the WatchService registered for the specified path.
   *
   * @param path Path whose WatchService registration needs to be removed.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  public static void deRegisterWatchServiceForDirectory(Path path) throws IOException {
    // Input validation
    Objects.requireNonNull(path);

    if (Files.exists(path) && !Files.isDirectory(path)) {
      throw new IllegalArgumentException(
          I18nUtility.getFormattedString("FileSystemUtility.pathNotADirectory", path));
    }

    // Warning if trying to unregister path that was never registered
    if (!pathToWatchServiceRegistrationInfoMap.containsKey(path.toString())) {
      logger.warn(() -> I18nUtility.getFormattedString(
          "FileSystemUtility.deRegisteringNotRegisteredPath",
          path
      ));
      return;
    }

    Set<String> registeredPaths = new HashSet<>(pathToWatchServiceRegistrationInfoMap.keySet());

    for (String registeredPath : registeredPaths) {
      Path registeredPathAbsolute = Paths.get(registeredPath).toAbsolutePath();
      if (isSubPath(path, registeredPathAbsolute)) {
        logger.debug(() ->
            I18nUtility.getFormattedString(
                "FileSystemUtility.deRegisteringPathMessage", registeredPath));
        // Remove watch service
        WatchService watchServiceToRemove =
            pathToWatchServiceRegistrationInfoMap.remove(registeredPath).getWatchService();

        // Close watch service
        watchServiceToRemove.close();
      }
    }
  }

  /**
   * Clears the generated triggered events map.
   */
  public static void clearTriggeredEventsMap() {

    pathToTriggeredEventMap.clear();
  }

  /**
   * Gets the trigger path to event types map.
   *
   * @return The trigger path to the event types map.
   */
  public static Map<String, Queue<Kind<?>>> getPathsToTriggeredEventMap() {
    return new HashMap<>(pathToTriggeredEventMap);
  }

  /**
   * Gets the list of monitored paths.
   *
   * @return The list of monitored paths.
   */
  public static List<String> getRegisteredPaths() {
    return new ArrayList<>(pathToWatchServiceRegistrationInfoMap.keySet());
  }

  /**
   * Checks if the specified pattern matches the given input.
   *
   * @param patternString  Pattern to match.
   * @param patternOptions Pattern match flags.
   * @param input          Input string.
   * @return Returns true if the pattern matches the input, else false.
   */
  private static boolean matches(String patternString, int patternOptions, String input) {
    return Pattern.compile(patternString, patternOptions)
        .matcher(input)
        .matches();
  }
}
