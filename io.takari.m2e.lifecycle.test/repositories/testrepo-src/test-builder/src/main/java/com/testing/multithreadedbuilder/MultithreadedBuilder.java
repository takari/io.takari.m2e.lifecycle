package com.testing.multithreadedbuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import io.takari.builder.Builder;
import io.takari.builder.InputDirectoryFiles;
import io.takari.builder.LifecyclePhase;
import io.takari.builder.OutputDirectory;
import io.takari.builder.enforcer.PolicyContextPreserver;

public class MultithreadedBuilder {
  
  @InputDirectoryFiles(defaultValue="${project.basedir}/resources", defaultIncludes={"**/*"})
  private List<Path> inputFiles;
  
  @OutputDirectory(defaultValue="${project.build.directory}/out")
  private Path outputDirectory;
  
  /**
   * 
   * @throws Exception
   */
  @Builder(name="build-multithreaded", defaultPhase=LifecyclePhase.PROCESS_RESOURCES)
  public void execute () throws Exception {
    PolicyContextPreserver preserver = new PolicyContextPreserver();
    ExecutorService exec = Executors.newWorkStealingPool(Math.min(7, inputFiles.size()));
    List<Future<?>> futures = inputFiles.stream().map((p)->exec.submit(preserver.wrap(getCallable(p)))).collect(Collectors.toList());
    futures.forEach((f)->{try {f.get();}catch(Exception e){throw new RuntimeException(e);}});
    exec.shutdown();
  }
  
  public Callable<Path> getCallable(Path path) {
    return new Callable<Path>() {

      @Override
      public Path call() throws Exception {
        return Files.copy(path, outputDirectory.resolve(path.getFileName()));
      }
      
    };
  }
}
