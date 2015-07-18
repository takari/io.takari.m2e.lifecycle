# Implemented mixed approach

The idea shamelessly borrowed from [m2e-apt](https://github.com/jbosstools/m2e-apt/). When annotation processing is enabled for `compile` or `testCompile` goals, m2e project configurator will do the following:

* during workspace project import and configuration update, generated sources directory is configured as Eclipse workspace project sources folder
* during workspace project build, execute the target goal with `proc=only` and `compilerId=jdt` parameters

Since `compilerId=jdt` is properly incremental and `proc=only` does not interfere with workspace JavaBuilder, the end-result is properly incremental workspace annotation processing. 

This approach maybe slower and require more memory than native JDT support, but only processes affected inputs and actual performance will likely be acceptable for all but really large projects.

Known problems and limitations

* [Bug 447546](https://bugs.eclipse.org/bugs/show_bug.cgi?id=447546) affects annotation processors that generate single output from multiple sources during last annotation processing round. This will be solved when new API are introduced in takari-lifecycle.


# JDT build-it annotation processing support limitations

## single generated sources location
This makes it impossible to have separate generated sources location for main and test sources. This also makes it impossible to support more exotic usecases when multiple compile mojo executions are used to generate different sets of sources in different output directories.

## single source path
This makes it impossible to scope annotation processors to their expected main or test sources. This will result in test classes annotations "leakage" into main annotation processor output. For example, main sisu index will include test classes. Likewise, maven plugin xml and mojo index will include test classes. Does not happen very often, but does happen often enough to be a show-stopper, I think.

## no control over processor selection
List of processors is read from `META-INF/services/javax.annotation.processing.Processor` elements of processor path entries. It is not possible to provide explicit list, i.e. similar to javac `-processor` parameter.
