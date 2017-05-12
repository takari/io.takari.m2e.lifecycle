package io.takari.m2e.incrementalbuild.core.internal.classpath;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.m2e.core.internal.equinox.DevClassPathHelper;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.incrementalbuild.classpath.ClasspathEntriesSupplier;
import io.takari.m2e.incrementalbuild.core.internal.activator.M2EIncrementalBuildActivator;

@SuppressWarnings("restriction")
@Named
@Singleton
public class EclipseClasspathEntriesSupplier implements ClasspathEntriesSupplier {

  private static final Logger log = LoggerFactory.getLogger(EclipseClasspathEntriesSupplier.class);

  private final Collection<String> classpath;

  public EclipseClasspathEntriesSupplier() {
    Set<String> classpath = new LinkedHashSet<>();
    List<Bundle> bundles = M2EIncrementalBuildActivator.getDefault().getBundles();
    for (Bundle bundle : bundles) {
      classpath.addAll(getClasspathEntries(bundle));
    }
    this.classpath = Collections.unmodifiableSet(classpath); // TODO ImmutableSet
  }

  @Override
  public Collection<String> entries() {
    return classpath;
  }

  // OSGi hides bundle physical location and classpath (for good reasons)
  // Security manager, however, requires actual physical classpath
  // The code below attempts to second-guess OSGi physical classpath

  private static LinkedHashSet<String> getClasspathEntries(Bundle bundle) {
    log.debug("getClasspathEntries(Bundle={})", bundle.toString());
    BundleFile bundleFile = getBundleFile(bundle);
    LinkedHashSet<String> entries = new LinkedHashSet<>();
    if (DevClassPathHelper.inDevelopmentMode()) {
      for (String cpe : DevClassPathHelper.getDevClassPath(bundle.getSymbolicName())) {
        File file = new File(cpe);
        if (file.exists() && file.isAbsolute()) {
          // development entries can be absolute files
          // pde "binary project with linked contents" is an example
          entries.add(cpe);
        } else {
          addBundleEntry(entries, bundleFile, cpe);
        }
      }
    }
    for (String cpe : parseBundleClasspath(bundle)) {
      addBundleEntry(entries, bundleFile, cpe);
    }
    return entries;
  }

  // I won't pretend I fully understand Equinox internal model, so don't ask
  private static BundleFile getBundleFile(Bundle bundle) {
    Module bundleModule = bundle.adapt(Module.class);
    ModuleRevision bundleRevision = bundleModule.getCurrentRevision();
    Generation bundleGeneration = (Generation) bundleRevision.getRevisionInfo();
    return bundleGeneration.getBundleFile();
  }

  private static void addBundleEntry(Set<String> entries, BundleFile bundleFile, String cpe) {
    File entry;
    if (".".equals(cpe)) {
      entry = bundleFile.getBaseFile();
    } else {
      entry = bundleFile.getFile(cpe, false);
    }
    if (entry != null) {
      log.debug("\tEntry:{}", entry);
      entries.add(entry.getAbsolutePath());
    }
  }

  // copy&paste from m2e Bundles
  private static String[] parseBundleClasspath(Bundle bundle) {
    String[] result = new String[] {"."};
    String header = bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
    ManifestElement[] classpathEntries = null;
    try {
      classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, header);
    } catch (BundleException ex) {
      log.warn("Could not parse bundle classpath of {}", bundle.toString(), ex);
    }
    if (classpathEntries != null) {
      result = new String[classpathEntries.length];
      for (int i = 0; i < classpathEntries.length; i++) {
        result[i] = classpathEntries[i].getValue();
      }
    }
    return result;
  }

}
