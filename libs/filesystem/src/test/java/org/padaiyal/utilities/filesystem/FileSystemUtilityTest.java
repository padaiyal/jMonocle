package org.padaiyal.utilities.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.padaiyal.utilities.I18nUtility;
import org.padaiyal.utilities.PropertyUtility;
import org.padaiyal.utilities.commandline.abstractions.OperatingSystem;
import org.padaiyal.utilities.unittestextras.parameterconverters.ArrayConverter;
import org.padaiyal.utilities.unittestextras.parameterconverters.ExceptionClassConverter;

/** Tests functionality of FileSystemUtility. */
@TestMethodOrder(OrderAnnotation.class)
class FileSystemUtilityTest {
  /** Logger object used to log info and errors. */
  private static final Logger logger = LogManager.getLogger(FileSystemUtilityTest.class);
  /** The time in milliseconds to wait for the background WatchService to log the file event. */
  private static int fileActionWaitTimeInMs;
  /** The path used to create the tests directories. */
  private static Path testBedPath;

  /** Private constructor. */
  private FileSystemUtilityTest() {}

  /**
   * Sets up the static variables necessary to run the tests in this class.
   *
   * @throws IOException When there is an issue adding the property file.
   */
  @BeforeAll
  public static void setUp() throws IOException {

    I18nUtility.addResourceBundle(
        FileSystemUtilityTest.class, FileSystemUtilityTest.class.getSimpleName(), Locale.US);
    PropertyUtility.addPropertyFile(
        FileSystemUtilityTest.class, "FileSystemUtilityTest.properties");
    fileActionWaitTimeInMs =
        Integer.parseInt(
            PropertyUtility.getProperty(
                "FileSystemUtilityTest.action.perform.waittime.milliseconds"));

    testBedPath =
        Files.createTempDirectory(
                PropertyUtility.getProperty("FileSystemUtilityTest.testDirectory"))
            .resolve("FUT")
            .toAbsolutePath();
  }

  /**
   * Get the expected logged events from the provided path, maxDepth and the types of events.
   *
   * @param path Registered path to provide the events for.
   * @param action The type of action that was performed.
   * @param maxDepth Maximum children depth from specified path to register and trigger events.
   * @return Hashmap of the paths with their respective expected logged events.
   */
  private static HashMap<String, Queue<Kind<?>>> getExpectedEvents(
      Path path, String action, int maxDepth) {

    HashMap<String, Queue<Kind<?>>> expectedEvents = new HashMap<>();
    Path absolutePath = path.toAbsolutePath();
    Path closestExistingParentPathForRegisteredPath =
        FileSystemUtility.getClosestExistingParent(absolutePath);

    Path relativePathToBeModified =
        closestExistingParentPathForRegisteredPath.relativize(absolutePath);

    int nameCount = relativePathToBeModified.getNameCount();
    int nameCountToUse = Math.min(nameCount, maxDepth + 1);

    IntStream.range(1, nameCountToUse + 1)
        .forEach(
            subPathEndIndex -> {
              Path relativePath = relativePathToBeModified.subpath(0, subPathEndIndex);
              Path subPath = closestExistingParentPathForRegisteredPath.resolve(relativePath);
              Queue<Kind<?>> subPathExpectedEvents = new LinkedList<>();
              expectedEvents.put(subPath.toAbsolutePath().toString(), subPathExpectedEvents);

              switch (action) {
                case "FILE_CREATE", "FOLDER_CREATE":
                  subPathExpectedEvents.add(StandardWatchEventKinds.ENTRY_CREATE);
                  break;

                case "FOLDER_MODIFY", "FILE_MODIFY":
                  subPathExpectedEvents.add(StandardWatchEventKinds.ENTRY_MODIFY);
                  break;

                case "FOLDER_DELETE", "FILE_DELETE":
                  subPathExpectedEvents.add(StandardWatchEventKinds.ENTRY_DELETE);
                  if (OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS
                      && subPathEndIndex < nameCountToUse) {
                    subPathExpectedEvents.add(StandardWatchEventKinds.ENTRY_MODIFY);
                  }
                  break;

                default:
                  // do nothing
              }
            });

    return expectedEvents;
  }

  /**
   * Converts a WatchEventKind from string to a WatchEvent.Kind object.
   *
   * @param watchEventToConvert The watchEvent to convert.
   * @return The converted WatchEvent.Kind
   */
  private static Kind<?> convertWatchEventKindFromString(String watchEventToConvert) {
    return switch (watchEventToConvert) {
      case "ENTRY_CREATE" -> StandardWatchEventKinds.ENTRY_CREATE;
      case "ENTRY_DELETE" -> StandardWatchEventKinds.ENTRY_DELETE;
      case "ENTRY_MODIFY" -> StandardWatchEventKinds.ENTRY_MODIFY;
      case "OVERFLOW" -> StandardWatchEventKinds.OVERFLOW;
      default -> throw new IllegalArgumentException(
          String.format(
              I18nUtility.getString("FileSystemUtilityTest.invalidArgumentMessage"),
              "watchEventToConvert",
              watchEventToConvert));
    };
  }

  /** Sets up testBedPath by creating the necessary files used by all tests. */
  @BeforeEach
  public void createTestBed() {
    try {
      Files.createDirectories(testBedPath);
    } catch (IOException e) {
      logger.error(e);
      throw new RuntimeException(e);
    }
    //noinspection StreamToLoop
    Stream.of("folder", "folders")
        .forEach(
            baseName ->
                IntStream.range(1, 3)
                    .forEach(
                        i -> {
                          try {
                            Files.createDirectory(testBedPath.resolve(baseName + i));
                            Files.createFile(
                                testBedPath.resolve(baseName + i).resolve(baseName + i + ".txt"));
                          } catch (IOException e) {
                            logger.error(e);
                            throw new RuntimeException(e);
                          }
                        }));

    /* Create 3 folders (fold3 to fold5) with a txt file in each with the same name of the
     *  folder and another child folder and grandchild folder.
     */
    IntStream.range(3, 6)
        .forEach(
            i -> {
              try {
                Path folderToCreate = testBedPath.resolve("fold" + i);
                Path grandChildFolderToCreate =
                    folderToCreate
                        .resolve("fold" + i + "_child")
                        .resolve("fold" + i + "_grandchild");
                Files.createDirectory(folderToCreate);
                Files.createFile(folderToCreate.resolve("fold" + i + ".txt"));
                Files.createDirectories(grandChildFolderToCreate);
              } catch (IOException e) {
                logger.error(e);
              }
            });

    // Create folderToDelete and folders2/fileToDelete.txt
    try {
      Files.createDirectory(testBedPath.resolve("folderToDelete"));
      Files.createFile(testBedPath.resolve("folders2").resolve("fileToDelete.txt"));
      Files.createFile(testBedPath.resolve("testBedFile.txt"));

    } catch (IOException e) {
      logger.error(e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Recursively deletes the contents in testBedPath and clears the pathToTriggeredEventMap.
   *
   * @throws IOException When there is an issue Unregistering testBedPath.
   */
  @AfterEach
  public void clearTest() throws IOException {
    FileSystemUtility.clearTriggeredEventsMap();
    FileSystemUtility.deRegisterWatchServiceForDirectory(testBedPath);
    Files.walk(testBedPath.toAbsolutePath())
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  /** Test creating batch folders. */
  @Test
  public void testCreateBatchFolders() {
    String baseName = "batchFolder";
    try {
      FileSystemUtility.createDirectories(testBedPath, baseName, false, 100);
      // Verify that all 100 folders have been created
      IntStream.range(1, 101)
          .forEach(
              i -> {
                String folderName = baseName.concat(Integer.toString(i));
                // Verify that each folder has been created
                Assertions.assertTrue(
                    Files.exists(testBedPath.resolve(folderName)));
                // Delete the folder
                try {
                  FileSystemUtility.deleteRecursively(testBedPath.resolve(folderName));
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
      // Generate a list of folder names starting with "folder"
      List<String> folderPaths =
          Files.list(testBedPath)
              .parallel()
              .filter(folderPath -> folderPath.getFileName().toString().startsWith(baseName))
              .map(folderPath -> folderPath.getFileName().toString())
              .collect(Collectors.toList());
      Assertions.assertEquals(0, folderPaths.size());
    } catch (IOException e) {
      logger.error(e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests creating directories with invalid inputs.
   *
   * @param pathToTest The path to test.
   * @param directoryName The name of the directory.
   * @param count The number of files to create.
   * @param expectedException The expected exception thrown by the method.
   */
  @ParameterizedTest
  @CsvSource({
      ",SomeName, 1, NullPointerException.class",
      "folder1/,,1, NullPointerException.class",
      "folder1/,SomeName, -1, IllegalArgumentException.class"
  })
  public void createDirectoriesWithInvalidInputs(
      String pathToTest,
      String directoryName,
      int count,
      @ConvertWith(ExceptionClassConverter.class) Class<? extends Exception> expectedException) {

    Path path = Objects.nonNull(pathToTest) ? testBedPath.resolve(pathToTest) : null;
    Assertions.assertThrows(
        expectedException,
        () -> FileSystemUtility.createDirectories(path, directoryName, true, count));
  }

  /**
   * Tests deleting files/directories recursively.
   *
   * @param directoryToDelete The directory to delete.
   */
  @ParameterizedTest
  @CsvSource({
      // One level path
      "folder1/child_folder",
      // Two level path
      "folder1/child_folder/grandchild_folder",
      // Two level path with files stored in it
      "folder1/child_folder/grandchild_folder/random.txt"
  })
  public void testDeleteRecursively(Path directoryToDelete) {
    Path path = testBedPath.resolve(directoryToDelete).toAbsolutePath();
    try {

      if (!Files.isDirectory(path)) {
        Path parentPath = path.getParent();
        Files.createDirectories(parentPath);
        Files.createFile(path);
      } else {
        Files.createDirectories(path);
      }
      FileSystemUtility.deleteRecursively(path);
    } catch (IOException e) {
      logger.error(e);
    }

    boolean folderDeleted = !Files.exists(path);
    Assertions.assertTrue(folderDeleted);
  }

  /**
   * Test deleting recursively with invalid inputs.
   *
   * @param invalidPath The invalid path to delete.
   * @param expectedExceptionClass The expected exception raised by deleteRecursively().
   */
  @ParameterizedTest
  @CsvSource({", NullPointerException.class", "randomness/, NoSuchFileException.class"})
  public void testDeleteRecursivelyWithInvalidInputs(
      Path invalidPath,
      @ConvertWith(ExceptionClassConverter.class)
      Class<? extends Exception> expectedExceptionClass) {
    Path path = !Objects.isNull(invalidPath) ? testBedPath.resolve(invalidPath) : null;
    Assertions.assertThrows(
        expectedExceptionClass, () -> FileSystemUtility.deleteRecursively(path));
  }

  /**
   * Tests getting immediate files whose name matches a specific regex pattern.
   *
   * @param patternToMatch The regex pattern to match.
   * @param expectedMatchingDirectories The expected matching Directories.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  @ParameterizedTest
  @CsvSource({
      "folder.*, 'folder1,folder2,folderToDelete,folders1,folders2'",
      "folder\\d+,'folder1,folder2'",
      "fold.*\\d+, 'folder1,folder2,folders1,folders2,fold3,fold4,fold5'",
      "fold\\d+, 'fold3,fold4,fold5'",
      "fold.*[3-4], 'fold3,fold4'",
      ".*old.*, 'folder1,folder2,folders1,folders2,fold3,fold4,fold5,folderToDelete'",
      "folder\\d+.*, 'folder1,folder2'",
      "abc.*, ''"
  })
  public void testListMatches(
      String patternToMatch,
      @ConvertWith(ArrayConverter.class) String[] expectedMatchingDirectories)
      throws IOException {

    List<Path> expectedMatchingDirectoriesList =
        Arrays.stream(expectedMatchingDirectories)
            .filter(expectedDirectory -> StringUtils.isNotBlank(expectedDirectory))
            .map(expectedDirectory -> testBedPath.resolve(expectedDirectory))
            .collect(Collectors.toList());

    List<Path> actualMatchingDirectories =
        FileSystemUtility.listMatches(testBedPath, patternToMatch);

    Collections.sort(expectedMatchingDirectoriesList);
    Collections.sort(actualMatchingDirectories);
    Assertions.assertEquals(expectedMatchingDirectoriesList, actualMatchingDirectories);
  }

  /**
   * Tests listMatches with invalid inputs.
   *
   * @param validPath Flag for providing a valid path. If true, the test uses testBedPath, else it
   *     uses null.
   * @param pattern The regex pattern to match.
   * @param expectedExceptionClass The expected exception that should be thrown.
   */
  @ParameterizedTest
  @CsvSource({
      // Null path
      "false, fold*, NullPointerException.class",
      // Null pattern
      "true,, NullPointerException.class",
      // Null pattern
      "false,,NullPointerException.class"
  })
  public void testListMatchesWithInvalidInputs(
      boolean validPath,
      String pattern,
      @ConvertWith(ExceptionClassConverter.class)
      Class<? extends Exception> expectedExceptionClass) {
    Path path = validPath ? testBedPath : null;

    Assertions.assertThrows(
        expectedExceptionClass, () -> FileSystemUtility.listMatches(path, pattern));
  }

  /**
   * Tests get Tree Matches with valid inputs.
   *
   * @param initialPath The initial path to use. If null then it uses testBedPath.
   * @param patternToMatch The patterns to match.
   * @param expectedMatchingDirectories The expected Matching directories.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  @ParameterizedTest
  @CsvSource({
      ",folder.*, 'folder1,folder1/folder1.txt,folders1,folders1/folders1.txt,"
          + "folder2,folder2/folder2.txt,folderToDelete,folders2,folders2/folders2.txt'",
      ",folder\\d+,'folder1,folder2'",
      ",fold.*\\d+, 'folder1,folder2,folders1,folders2,fold3,fold4,fold5'",
      ",fold\\d+, 'fold3,fold4,fold5'",
      ",fold.*[3-4].*, 'fold3,fold3/fold3.txt,fold3/fold3_child,"
          + "fold3/fold3_child/fold3_grandchild,fold4,fold4/fold4.txt,fold4/fold4_child,"
          + "fold4/fold4_child/fold4_grandchild'",
      ",folder\\d+.*, 'folder1,folder1/folder1.txt,folder2,folder2/folder2.txt'",
      ",abc.*, ''",
      "folder1/folder1.txt,folder\\d+.*,folder1/folder1.txt",
      "folder1/folder1.txt,something,''"
  })
  public void testTreeMatches(
      Path initialPath,
      String patternToMatch,
      @ConvertWith(ArrayConverter.class) String[] expectedMatchingDirectories)
      throws IOException {

    Path pathToUse = Objects.nonNull(initialPath) ? testBedPath.resolve(initialPath) : testBedPath;
    List<Path> expectedMatchingDirectoriesList =
        Arrays.stream(expectedMatchingDirectories)
            .filter(directory -> StringUtils.isNotBlank(directory))
            .map(testBedPath::resolve)
            .sorted()
            .collect(Collectors.toList());

    List<Path> actualMatchingDirectories = FileSystemUtility.treeMatches(pathToUse, patternToMatch);
    Collections.sort(actualMatchingDirectories);
    Assertions.assertEquals(expectedMatchingDirectoriesList, actualMatchingDirectories);
  }

  /**
   * Tests org.padaiyal.utilities.filesystem.FileSystemUtility#treeMatches(Path, String) with
   * invalid inputs.
   *
   * @param validPath Flag for providing a valid path. If true testBedPath is used, else null is
   *     used.
   * @param pattern The pattern to match.
   * @param expectedExceptionClass The expected exception to receive when inputting invalid inputs.
   */
  @ParameterizedTest
  @CsvSource({
      // Null path
      "false, fold*, NullPointerException.class",
      // Null pattern
      "true,, NullPointerException.class",
      // Null pattern
      "false,,NullPointerException.class"
  })
  public void testTreeMatchesWithInvalidInputs(
      boolean validPath,
      String pattern,
      @ConvertWith(ExceptionClassConverter.class)
      Class<? extends Exception> expectedExceptionClass) {
    Path path = validPath ? testBedPath.resolve("fold5/fold5.txt") : null;

    Assertions.assertThrows(
        expectedExceptionClass, () -> FileSystemUtility.treeMatches(path, pattern));
  }

  /**
   * Tests setting the permissions of a specific file.
   *
   * @param pathToTest The path of the file whose permission will be changed.
   * @param permissionToRemove The permission to remove.
   * @param recursively Flag for recursively changing the permissions of the contents inside the
   *     pathToTest.
   * @throws IOException When there is an error accessing a file or directory.
   */
  @ParameterizedTest
  @CsvSource({
      "folder1/, OWNER_WRITE, false",
      "folder1/, OWNER_READ, false",
      "folder1/folder1.txt, OWNER_WRITE, false",
      "folder1/folder1.txt, OWNER_READ, false",
      "folder1/, OWNER_WRITE, true",
      "folder1/, OWNER_READ, true"
  })
  public void testSetPermissions(
      Path pathToTest, PosixFilePermission permissionToRemove, boolean recursively)
      throws IOException {

    Assumptions.assumeFalse(
        OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS,
        I18nUtility.getString("FileSystemUtilityTest.skippingSetPosixPermissionsForNonPosixOS"));

    Path path = testBedPath.resolve(pathToTest);

    Set<PosixFilePermission> expectedPermissions = Files.getPosixFilePermissions(path);
    expectedPermissions.remove(permissionToRemove);

    Set<PosixFilePermission> actualPermissions = new HashSet<>();
    try {
      FileSystemUtility.setPermissions(path, expectedPermissions, recursively);
      Path pathToPerformAction = recursively ? path.resolve("folder1.txt") : path;
      boolean isActionPermitted;
      //noinspection EnhancedSwitchMigration
      switch (permissionToRemove) {
        case OWNER_WRITE: {
          isActionPermitted = pathToPerformAction.toFile().canWrite();
          break;
        }
        case OWNER_READ: {
          isActionPermitted = pathToPerformAction.toFile().canRead();
          break;
        }
        default: {
          throw new IllegalArgumentException(
              I18nUtility.getFormattedString(
                  "FileSystemUtilityTest.invalidArgumentMessage",
                  "permissionToRemove",
                  permissionToRemove));
        }
      }
      actualPermissions = Files.getPosixFilePermissions(path);

      Assertions.assertFalse(isActionPermitted);
      Assertions.assertEquals(
          expectedPermissions,
          actualPermissions,
          I18nUtility.getFormattedString(
              "FileSystemUtilityTest.failedToSetPermissionToCreatedFileErrorMessage",
              permissionToRemove.toString(),
              path.toAbsolutePath().toString()));
    } finally {
      actualPermissions.add(permissionToRemove);
      FileSystemUtility.setPermissions(path, actualPermissions, recursively);
    }
  }

  /**
   * Test setting path permissions with invalid input.
   *
   * @param pathToTest The path to test.
   * @param pathPermission The path permission to test.
   * @param expectedExceptionClass The expected exception raised.
   */
  @ParameterizedTest
  @CsvSource({
      // Null path
      ",OWNER_READ,NullPointerException.class",
      // Non-existing path
      "folder1/folder12.txt,OWNER_READ,IOException.class",
      // Invalid Path
      "folder1/folder1.txt/children,OWNER_READ,IOException.class",
  })
  public void testSetPermissionsWithInvalidInput(
      String pathToTest,
      PosixFilePermission pathPermission,
      @ConvertWith(ExceptionClassConverter.class)
      Class<? extends Exception> expectedExceptionClass) {
    Set<PosixFilePermission> filePermissionsToSet = new HashSet<>();
    filePermissionsToSet.add(pathPermission);

    Path filePath = Objects.nonNull(pathToTest) ? testBedPath.resolve(pathToTest) : null;

    Assertions.assertThrows(
        expectedExceptionClass,
        () -> FileSystemUtility.setPermissions(filePath, filePermissionsToSet, true));
  }

  /**
   * Tests setting the permissions of files that matches a specific regex pattern.
   *
   * @param pattern The regex pattern to match.
   * @param permissionToRemove The permission to remove.
   * @param expectedMatchingDirectories The expected directories that have changed permissions.
   * @throws IOException When there is an error accessing a file or a directory.
   */
  @ParameterizedTest
  @CsvSource({
      "folder\\d+, OWNER_WRITE, 'folder1,folder2'",
      "folder\\d+, OWNER_READ, 'folder1,folder2'",
      ".*File\\.txt, OWNER_WRITE, 'testBedFile.txt'",
      ".*File\\.txt, OWNER_READ, 'testBedFile.txt'",
      "folder\\d+\\.txt, OWNER_WRITE, 'folder1/folder1.txt,folder2/folder2.txt'",
      "folder\\d+\\.txt, OWNER_READ, 'folder1/folder1.txt,folder2/folder2.txt'"
  })
  public void testSetPermissionsIfMatches(
      String pattern,
      PosixFilePermission permissionToRemove,
      @ConvertWith(ArrayConverter.class) String[] expectedMatchingDirectories)
      throws IOException {

    Assumptions.assumeFalse(
        OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS,
        I18nUtility.getString("FileSystemUtilityTest.skippingSetPosixPermissionsForNonPosixOS"));

    Set<PosixFilePermission> filePermissionsToSet = new HashSet<>();
    Collections.addAll(
        filePermissionsToSet,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.GROUP_WRITE,
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_READ,
        PosixFilePermission.OTHERS_WRITE,
        PosixFilePermission.OWNER_EXECUTE);
    Set<PosixFilePermission> initialFilePermissions = new HashSet<>(filePermissionsToSet);
    filePermissionsToSet.remove(permissionToRemove);
    FileSystemUtility.setPermissions(testBedPath, initialFilePermissions, false);
    FileSystemUtility.setPermissionsIfMatches(testBedPath, filePermissionsToSet, pattern, false);

    Arrays.stream(expectedMatchingDirectories)
        .map(testBedPath::resolve)
        .forEach(
            resolvedPath -> {
              try {
                boolean isActionPermitted;
                //noinspection EnhancedSwitchMigration
                switch (permissionToRemove) {
                  case OWNER_WRITE: {
                    isActionPermitted = resolvedPath.toFile().canWrite();
                    break;
                  }
                  case OWNER_READ: {
                    isActionPermitted = resolvedPath.toFile().canRead();
                    break;
                  }
                  default: {
                    throw new IllegalArgumentException(
                        I18nUtility.getFormattedString(
                            "FileSystemUtilityTest.invalidArgumentMessage",
                            "permissionToRemove",
                            permissionToRemove));
                  }
                }
                Assertions.assertFalse(isActionPermitted);
                Assertions.assertEquals(
                    filePermissionsToSet,
                    Files.getPosixFilePermissions(resolvedPath),
                    I18nUtility.getFormattedString(
                        "FileSystemUtilityTest.failedToSetPermissionToCreatedFileErrorMessage",
                        permissionToRemove.toString(),
                        resolvedPath.toAbsolutePath().toString()));
                FileSystemUtility.setPermissions(resolvedPath, initialFilePermissions, false);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  /**
   * Tests the deleting files that matches a specific pattern.
   *
   * @param rootPath The path from where to start finding the files that matches the specific
   *     pattern.
   * @param pattern The pattern to match.
   * @param expectedDeletedPaths The expected matched paths.
   * @throws IOException When there is an error accessing a specific file or directory.
   */
  @ParameterizedTest
  @CsvSource({
      ",folder.*, 'folder1,folder1/folder1.txt,folders1,folders1/folders1.txt,"
          + "folder2,folder2/folder2.txt,folderToDelete,folders2,folders2/folders2.txt'",
      ",folder\\d+,'folder1,folder2'",
      ",fold.*\\d+, 'folder1,folder2,folders1,folders2,fold3,fold4,fold5'",
      ",fold\\d+, 'fold3,fold4,fold5'",
      ",fold.*[3-4].*, 'fold3,fold3/fold3.txt,fold3/fold3_child,"
          + "fold3/fold3_child/fold3_grandchild,fold4,fold4/fold4.txt,fold4/fold4_child,"
          + "fold4/fold4_child/fold4_grandchild'",
      ",folder\\d+.*, 'folder1,folder1/folder1.txt,folder2,folder2/folder2.txt'",
      ",folder\\d+.*,folder1/folder1.txt",
      "folder1,.*folder1,folder1/"
  })
  public void testDeleteIfMatchesPattern(
      Path rootPath,
      String pattern,
      @ConvertWith(ArrayConverter.class) String[] expectedDeletedPaths)
      throws IOException {
    Arrays.stream(expectedDeletedPaths)
        .map(testBedPath::resolve)
        .forEach(resolvedPath -> Assertions.assertTrue(resolvedPath.toFile().exists()));
    Path pathToUse = Objects.nonNull(rootPath) ? testBedPath.resolve(rootPath) : testBedPath;
    FileSystemUtility.deleteIfMatches(pathToUse, pattern);
    Arrays.stream(expectedDeletedPaths)
        .map(testBedPath::resolve)
        .forEach(resolvedPath -> Assertions.assertFalse(resolvedPath.toFile().exists()));
  }

  /**
   * Tests setting file attributes for files with matching names.
   *
   * @param pattern The pattern the file needs to match.
   * @param expectedMatchedPaths The expected matched paths.
   * @param attributeName The name of the attribute to change.
   * @param attributeValue The new value of the attribute.
   * @param isRecursive Flag for performing recursive file name matching.
   * @throws ParseException When the attributeValue cannot be changed.
   * @throws IOException When there is an issue accessing a file or directory.
   */
  @ParameterizedTest
  @CsvSource({
      ".*folder\\d+.*, 'folder1,folder1/folder1.txt,folder2,folder2/folder2.txt'"
          + ",lastModifiedTime,01.01.2013,true",
      ".*folder\\d+.*, 'folder1,folder2',lastModifiedTime,01.01.2013, false"
  })
  public void testSetAttributesIfMatches(
      String pattern,
      @ConvertWith(ArrayConverter.class) String[] expectedMatchedPaths,
      String attributeName,
      String attributeValue,
      boolean isRecursive)
      throws ParseException, IOException {

    String timeFormat = PropertyUtility.getProperty("FileSystemUtilityTest.timeFormat");
    Object parsedValue = attributeValue;
    if (attributeName.equals("lastModifiedTime") || attributeName.equals("lastAccessTime")) {
      long milliseconds = new SimpleDateFormat(timeFormat).parse(attributeValue).getTime();
      parsedValue = FileTime.fromMillis(milliseconds);
    } else if (attributeName.equals("size")) {
      parsedValue = Integer.parseInt(attributeValue);
    }

    FileSystemUtility.setAttributeIfMatches(
        testBedPath, attributeName, parsedValue, pattern, isRecursive);
    Object finalParsedValue = parsedValue;
    Arrays.stream(expectedMatchedPaths)
        .map(testBedPath::resolve)
        .forEach(
            resolvedPath -> {
              try {
                Object actualValue = Files.getAttribute(resolvedPath, attributeName);
                Assertions.assertEquals(finalParsedValue, actualValue);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  /**
   * Test if isSubPath checks that a path is a sub path of another.
   *
   * @param parentPathString Path to check for sub paths.
   * @param pathToCheckString Path to check it is sub path of parentPathsString.
   * @param expectedResult The expected result.
   */
  @ParameterizedTest
  @CsvSource({
      // Two subdirectories of /tmp/FUT
      "fold3, fold4, false",
      // Subdirectory of fold3
      "fold3, fold3/fold3_child, true",
      // Same directory
      "fold4/fold4_child, fold4/fold4_child, true",
      // Subdirectory of fold5/fold5_child
      "fold5/fold5_child, fold5/fold5_child/fold5_grandchild, true",
      // Different subdirectory of fold4/fold4_child
      "fold5/fold5_child, fold4/fold4_child/fold4_grandchild, false",
      // Comparing file in main directory
      "fold5/fold5_child, fold5/fold5.txt, false"
  })
  public void testIsSubPath(
      String parentPathString, String pathToCheckString, boolean expectedResult) {
    Path parentPath = testBedPath.resolve(parentPathString);
    Path pathToCheck = testBedPath.resolve(pathToCheckString);
    boolean actualResult = FileSystemUtility.isSubPath(parentPath, pathToCheck);
    Assertions.assertEquals(expectedResult, actualResult);
  }

  /** Test isSubPath method with invalid inputs. */
  @ParameterizedTest
  @CsvSource({
      // Null parent path
      ",folder1/, NullPointerException.class",
      // Null potential sub path
      "folder1/,,NullPointerException.class",
      // Null parent path and potential sub path
      ",,NullPointerException.class",
      // File as parent path
      "folder1/folder1.txt, folder1/, IllegalArgumentException.class"
  })
  public void testIsSubPathWithInvalidInputs(
      Path parentPath,
      Path potentialSubPath,
      @ConvertWith(ExceptionClassConverter.class)
      Class<? extends Exception> expectedExceptionClass) {

    Path resolvedParentPath = Objects.nonNull(parentPath) ? testBedPath.resolve(parentPath) : null;

    Path resolvedPotentialSubPath =
        Objects.nonNull(potentialSubPath) ? testBedPath.resolve(potentialSubPath) : null;

    Assertions.assertThrows(
        expectedExceptionClass,
        () -> FileSystemUtility.isSubPath(resolvedParentPath, resolvedPotentialSubPath));
  }

  /** Test getClosestExistingParent with invalid inputs. */
  @Test
  public void testGetClosestExistingParentWithInvalidInputs() {
    Assertions.assertThrows(
        NullPointerException.class, () -> FileSystemUtility.getClosestExistingParent(null));
  }

  /**
   * Test get closest existing parent from a specific directory or file path.
   *
   * @param path Path to get the closest existing parent from.
   * @param expectedClosestExistingParentPath Expected parent path of the provided path.
   */
  @ParameterizedTest
  @CsvSource({
      // Existing directory
      "fold4, fold4",
      // Non-existing directory
      "fold4/sdsds/, fold4/",
      // Current directory
      "fold1, .",
      // Parent directory
      "../lol, .."
  })
  public void testGetClosestExistingParent(String path, String expectedClosestExistingParentPath) {
    Path pathToFindClosestExistingParent = testBedPath.resolve(path);
    Path expectedClosestExistingParent =
        testBedPath.resolve(expectedClosestExistingParentPath).toAbsolutePath().normalize();
    Path actualClosestExistingParent =
        FileSystemUtility.getClosestExistingParent(pathToFindClosestExistingParent)
            .toAbsolutePath()
            .normalize();

    Assertions.assertEquals(
        expectedClosestExistingParent.toString(), actualClosestExistingParent.toString());
  }

  /** Test get closest existing parent when there is no parent. */
  @Test
  public void testGetClosestExistingParentWhenNoParent() {
    try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
      mockedFiles.when(() -> Files.exists(Mockito.any())).thenReturn(false);

      Assertions.assertNull(FileSystemUtility.getClosestExistingParent(testBedPath));
    }
  }

  /**
   * Test registeringPath with invalid input.
   *
   * @param pathToRegister The path to register.
   * @param maxDepth The maximum depth of the path look for events.
   * @param validConsumer A flag for providing a valid Consumer. If false, it uses null, else it
   *     uses a Consumer that does nothing.
   * @param validWatchEvents A flag for providing valid WatchEvents. If false, it uses null, else it
   *     uses the values provided by eventsToRegisterString.
   * @param eventsToRegisterString The events to register in string format.
   * @param expectedException The expected exception raised by the method tested.
   */
  @ParameterizedTest
  @CsvSource({
      // Null path
      ",1,true, true, 'ENTRY_CREATE', NullPointerException.class",
      // Negative max depth
      "folder1/, -1, true, true, 'ENTRY_CREATE', IllegalArgumentException.class",
      // Null BiConsumer
      "folder1/,1,false, true, 'ENTRY_CREATE', NullPointerException.class",
      // Null WatchEvents
      "folder1/,1,true, false, 'ENTRY_CREATE', NullPointerException.class",
      // All invalid inputs
      ",-1, false,false, '', NullPointerException.class",
      // Empty WatchEvents
      "folder1/,1,true, true, '', IllegalArgumentException.class",
      // Non existing registration path
      "folder1/wasntme/,1,true, true, 'ENTRY_CREATE', IllegalArgumentException.class",
      // File path
      "folder1/folder1.txt,1,true, true, 'ENTRY_CREATE', IllegalArgumentException.class",
      // File path
      "folder1/,1,true, true, 'ENTRY_CREATE,ENTRY_CREATE,ENTRY_CREATE,ENTRY_CREATE,"
          + "ENTRY_CREATE', IllegalArgumentException.class",
  })
  public void testRegisteringPathWithInputValidation(
      Path pathToRegister,
      int maxDepth,
      boolean validConsumer,
      Boolean validWatchEvents,
      @ConvertWith(ArrayConverter.class) String[] eventsToRegisterString,
      @ConvertWith(ExceptionClassConverter.class) Class<? extends Exception> expectedException) {
    Path resolvedPathToRegister =
        Objects.nonNull(pathToRegister) ? testBedPath.resolve(pathToRegister) : null;
    BiConsumer<Path, WatchEvent<?>> biConsumerToUse =
        validConsumer ? (path, watchEvent) -> {} : null;
    int eventsToRegisterSize =
        (eventsToRegisterString.length == 1 && eventsToRegisterString[0].equals(""))
            ? 0
            : eventsToRegisterString.length;

    Kind<?>[] eventsToRegister;
    if (validWatchEvents) {
      eventsToRegister = new Kind<?>[eventsToRegisterSize];
      IntStream.range(0, eventsToRegisterSize)
          .forEach(
              index ->
                  eventsToRegister[index] =
                      convertWatchEventKindFromString(eventsToRegisterString[index]));
    } else {
      eventsToRegister = null;
    }

    Assertions.assertThrows(
        expectedException,
        () ->
            FileSystemUtility.registerWatchServiceForNotRegisteredDirectoryPath(
                resolvedPathToRegister, maxDepth, biConsumerToUse, eventsToRegister));
  }

  /**
   * Test deRegisteringPaths with invalid inputs.
   *
   * @param pathToTest The invalid path to test.
   * @param expectedExceptionClass The expected exception raised by the method tested.
   */
  @ParameterizedTest
  @CsvSource({
      // Null path
      ", NullPointerException.class",
      // Existing file path
      "testBedFile.txt, IllegalArgumentException.class"
  })
  public void testDeregisterWithInputValidation(
      String pathToTest,
      @ConvertWith(ExceptionClassConverter.class)
      Class<? extends Exception> expectedExceptionClass) {

    Path inputPath = Objects.nonNull(pathToTest) ? testBedPath.resolve(pathToTest) : null;
    // Null path
    Assertions.assertThrows(
        expectedExceptionClass,
        () -> FileSystemUtility.deRegisterWatchServiceForDirectory(inputPath));
  }

  /**
   * Test registering paths with service events.
   *
   * @param relativePath Relative path of the file or directory to modify.
   * @param action Action to perform on the relative path provided.
   * @param maxDepth Maximum children depth from specified path to register and trigger events.
   * @throws IOException Thrown if registering a WatchService or walking through a path or modifying
   *     it fails.
   * @throws InterruptedException Thrown if the current is interrupted.
   */
  @ParameterizedTest
  @CsvSource({
      // Modify subdirectory of /fold4 at second level
      "fold4/fold4_child/fold4_grandchild, FOLDER_MODIFY, 2",
      // Modify existing subdirectory of /tmp/FUT
      "fold4, FOLDER_MODIFY, 2",
      // Create subdirectories of /tmp/FUT with a depth value equal to the number of directories
      // created
      "createdFolder/1/2/3/4/5, FOLDER_CREATE, 6",
      // Create subdirectories of /tmp/FUT with a depth value less than the number of
      // directories created
      "createdFolder/1/2/3/4/5, FOLDER_CREATE, 3",
      // Create file in a subdirectory of /tmp/FUT that doesn't exist and low max depth
      "createdFolder/1/2.txt, FILE_CREATE, 1",
      // Create file in a subdirectory of /tmp/FUT that doesn't exist and low max depth
      "fold3/fold3.txt, FILE_MODIFY, 1",
      // Delete existing subdirectory of /tmp/FUT
      "folderToDelete, FOLDER_DELETE, 2",
      // Delete subdirectory of a specific subdirectory of /tmp/FUT with a depth level to catch
      // event
      "fold4/fold4_child/fold4_grandchild, FOLDER_DELETE, 3",
      // Modify file in subdirectory of /tmp/FUT
      "fold4/fold4.txt, FILE_MODIFY, 1",
      // Delete existing file in subdirectory of /tmp/FUT
      "folders2/fileToDelete.txt, FILE_DELETE, 1"
  })
  public void testFolderWatchServiceRegistering(String relativePath, String action, int maxDepth)
      throws IOException, InterruptedException {
    Assumptions.assumeFalse(
        OperatingSystem.getOperatingSystem() == OperatingSystem.MAC_OS_X,
        I18nUtility.getString(
            "FileSystemUtilityTest.skippingWatchServiceTestingForMACOSDueToTimeout"));

    Assumptions.assumeFalse(
        OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS,
        I18nUtility.getString("FileSystemUtilityTest.skippingSetPosixPermissionsForNonPosixOS"));

    final HashMap<String, Queue<Kind<?>>> expectedPathToEventKindMap;

    Path pathToRegister = testBedPath.resolve(relativePath);
    FileSystemUtility.registerWatchServiceForNotRegisteredDirectoryPath(
        testBedPath,
        maxDepth,
        (path, watchEvent) -> {},
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.OVERFLOW);

    expectedPathToEventKindMap = getExpectedEvents(pathToRegister, action, maxDepth);
    switch (action) {
      case "FOLDER_CREATE":
        createDirectoriesRecursively(
            pathToRegister,
            Integer.parseInt(
                PropertyUtility.getProperty(
                    "FileSystemUtilityTest.create.directories.recursively.waittime.milliseconds")));
        break;
      case "FILE_CREATE":
        createDirectoriesRecursively(
            pathToRegister.getParent(),
            Integer.parseInt(
                PropertyUtility.getProperty(
                    "FileSystemUtilityTest.create.directories.recursively.waittime.milliseconds")));
        Files.createFile(pathToRegister);
        break;

      case "FOLDER_MODIFY", "FILE_MODIFY":
        Files.setLastModifiedTime(pathToRegister, FileTime.from(Instant.now()));
        break;

      case "FOLDER_DELETE", "FILE_DELETE":
        FileSystemUtility.deleteRecursively(pathToRegister);
        break;

      default:
        throw new IllegalArgumentException(
            String.format(
                I18nUtility.getString("FileSystemUtilityTest.invalidArgumentMessage"),
                "action",
                action));
    }

    logger.info(() ->
        String.format(
            I18nUtility.getString("FileSystemUtilityTest.waitingForFileChangesMessage"),
            fileActionWaitTimeInMs / 1000));
    Thread.sleep(fileActionWaitTimeInMs);

    Map<String, Queue<Kind<?>>> actualEvents = FileSystemUtility.getPathsToTriggeredEventMap();
    Map<String, Queue<Kind<?>>> actualEventsFound =
        actualEvents.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, entry -> new LinkedList<>(entry.getValue())));

    FileSystemUtility.deRegisterWatchServiceForDirectory(testBedPath);
    FileSystemUtility.clearTriggeredEventsMap();

    Assertions.assertEquals(expectedPathToEventKindMap, actualEventsFound);
  }

  /**
   * Test unregistering a directory.
   *
   * @param pathToRegisterString Path to register.
   * @param pathToDeRegisterString Sub path of the pathToRegisterString.
   * @param maxDepth Maximum children depth from specified path to register and trigger events.
   * @throws IOException Thrown if registering a WatchService or walking through a path or modifying
   *     it fails.
   * @throws InterruptedException Thrown if the current thread is interrupted.
   */
  @ParameterizedTest
  @CsvSource({
      "fold4, fold4/fold4_child, 3", // existing directory to register and path to deregister
      ", fold3, 3" // null with existing directory
  })
  public void testFolderWatchServiceDeRegistering(
      String pathToRegisterString, String pathToDeRegisterString, int maxDepth)
      throws IOException, InterruptedException {
    Assumptions.assumeFalse(
        OperatingSystem.getOperatingSystem() == OperatingSystem.MAC_OS_X,
        I18nUtility.getString(
            "FileSystemUtilityTest.skippingWatchServiceTestingForMACOSDueToTimeout"));

    if (pathToRegisterString != null) {
      Path pathToRegister = testBedPath.resolve(pathToRegisterString);

      FileSystemUtility.registerWatchServiceForNotRegisteredDirectoryPath(
          pathToRegister,
          maxDepth,
          (path, watchEvent) -> {},
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_DELETE,
          StandardWatchEventKinds.ENTRY_MODIFY,
          StandardWatchEventKinds.OVERFLOW);

      // Perform actions to trigger the events
      Path createdFilePath = pathToRegister.resolve("parent_random.txt");
      Files.createFile(createdFilePath);
      Thread.sleep(fileActionWaitTimeInMs);

      Map<String, Queue<Kind<?>>> pathsToTriggeredEventMap =
          FileSystemUtility.getPathsToTriggeredEventMap();

      Queue<Kind<?>> eventsForRegisteredPath =
          pathsToTriggeredEventMap.get(createdFilePath.toAbsolutePath().toString());
      // Assert actions have been logged
      Assertions.assertNotNull(eventsForRegisteredPath);
      Assertions.assertTrue(eventsForRegisteredPath.contains(StandardWatchEventKinds.ENTRY_CREATE));
    }

    // Deregister sub path
    Path pathToDeRegister = testBedPath.resolve(pathToDeRegisterString);
    FileSystemUtility.deRegisterWatchServiceForDirectory(pathToDeRegister);
    // Perform actions in sub path
    Files.createFile(pathToDeRegister.resolve("random.txt"));
    Thread.sleep(fileActionWaitTimeInMs);
    Map<String, Queue<Kind<?>>> pathsToTriggeredEventMap =
        FileSystemUtility.getPathsToTriggeredEventMap();
    Queue<Kind<?>> eventsForDeRegisteredPath =
        pathsToTriggeredEventMap.get(pathToDeRegister.toAbsolutePath().toString());
    // Assert actions have not been logged
    Assertions.assertNull(eventsForDeRegisteredPath);
  }

  /**
   * Test register events with only one type of event.
   *
   * @param eventToRegisterString Event to register.
   * @throws IOException Thrown if registering a WatchService or walking through a path or modifying
   *     it fails.
   * @throws InterruptedException Thrown if the current thread is interrupted.
   */
  @ParameterizedTest
  @CsvSource({
      "ENTRY_CREATE", // Create event
      "ENTRY_MODIFY", // Modify event
      "ENTRY_DELETE", // Delete event
  })
  public void testSelectiveRegisteringWatchService(String eventToRegisterString)
      throws IOException, InterruptedException {
    Assumptions.assumeFalse(
        OperatingSystem.getOperatingSystem() == OperatingSystem.MAC_OS_X,
        I18nUtility.getString(
            "FileSystemUtilityTest.skippingWatchServiceTestingForMACOSDueToTimeout"));

    Kind<?> eventToRegister = convertWatchEventKindFromString(eventToRegisterString);

    FileSystemUtility.registerWatchServiceForNotRegisteredDirectoryPath(
        testBedPath, 3, (path, watchEvent) -> {}, eventToRegister);
    // Perform the actions to trigger the events
    Thread.sleep(fileActionWaitTimeInMs);
    Path fileToCreatePath = testBedPath.resolve("selectiveRegistering.txt");
    Files.createFile(fileToCreatePath);
    Files.setLastModifiedTime(fileToCreatePath, FileTime.from(Instant.now()));
    Files.delete(fileToCreatePath);
    Thread.sleep(fileActionWaitTimeInMs);

    String fileToCreatePathString = fileToCreatePath.toAbsolutePath().toString();
    Map<String, Queue<Kind<?>>> pathsToTriggeredEventMap =
        FileSystemUtility.getPathsToTriggeredEventMap();
    Queue<Kind<?>> generatedEventsQueue = pathsToTriggeredEventMap.get(fileToCreatePathString);
    // Assert that only the provided event has been logged
    Assertions.assertNotNull(
        generatedEventsQueue,
        String.format(
            I18nUtility.getString("FileSystemUtilityTest.noEventsGeneratedForActionMessage"),
            fileToCreatePathString));
    Assertions.assertEquals(generatedEventsQueue.size(), 1);
    Assertions.assertEquals(eventToRegister, generatedEventsQueue.remove());

    FileSystemUtility.deRegisterWatchServiceForDirectory(testBedPath);
  }

  /**
   * Creates a directory by creating all nonexistent parent directories first.
   *
   * @param path Path of the directory to create
   * @param delayInMsBetweenDirectoryCreation Time to delay between each directory creation in
   *     milliseconds
   */
  private void createDirectoriesRecursively(Path path, long delayInMsBetweenDirectoryCreation) {
    // Input validation
    Objects.requireNonNull(path);
    if (Files.exists(path) && !Files.isDirectory(path)) {
      throw new IllegalArgumentException(
          String.format(
              I18nUtility.getString(
                  "org.padaiyal.utilities.file.FileUtility.invalidDirPathMessage"),
              path));
    }

    Path absolutePath = testBedPath.toAbsolutePath().relativize(path.toAbsolutePath());
    int nameCount = absolutePath.getNameCount();
    IntStream.range(1, nameCount + 1)
        .mapToObj(index -> absolutePath.subpath(0, index))
        .forEach(
            relativePathToCreate -> {
              try {
                Path pathToCreate = testBedPath.resolve(relativePathToCreate);
                if (!Files.exists(pathToCreate)) {
                  Files.createDirectory(pathToCreate);
                  Thread.sleep(delayInMsBetweenDirectoryCreation);
                }
              } catch (IOException | InterruptedException e) {
                logger.error(e);
                throw new RuntimeException(e);
              }
            });
  }

  /** Tests creating directories throws an Unchecked IOException using Mockito. */
  @Test
  public void testCreateDirectoriesWithIoException() {
    String name = "folder12";
    boolean isPrefix = false;
    int count = 5;
    try (MockedStatic<Files> mockedFileUtility = Mockito.mockStatic(Files.class)) {
      mockedFileUtility
          .when(() -> Files.createDirectories(ArgumentMatchers.any()))
          .thenThrow(IOException.class);
      Assertions.assertThrows(
          UncheckedIOException.class,
          () -> FileSystemUtility.createDirectories(testBedPath, name, isPrefix, count));
    }
  }

  /**
   * Tests setting permissions when there is an IOException.
   *
   * @throws IOException When there is an issue accessing a file or directory.
   */
  @Test
  public void testSetPermissionsWithIoException() throws IOException {

    Assumptions.assumeFalse(
        OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS,
        I18nUtility.getString("FileSystemUtilityTest.skippingSetPosixPermissionsForNonPosixOS"));

    boolean recursively = true;
    Path path = testBedPath.resolve("folder1/");
    Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(path);

    // Using result here, because calling callRealMethod throws a casting error.
    List<Path> paths = Files.walk(path).collect(Collectors.toList());
    try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
      mockedFiles
          .when(() -> Files.setPosixFilePermissions(Mockito.any(Path.class), Mockito.anySet()))
          .thenThrow(IOException.class);
      mockedFiles.when(() -> Files.walk(Mockito.any(Path.class))).thenReturn(paths.stream());

      Assertions.assertThrows(
          UncheckedIOException.class,
          () -> FileSystemUtility.setPermissions(path, filePermissions, recursively));
    }
  }

  /**
   * Tests setting permissions (with a certain pattern) when there is an IOException.
   *
   * @throws IOException When there is an issue accessing a file or directory.
   */
  @Test
  public void testSetPermissionsIfMatchesWithIoException() throws IOException {

    Assumptions.assumeFalse(
        OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS,
        I18nUtility.getString("FileSystemUtilityTest.skippingSetPosixPermissionsForNonPosixOS"));

    Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(testBedPath);
    boolean recursively = false;
    String pattern = ".*fold.*";

    // Using result here, because calling callRealMethod throws a casting error.
    List<Path> paths = Files.walk(testBedPath).collect(Collectors.toList());
    try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
      mockedFiles
          .when(() -> Files.setPosixFilePermissions(Mockito.any(Path.class), Mockito.anySet()))
          .thenThrow(IOException.class);
      mockedFiles.when(() -> Files.walk(Mockito.any(Path.class))).thenReturn(paths.stream());
      mockedFiles.when(() -> Files.isDirectory(Mockito.any(Path.class))).thenReturn(true);
      Assertions.assertThrows(
          UncheckedIOException.class,
          () ->
              FileSystemUtility.setPermissionsIfMatches(
                  testBedPath, filePermissions, pattern, recursively));
    }
  }

  /** Test initializeDependantValues. */
  @Test
  public void testInitializeDependantValuesWithIoException() {
    FileSystemUtility.initializeDependantValues();
    try (MockedStatic<PropertyUtility> mockedProperty = Mockito.mockStatic(PropertyUtility.class)) {
      mockedProperty
          .when(() -> PropertyUtility.addPropertyFile(Mockito.any(), Mockito.anyString()))
          .thenThrow(IOException.class);

      Assertions.assertDoesNotThrow(FileSystemUtility::initializeDependantValues);
    }
  }

  /**
   * Test if background watch service thread stops when threadSwitch is set to false.
   *
   * @throws IOException Thrown if registering a WatchService or walking through a path or modifying
   *     it fails.
   * @throws InterruptedException Thrown if the current thread is interrupted.
   */
  @Test
  @Order(1)
  public void testBackgroundWatchServiceWhenThreadSwitchIsDisabled()
      throws IOException, InterruptedException {
    FileSystemUtility.initializeDependantValues();
    try (MockedStatic<PropertyUtility> mockedProperty = Mockito.mockStatic(PropertyUtility.class)) {
      mockedProperty
          .when(
              () ->
                  PropertyUtility.getProperty(
                      "FileSystemUtility.backgroundThread.watchService.switch"))
          .thenReturn("false");

      FileSystemUtility.registerWatchServiceForNotRegisteredDirectoryPathAfterFileVisitor(
          testBedPath,
          1,
          new Kind[] {StandardWatchEventKinds.ENTRY_CREATE},
          (path, watchEvent) -> {});

      FileSystemUtility.backgroundWatchServiceRegisteringThreadRunnable.run();
      Path pathToCreate = testBedPath.resolve(Paths.get("test.txt"));
      Files.createFile(pathToCreate);

      Thread.sleep(fileActionWaitTimeInMs);
      Map<String, Queue<Kind<?>>> triggeredEvents = FileSystemUtility.getPathsToTriggeredEventMap();
      Assertions.assertEquals(0, triggeredEvents.size());
    }
  }

  /**
   * Test registering a WatchService for a file path.
   *
   * @throws IOException When there is an issue accessing a file or directory.
   */
  @Test
  @Order(2)
  public void registerWatchServiceForNotRegisteredFilePathAfterFileVisitor() throws IOException {
    FileSystemUtility.registerWatchServiceForNotRegisteredDirectoryPathAfterFileVisitor(
        testBedPath.resolve("fold5/fold5.txt"),
        1,
        new Kind[] {StandardWatchEventKinds.ENTRY_CREATE},
        (path, watchEvent) -> {});
    Assertions.assertEquals(0, FileSystemUtility.getRegisteredPaths().size());
  }
}
