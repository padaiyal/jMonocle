package org.padaiyal.utilities.filesystem.abstractions;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test methods for WatchServiceRegisteringFileVisitor objects.
 */
public class WatchServiceRegisteringFileVisitorTest {

  /**
   * Test that visitFailed modifies the correct attributes.
   */
  @Test
  public void testVisitFailed() {
    Path path = Paths.get(".");
    WatchEvent.Kind<?>[] watchEvents = new WatchEvent.Kind<?>[]{};
    BiConsumer<Path, WatchEvent<?>> consumer = (eventPath, event) -> {};
    WatchServiceRegisteringFileVisitor fileVisitor = new WatchServiceRegisteringFileVisitor(path,
        10,
        watchEvents,
        consumer
    );
    long previousVisitsCount = fileVisitor.getFailedVisitsCount();
    FileVisitResult result = fileVisitor.visitFileFailed(path, new IOException());
    Assertions.assertEquals(FileVisitResult.SKIP_SUBTREE, result);
    long newestVisitsCount = fileVisitor.getFailedVisitsCount();
    Assertions.assertEquals(previousVisitsCount + 1, newestVisitsCount);
  }

}
