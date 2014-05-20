package io.takari.m2e.incrementalbuild.core.internal;

import io.takari.incrementalbuild.workspace.Workspace;
import io.takari.m2e.incrementalbuild.core.internal.workspace.ThreadLocalBuildWorkspace;

import java.util.Iterator;

import org.apache.maven.classrealm.ClassRealmConstituent;
import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.classrealm.ClassRealmRequest;
import org.apache.maven.classrealm.ClassRealmRequest.RealmType;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.m2e.core.internal.embedder.IMavenComponentContributor;

@SuppressWarnings("restriction")
public class MavenComponentContributor
    implements
      IMavenComponentContributor,
      ClassRealmManagerDelegate {

  @Override
  public void contribute(IMavenComponentBinder binder) {
    binder.bind(ClassRealmManagerDelegate.class, getClass(), getClass().getName());
    binder.bind(Workspace.class, ThreadLocalBuildWorkspace.class, null);
  }

  @Override
  public void setupRealm(ClassRealm classRealm, ClassRealmRequest request) {
    if (request.getType() == RealmType.Plugin) {
      for (Iterator<ClassRealmConstituent> iter = request.getConstituents().iterator(); iter
          .hasNext();) {
        ClassRealmConstituent entry = iter.next();
        if ("io.takari".equals(entry.getGroupId())
            && "incrementalbuild-workspace".equals(entry.getArtifactId())) {
          iter.remove();
          ClassLoader cl = Workspace.class.getClassLoader();
          request.getForeignImports().put("io.takari.incrementalbuild.workspace", cl);
        }
      }
    }
  }
}
