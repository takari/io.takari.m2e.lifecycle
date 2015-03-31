## single generated sources location
This makes it impossible to have separate generated sources location for main and test sources. This also makes it impossible to support more exotic usecases when multiple compile mojo executions are used to generate different sets of sources in different output directories.

## single source path
This makes it impossible to scope annotation processors to their expected main or test sources. This will result in test classes annotations "leakage" into main annotation processor output. For example, main sisu index will include test classes. Likewise, maven plugin xml and mojo index will include test classes. Does not happen very often, but does happen often enough to be a show-stopper, I think.

## no control over processor selection
List of processors is read from `META-INF/services/javax.annotation.processing.Processor` elements of processor path entries. It is not possible to provide explicit list, i.e. similar to javac `-processor` parameter.
