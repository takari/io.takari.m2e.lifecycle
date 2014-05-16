package io.takari.m2e.incrementalbuild.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.plexus.util.MatchPatterns;
import org.eclipse.core.runtime.IPath;

class PathMatcher {

  private final MatchPatterns includes;

  private final MatchPatterns excludes;

  private PathMatcher(MatchPatterns includes, MatchPatterns excludes) {
    this.includes = includes;
    this.excludes = excludes;
  }

  public boolean matches(IPath path) {
    if (includes == null && excludes == null) {
      return true;
    }

    if (excludes != null && excludes.matches(path.toString(), true)) {
      return false;
    }

    if (includes != null) {
      return includes.matches(path.toString(), true);
    }

    return true;
  }

  public static PathMatcher fromStrings(Collection<String> includes, Collection<String> excludes) {
    return new PathMatcher(toMatchPatterns(includes), toMatchPatterns(excludes));
  }

  private static MatchPatterns toMatchPatterns(Collection<String> globs) {
    if (globs == null || globs.isEmpty()) {
      return null;
    }
    List<String> normalized = new ArrayList<>();
    for (String glob : globs) {
      normalized.add(glob.startsWith("**") ? glob : "**/" + glob);
    }
    return MatchPatterns.from(normalized);
  }
}
