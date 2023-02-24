package org.padaiyal.utilities.vaidhiyar;

import com.google.gson.JsonArray;
import com.sun.management.OperatingSystemMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import javax.management.MBeanServerConnection;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.padaiyal.utilities.I18nUtility;
import org.padaiyal.utilities.PropertyUtility;
import org.padaiyal.utilities.unittestextras.parameterconverters.ExceptionClassConverter;
import org.padaiyal.utilities.vaidhiyar.abstractions.CpuLoadGenerator;
import org.padaiyal.utilities.vaidhiyar.abstractions.ExtendedMemoryUsage;
import org.padaiyal.utilities.vaidhiyar.abstractions.ExtendedThreadInfo;
import org.padaiyal.utilities.vaidhiyar.abstractions.GarbageCollectionInfo;

/**
 * Tests JvmUtility.
 */
class JvmUtilityTest {

  /**
   * Logger object used to log information and errors.
   */
  private static final Logger logger = LogManager.getLogger(JvmUtilityTest.class);
  /**
   * Duration to wait for the CPU load generator to start generating the load.
   */
  private static long durationToWaitForCpuLoadGeneratorInMilliSeconds;
  /**
   * OperatingSystemMxBean object used to get the number of CPU cores in this system.
   */
  private static OperatingSystemMXBean operatingSystemMxBean;
  /**
   * Duration over which the CPU thread times have been sampled. (Test value)
   */
  private static Long samplingDurationInMilliSeconds;
  /**
   * Initial thread CPU times. (Test values)
   */
  private static Map<Long, Long> initialThreadCpuTimesInNanoSeconds;
  /**
   * Current thread CPU times. (Test values)
   */
  private static Map<Long, Long> currentThreadCpuTimesInNanoSeconds;
  /**
   * Stores the computed thread CPU usages.
   */
  private static Map<Long, Double> threadCpuUsages;
  /**
   * The logging level to set for the code to test.
   */
  protected static Level loggingLevel = Level.OFF;

  /**
   * Sets up dependant values needed for the test.
   *
   * @throws IOException When there is an issue adding the property file.
   */
  @BeforeAll
  public static void setUp() throws IOException {
    Configurator.setAllLevels("", loggingLevel);
    PropertyUtility.addPropertyFile(
        JvmUtilityTest.class,
        "JvmUtilityTest.properties"
    );
    durationToWaitForCpuLoadGeneratorInMilliSeconds = PropertyUtility.getTypedProperty(
        Long.class,
        "JvmUtilityTest.durationToWaitForCpuLoadGenerator.milliseconds"
    );

    I18nUtility.addResourceBundle(
        JvmUtilityTest.class,
        JvmUtilityTest.class.getSimpleName(),
        Locale.US
    );

    operatingSystemMxBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    samplingDurationInMilliSeconds = 1L;
    initialThreadCpuTimesInNanoSeconds = new HashMap<>();
    initialThreadCpuTimesInNanoSeconds.put(996L, 112_000L);
    initialThreadCpuTimesInNanoSeconds.put(997L, 34_000L);
    initialThreadCpuTimesInNanoSeconds.put(998L, 232_000L);

    currentThreadCpuTimesInNanoSeconds = new HashMap<>();
    currentThreadCpuTimesInNanoSeconds.put(997L, 112_000L);
    currentThreadCpuTimesInNanoSeconds.put(998L, 580_000L);
    currentThreadCpuTimesInNanoSeconds.put(999L, 930_000L);

    // Valid values
    threadCpuUsages = JvmUtility.getThreadCpuUsages(
        initialThreadCpuTimesInNanoSeconds,
        currentThreadCpuTimesInNanoSeconds,
        samplingDurationInMilliSeconds
    );
  }

  /**
   * Tests JvmUtility::getAllExtendedThreadInfo().
   */
  @Test
  public void testExtendedThreadInfo() {
    Arrays.stream(JvmUtility.getAllExtendedThreadInfo(0))
        .forEach(extendedThreadInfo -> {
          Assertions.assertTrue(extendedThreadInfo.getCpuUsage() >= -1);
          Assertions.assertTrue(extendedThreadInfo.getCpuUsage() <= 100);
          Assertions.assertTrue(extendedThreadInfo.getMemoryAllocatedInBytes() >= 0);
          Assertions.assertNotNull(extendedThreadInfo.getThreadInfo());
        });
  }

  /**
   * Tests JvmUtility::getAllExtendedThreadInfo() for the CPU usage aspect.
   *
   * @param testDurationInMilliSeconds    Duration for which the CPU load has to be generated.
   * @param expectedCpuLoad               CPU load to be generated by the CPU load generator.
   * @param actualCpuLoadAllowedTolerance Allowed % tolerance in actual load thread CPU usage value.
   * @throws InterruptedException         When the thread sleep is interrupted.
   * @throws IOException                  When there is an issue instantiating the CPULoadGenerator
   *                                      object.
   */
  @ParameterizedTest
  @CsvSource({
      "60000, 0.6, 0.5"
  })
  public void testThreadCpuUsage(
      long testDurationInMilliSeconds,
      double expectedCpuLoad,
      double actualCpuLoadAllowedTolerance
  ) throws InterruptedException, IOException {
    final CpuLoadGenerator cpuLoadGenerator = new CpuLoadGenerator();
    String expectedCpuLoadThreadName = PropertyUtility.getProperty("CpuLoadGenerator.thread.name");

    ExtendedThreadInfo[] preExistingCpuLoadThreadInfos = JvmUtility.getAllExtendedThreadInfo(0);
    final long preExistingCpuLoadThreadsCount = Arrays.stream(preExistingCpuLoadThreadInfos)
        .filter(extendedThreadInfo -> extendedThreadInfo.getThreadInfo().getThreadName()
              .equals(expectedCpuLoadThreadName))
        .count();

    cpuLoadGenerator.start(expectedCpuLoad, testDurationInMilliSeconds);
    // Wait for the CPU load generator to start.
    Thread.sleep(durationToWaitForCpuLoadGeneratorInMilliSeconds);
    Arrays.stream(JvmUtility.getAllExtendedThreadInfo(0))
            .filter(extendedThreadInfo -> extendedThreadInfo.getThreadInfo()
                    .getThreadName()
                    .equals(expectedCpuLoadThreadName)
            )
            .map(extendedThreadInfo -> extendedThreadInfo.getCpuUsage() >= 0)
            .forEach(Assertions::assertTrue);
    ExtendedThreadInfo[] extendedThreadInfos = JvmUtility.getAllExtendedThreadInfo(0);

    Assertions.assertNotNull(extendedThreadInfos);

    Assertions.assertEquals(
        operatingSystemMxBean.getAvailableProcessors(),
        Arrays.stream(extendedThreadInfos)
            .filter(extendedThreadInfo -> extendedThreadInfo.getThreadInfo().getThreadName()
                .equals(expectedCpuLoadThreadName))
            .count() - preExistingCpuLoadThreadsCount
    );

    // Test if required load is generated within an expected amount of time.
    Assertions.assertTimeoutPreemptively(
        Duration.ofMillis(testDurationInMilliSeconds),
        () -> {
          boolean requiredLoadGenerated = false;
          while (!requiredLoadGenerated) {
            requiredLoadGenerated = Arrays.stream(extendedThreadInfos)
                .filter(extendedThreadInfo -> extendedThreadInfo.getThreadInfo()
                    .getThreadName()
                    .equals(expectedCpuLoadThreadName)
                )
                .peek(
                    extendedThreadInfo -> logger.info(
                        I18nUtility.getFormattedString(
                          "JvmUtilityTest.printThreadCpuUsage",
                          extendedThreadInfo.getThreadInfo().getThreadName(),
                          extendedThreadInfo.getThreadInfo().getThreadId(),
                          extendedThreadInfo.getCpuUsage(),
                          expectedCpuLoad * (1.0 - actualCpuLoadAllowedTolerance)
                        )
                    )
                )
                .map(extendedThreadInfo -> (
                        extendedThreadInfo.getCpuUsage() / 100.0
                            >= 0
                    )
                )
                .reduce(Boolean::logicalAnd)
                .orElse(false);
          }
        }
    );

  }

  /**
   * Tests starting/terminating the thread CPU usage collector.
   *
   * @throws InterruptedException If the thread sleep is interrupted.
   */
  @Test
  public void testCpuUsageCollector() throws InterruptedException {
    JvmUtility.setRunThreadCpuUsageCollectorSwitch(true);
    Assertions.assertTrue(JvmUtility.getRunThreadCpuUsageCollectorSwitch());
    Assertions.assertTrue(JvmUtility.isThreadCpuUsageCollectorRunning());

    JvmUtility.setRunThreadCpuUsageCollectorSwitch(false);
    Assertions.assertFalse(JvmUtility.getRunThreadCpuUsageCollectorSwitch());

    // Wait for CPU usage collector to terminate.
    Thread.sleep(durationToWaitForCpuLoadGeneratorInMilliSeconds);
    Assertions.assertFalse(JvmUtility.isThreadCpuUsageCollectorRunning());

    JvmUtility.setRunThreadCpuUsageCollectorSwitch(false);
    Assertions.assertFalse(JvmUtility.getRunThreadCpuUsageCollectorSwitch());

    JvmUtility.setRunThreadCpuUsageCollectorSwitch(true);

    Assertions.assertTrue(JvmUtility.getRunThreadCpuUsageCollectorSwitch());

    // Wait for CPU usage collector to start.
    Thread.sleep(durationToWaitForCpuLoadGeneratorInMilliSeconds);
    Assertions.assertTrue(JvmUtility.isThreadCpuUsageCollectorRunning());

    // Ensure that a new thread is not spawned when the runThreadCpuUsageCollector is set to true
    // again.
    Future<Void> initialFutureObject = JvmUtility.getCpuUsageCollectorFuture();
    JvmUtility.setRunThreadCpuUsageCollectorSwitch(true);
    Assertions.assertEquals(
        initialFutureObject,
        JvmUtility.getCpuUsageCollectorFuture()
    );
  }

  /**
   * Tests JvmUtility.cpuUsageCollectorThread when JvmUtility::runCpuUsageCollector() throws an
   * InterruptedException.
   */
  @Test
  void testRunCpuCollectorInterruptedException() {
    try (
        MockedStatic<JvmUtility> threadMock = Mockito.mockStatic(JvmUtility.class)
    ) {
      threadMock.when(
          JvmUtility::runCpuUsageCollector
      ).thenThrow(InterruptedException.class);

      Assertions.assertThrows(
          RuntimeException.class,
          JvmUtility.cpuUsageCollectorThread::call
      );
    }
  }

  /**
   * Tests thread CPU usage computation given invalid inputs - initial thread CPU times,
   * current thread CPU times and the sampling duration.
   */
  @Test
  void testGetThreadCpuUsages() {

    Map<Long, Long> initialThreadCpuTimesInNanoSecondsWithNullValues = new HashMap<>();
    initialThreadCpuTimesInNanoSecondsWithNullValues.put(998L, null);

    Map<Long, Long> currentThreadCpuTimesInNanoSecondsWithNullValues = new HashMap<>();
    currentThreadCpuTimesInNanoSecondsWithNullValues.put(998L, null);

    Map<Long, Long> initialThreadCpuTimesInNanoSecondsWithInitialTimeGreaterThanCurrentTime
        = new HashMap<>();
    initialThreadCpuTimesInNanoSecondsWithInitialTimeGreaterThanCurrentTime.put(998L, 800L);

    Map<Long, Long> currentThreadCpuTimesInNanoSecondsWithCurrentTimeLesserThanInitialTime
        = new HashMap<>();
    currentThreadCpuTimesInNanoSecondsWithCurrentTimeLesserThanInitialTime.put(998L, 600L);

    Map<Long, Long> initialThreadCpuTimesInNanoSecondsWithNegativeValues = new HashMap<>();
    initialThreadCpuTimesInNanoSecondsWithNegativeValues.put(998L, -100L);

    Map<Long, Long> currentThreadCpuTimesInNanoSecondsWithNegativeValues = new HashMap<>();
    currentThreadCpuTimesInNanoSecondsWithNegativeValues.put(998L, -800L);


    // Null inputs
    Assertions.assertThrows(
        NullPointerException.class,
        () -> JvmUtility.getThreadCpuUsages(
            null,
            currentThreadCpuTimesInNanoSeconds,
            samplingDurationInMilliSeconds
        )
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> JvmUtility.getThreadCpuUsages(
            initialThreadCpuTimesInNanoSeconds,
            null,
            samplingDurationInMilliSeconds
        )
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> JvmUtility.getThreadCpuUsages(
            initialThreadCpuTimesInNanoSeconds,
            currentThreadCpuTimesInNanoSeconds,
            null
        )
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> JvmUtility.getThreadCpuUsages(
            null,
            null,
            samplingDurationInMilliSeconds
        )
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> JvmUtility.getThreadCpuUsages(
            initialThreadCpuTimesInNanoSeconds,
            null,
            null
        )
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> JvmUtility.getThreadCpuUsages(
            null,
            currentThreadCpuTimesInNanoSeconds,
            null
        )
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> JvmUtility.getThreadCpuUsages(
            null,
            null,
            null
        )
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> JvmUtility.getThreadCpuUsages(
            initialThreadCpuTimesInNanoSecondsWithNullValues,
            currentThreadCpuTimesInNanoSeconds,
            samplingDurationInMilliSeconds
        )
    );
    Assertions.assertThrows(
        NullPointerException.class,
        () -> JvmUtility.getThreadCpuUsages(
            initialThreadCpuTimesInNanoSeconds,
            currentThreadCpuTimesInNanoSecondsWithNullValues,
            samplingDurationInMilliSeconds
        )
    );

    // Invalid inputs
    // currentThreadTime < initialThreadTime
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> JvmUtility.getThreadCpuUsages(
            initialThreadCpuTimesInNanoSecondsWithInitialTimeGreaterThanCurrentTime,
            currentThreadCpuTimesInNanoSecondsWithCurrentTimeLesserThanInitialTime,
            samplingDurationInMilliSeconds
        )
    );

    // initialThreadTime < 0
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> JvmUtility.getThreadCpuUsages(
            initialThreadCpuTimesInNanoSecondsWithNegativeValues,
            currentThreadCpuTimesInNanoSeconds,
            samplingDurationInMilliSeconds
        )
    );

    // currentThreadTime < 0
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> JvmUtility.getThreadCpuUsages(
            initialThreadCpuTimesInNanoSecondsWithInitialTimeGreaterThanCurrentTime,
            currentThreadCpuTimesInNanoSecondsWithNegativeValues,
            samplingDurationInMilliSeconds
        )
    );
  }

  /**
   * Tests thread CPU usage computation given valid inputs - initial thread CPU times,
   * current thread CPU times and the sampling duration.
   *
   * @param threadId          Thread ID for which the CPU usage has to be retrieved.
   * @param expectedCpuUsage  Expected CPU usage for the specified thread ID.
   */
  @ParameterizedTest
  @CsvSource({
      // Should return null as Thread 996 does not have a currentThreadCPUTime.
      "996,",
      // Should return null as Thread 996 does not have a initialThreadCPUTime.
      "999,",
      "997,7.8",
      "998,34.8",
  })
  void testGetThreadCpuUsagesWithValidInputs(long threadId, Double expectedCpuUsage) {
    Assertions.assertEquals(
        expectedCpuUsage,
        threadCpuUsages.get(threadId)
    );
  }

  /**
   * Tests initializing dependant values when an IOException is thrown while adding a property file.
   */
  @Test
  void testDependantValuesInitializer() {
    // Mock throwing IOException when adding the property file.
    try (
        MockedStatic<PropertyUtility> propertyUtilityMock = Mockito.mockStatic(
            PropertyUtility.class
        )
    ) {
      propertyUtilityMock.when(
          () -> PropertyUtility.addPropertyFile(
              ArgumentMatchers.any(),
              ArgumentMatchers.anyString()
          )
      ).thenThrow(IOException.class);

      Assertions.assertDoesNotThrow(JvmUtility.dependantValuesInitializer::run);
    }

    // Mock throwing IOException when initializing the HotSpotDiagnosticMXBean.
    try (
        MockedStatic<ManagementFactory> propertyUtilityMock = Mockito.mockStatic(
            ManagementFactory.class
        )
    ) {
      propertyUtilityMock.when(
          () -> ManagementFactory.newPlatformMXBeanProxy(
              ArgumentMatchers.any(MBeanServerConnection.class),
              ArgumentMatchers.anyString(),
              ArgumentMatchers.any()
          )
      ).thenThrow(IOException.class);

      Assertions.assertDoesNotThrow(JvmUtility.dependantValuesInitializer::run);
    }
  }

  /**
   * Tests JvmUtility::getCpuUsage() when an invalid thread ID is supplied.
   *
   * @param threadId Thread ID for which the CPu usage is to be retrieved.
   */
  @ParameterizedTest
  @CsvSource({
      "-100",
      "99999999" // Non-existent thread.
  })
  void testGetCpuUsageWithInvalidInput(long threadId) {
    if (threadId < 0) {
      Assertions.assertThrows(
              IllegalArgumentException.class,
              () -> JvmUtility.getCpuUsage(threadId)
      );
    } else {
      Assertions.assertEquals(
              -1,
              JvmUtility.getCpuUsage(threadId)
      );
    }
  }

  /**
   * Tests if a specified value is within a desired range.
   *
   * @param fieldName           Name of the field whose value is tested.
   * @param value               Value to test.
   * @param lowerLimitInclusive Inclusive lower limit of range.
   * @param upperLimitInclusive Inclusive upper limit of range.
   *
   * @param <T>                 Numeric type to test.
   */
  private <T extends Number> void testNumericRange(
      String fieldName,
      T value,
      T lowerLimitInclusive,
      T upperLimitInclusive
  ) {
    Assertions.assertTrue(
        value.doubleValue() >= lowerLimitInclusive.doubleValue()
            && value.doubleValue() <= upperLimitInclusive.doubleValue(),
        I18nUtility.getFormattedString(
            "JvmUtilityTest.error.valueOutOfNumericRange",
            fieldName,
            value,
            lowerLimitInclusive,
            upperLimitInclusive
        )
    );
  }

  /**
   * Tests JvmUtility::getAllocatedMemoryInBytes() when an invalid thread ID is supplied.
   *
   * @param threadId Thread ID for which the allocated memory size is to be retrieved.
   */
  @ParameterizedTest
  @CsvSource({
      "-100"
  })
  void testGetAllocatedMemoryInBytesWithInvalidInput(long threadId) {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> JvmUtility.getAllocatedMemoryInBytes(threadId)
    );
  }

  /**
   * Tests JvmUtility::getMemoryUsage() for heap and non heap memory.
   *
   * @param memoryType Type of memory to get usage values and test.
   */
  @ParameterizedTest
  @CsvSource({
      "heap",
      "nonHeap"
  })
  void testGetMemoryUsage(String memoryType) {
    double memoryUsagePercentageLowerLimit = -1.0;
    double memoryUsagePercentageUpperLimit = -1.0;
    long maxMemory = Long.MAX_VALUE;
    ExtendedMemoryUsage extendedMemoryUsage = null;
    if (memoryType.equals("heap")) {
      extendedMemoryUsage = JvmUtility.getHeapMemoryUsage();
      memoryUsagePercentageLowerLimit = 0.0;
      memoryUsagePercentageUpperLimit = 100.0;
      maxMemory = extendedMemoryUsage.getMax();
    } else if (memoryType.equals("nonHeap")) {
      extendedMemoryUsage = JvmUtility.getNonHeapMemoryUsage();
    }

    Assertions.assertNotNull(
        extendedMemoryUsage,
        I18nUtility.getFormattedString(
            "JvmUtilityTest.error.invalidValue",
            memoryType,
            "memoryType"
        )
    );

    testNumericRange(
        memoryType + " memoryUsagePercentage",
        extendedMemoryUsage.getMemoryUsagePercentage(),
        memoryUsagePercentageLowerLimit,
        memoryUsagePercentageUpperLimit
    );
    testNumericRange(
        memoryType + " memoryInit",
        extendedMemoryUsage.getInit(),
        0.0,
        maxMemory
    );
    testNumericRange(
        memoryType + " memoryUsed",
        extendedMemoryUsage.getUsed(),
        0.0,
        extendedMemoryUsage.getCommitted()
    );
    testNumericRange(
        memoryType + " memoryCommitted",
        extendedMemoryUsage.getCommitted(),
        extendedMemoryUsage.getUsed(),
        maxMemory
    );
    testNumericRange(
        memoryType + " memoryMax",
        maxMemory,
        extendedMemoryUsage.getCommitted(),
        maxMemory
    );
  }

  /**
   * Test JvmUtility::getGarbageCollectionInfos().
   */
  @Test
  void testGarbageCollectionInfo() {
    GarbageCollectionInfo[] garbageCollectionInfos = JvmUtility.getGarbageCollectionInfo();

    Assertions.assertNotNull(garbageCollectionInfos);

    Arrays.stream(garbageCollectionInfos)
        .peek(garbageCollectionInfo -> logger.info(
            I18nUtility.getFormattedString(
                "JvmUtilityTest.info.garbageCollectionInfo",
                garbageCollectionInfo.getName(),
                garbageCollectionInfo.getCollectionCount(),
                garbageCollectionInfo.getCollectionTime().toMillis(),
                "ms"
            )
          )
        )
        .forEach(garbageCollectionInfo -> {
          Assertions.assertNotNull(garbageCollectionInfo.getName());
          testNumericRange(
              "GC collection count",
              garbageCollectionInfo.getCollectionCount(),
              0,
              Long.MAX_VALUE
          );
          testNumericRange(
              "GC collection time",
              garbageCollectionInfo.getCollectionTime()
                  .toMillis(),
              0,
              Long.MAX_VALUE
          );
        });
  }

  /**
   * Test JvmUtility::runGarbageCollector().
   */
  @Test
  void testRunGarbageCollector() {
    Duration garbageCollectionDuration = JvmUtility.runGarbageCollector();
    Assertions.assertNotNull(garbageCollectionDuration);
    Assertions.assertTrue(garbageCollectionDuration.toMillis() >= 0);
  }

  /**
   * Tests JvmUtility::generateHeapDump() with valid inputs.
   *
   * @param heapDumpFileName                Name of the output heap dump file. May or may not
   *                                        include file extension.
   * @param expectedHeapDumpFileName        Expected name of the output heap dump file.
   * @param dumpOnlyLiveObjects             If true, it only dumps reachable objects, else dumps all
   *                                        objects in heap.
   * @throws IOException                    When there is abn issue generating the heap dump.
   */
  @ParameterizedTest
  @CsvSource({
      "heapDump, heapDump.hprof, false",
      "heapDump, heapDump.hprof, true",
      "heapDump.hprof, heapDump.hprof, false",
      "heapDump.hprof, heapDump.hprof, true"
  })
  void testGenerateHeapDumpWithValidInputs(
      String heapDumpFileName,
      String expectedHeapDumpFileName,
      boolean dumpOnlyLiveObjects
  ) throws IOException {
    Path destinationDirectoryPath = Files.createTempDirectory("heap_destination");

    Assertions.assertTrue(
        Files.exists(destinationDirectoryPath)
    );

    Duration heapDumpGenerationDuration = JvmUtility.generateHeapDump(
        destinationDirectoryPath,
        heapDumpFileName,
        dumpOnlyLiveObjects
    );

    Path expectedHeapDumpFilePath = destinationDirectoryPath.resolve(expectedHeapDumpFileName);
    Assertions.assertTrue(
        Files.exists(expectedHeapDumpFilePath)
    );

    Assertions.assertNotNull(heapDumpGenerationDuration);
    Assertions.assertTrue(heapDumpGenerationDuration.toMillis() >= 0);

    // Clean test bed.
    Files.deleteIfExists(expectedHeapDumpFilePath);
    Files.deleteIfExists(destinationDirectoryPath);
  }

  /**
   * Tests JvmUtility::generateHeapDump() with invalid inputs.
   *
   * @param destinationDirectoryPathString  Directory where the heap memory is to be dumped.
   * @param heapDumpFileName                Name of the output heap dump file.
   * @param dumpOnlyLiveObjects             If true, it only dumps reachable objects, else dumps all
   *                                        objects in heap.
   * @param expectedExceptionClass          Expected exception to be thrown.
   */
  @ParameterizedTest
  @CsvSource({
      ", , false, NullPointerException.class",
      ", , true, NullPointerException.class",
      "., , false, NullPointerException.class",
      "., , true, NullPointerException.class",
      ", heapDump, true, NullPointerException.class",
      ", heapDump, false, NullPointerException.class",
      ", heapDump.prof, true, NullPointerException.class",
      ", heapDump.prof, false, NullPointerException.class",
      "nonExistentDirectory, heapDump, true, IllegalArgumentException.class",
      "pom.xml, heapDump, true, IllegalArgumentException.class"
  })
  void testGenerateHeapDumpWithInvalidInputs(
      String destinationDirectoryPathString,
      String heapDumpFileName,
      boolean dumpOnlyLiveObjects,
      @ConvertWith(ExceptionClassConverter.class)
      Class<? extends Exception> expectedExceptionClass
  ) {
    Path destinationDirectoryPath
        = (destinationDirectoryPathString != null)
        ? Paths.get(destinationDirectoryPathString) : null;
    Assertions.assertThrows(
        expectedExceptionClass,
        () -> JvmUtility.generateHeapDump(
            destinationDirectoryPath,
            heapDumpFileName,
            dumpOnlyLiveObjects
        )
    );
  }

  /**
   * Tests JvmUtility::getAllVmOptions().
   */
  @Test
  void testGetVmOptions() {
    JsonArray vmOptions = JvmUtility.getAllVmOptions();
    Assertions.assertNotNull(vmOptions);
    Assertions.assertTrue(vmOptions.size() > 0);
  }

  /**
   * Tests JvmUtility::dumpJvmInformationToFile() with invalid inputs.
   *
   * @param destinationDirectory    Destination directory to create the file in.
   * @param fileName                Name of the output file.
   * @param threadStackDepth        Depth of the thread stack to export.
   * @param expectedExceptionClass  Expected exception to be thrown.
   */
  @ParameterizedTest
  @CsvSource({
      // Null inputs.
      ",,10,NullPointerException.class",
      ",info,10,NullPointerException.class",
      ",info.json,10,NullPointerException.class",
      ".,,10,NullPointerException.class",

      // Non existent destinationDirectory
      "nonexistent,info.json,10,IllegalArgumentException.class",
      // File path provided instead of a destination directory.
      "pom.xml,info.json,10,IllegalArgumentException.class",

      // Invalid thread stack depth.
      ".,info.json,-1,IllegalArgumentException.class",
  })
  void testDumpingJvmInformationToFileWithInvalidInputs(
      String destinationDirectory,
      String fileName,
      int threadStackDepth,
      @ConvertWith(ExceptionClassConverter.class)
      Class<? extends Exception> expectedExceptionClass
  ) {
    Path destinationDirectoryPath = (destinationDirectory == null) ? null
        : Paths.get(destinationDirectory);
    Assertions.assertThrows(
        expectedExceptionClass,
        () -> JvmUtility.dumpJvmInformationToFile(
            destinationDirectoryPath,
            fileName,
            threadStackDepth
        )
    );
  }

  /**
   * Tests JvmUtility::dumpJvmInformationToFile() with valid inputs.
   *
   * @param destinationDirectory    Destination directory to create the file in.
   * @param fileName                Name of the output file.
   * @param threadStackDepth        Depth of the thread stack to export.
   * @throws IOException            If there is an issue writing to the file.
   */
  @ParameterizedTest
  @CsvSource({
      "., jvm_info, 0, false",
      "., jvm_info, 100, false",
  })
  void testDumpingJvmInformationToFileWithValidInputs(
      String destinationDirectory,
      String fileName,
      int threadStackDepth,
      boolean errorOnWritingJson
  ) throws IOException {
    // TODO: If errorOnWritingJson is set to true, mock gsonObject.toJson() to raise

    Path destinationDirectoryPath = Paths.get(destinationDirectory);
    JvmUtility.dumpJvmInformationToFile(
        destinationDirectoryPath,
        fileName,
        threadStackDepth
    );

    String expectedFileName = fileName.endsWith(".json") ? fileName : (fileName + ".json");
    Assertions.assertEquals(
        !errorOnWritingJson,
        Files.exists(
            destinationDirectoryPath.resolve(expectedFileName)
        )
    );

    Files.delete(destinationDirectoryPath.resolve(expectedFileName));
  }
}
