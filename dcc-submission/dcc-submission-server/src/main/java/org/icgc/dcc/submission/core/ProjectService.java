/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.subject.Subject;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.QProject;
import org.icgc.dcc.submission.core.morphia.BaseMorphiaService;
import org.icgc.dcc.submission.core.util.NameValidator;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.model.QRelease;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.shiro.AuthorizationPrivileges;
import org.icgc.dcc.submission.web.DuplicateNameException;
import org.icgc.dcc.submission.web.InvalidNameException;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.Predicate;

public class ProjectService extends BaseMorphiaService<Project> {

  private final DccFileSystem fs;

  @Inject
  public ProjectService(Morphia morphia, Datastore datastore, DccFileSystem fs, MailService mailService) {
    super(morphia, datastore, QProject.project, mailService);
    super.registerModelClasses(Project.class);
    this.fs = fs;
  }

  public List<Release> getReleases(Project project) {
    List<Release> releases = new ArrayList<Release>();
    for (Release release : listReleases()) {
      for (Submission submission : release.getSubmissions()) {
        if (submission.getProjectKey().equals(project.getKey())) {
          releases.add(release);
          continue;
        }
      }
    }

    return releases;
  }

  @SuppressWarnings("all")
  public void addProject(Project project) {
    // check for project key
    if (!NameValidator.validateProjectId(project.getKey())) {
      throw new InvalidNameException(project.getKey());
    }
    // check for duplicate project key
    if (this.hasProject(project.getKey())) {
      throw new DuplicateNameException(project.getKey());
    }

    this.saveProject(project);

    Release release = getOpenedRelease(QRelease.release.state.eq(ReleaseState.OPENED));
    Submission submission = new Submission();
    submission.setProjectKey(project.getKey());
    submission.setState(SubmissionState.NOT_VALIDATED);
    release.addSubmission(submission);
    fs.mkdirProjectDirectory(release.getName(), project.getKey());

    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", release.getName());
    UpdateOperations<Release> ops = datastore().createUpdateOperations(Release.class).add("submissions", submission);
    datastore().update(updateQuery, ops);
  }

  public List<Project> getProjects() {
    return this.query().list();
  }

  /**
   * Returns a filtered list of projects based on authorizations for the given user.
   */
  public List<Project> getProjectsBySubject(Subject user) {
    List<Project> filteredProjects = new ArrayList<Project>();
    for (Project project : this.getProjects()) {
      if (user.isPermitted(AuthorizationPrivileges.projectViewPrivilege(project.getKey()))) {
        filteredProjects.add(project);
      }
    }
    return filteredProjects;
  }

  public Project getProject(final String projectKey) {
    Project project = this.getProjectQuery(projectKey).singleResult();

    if (project == null) {
      throw new ProjectServiceException("No project found with key " + projectKey);
    }

    return project;
  }

  public boolean hasProject(final String projectKey) {
    Project project = this.getProjectQuery(projectKey).singleResult();

    return project != null;
  }

  public List<Project> getProjects(List<String> projectKeys) {
    List<Project> projects = this.query().where(QProject.project.key.in(projectKeys)).list();
    if (projects == null) {
      throw new ProjectServiceException("No projects found within the key list");
    }
    return projects;
  }

  private void saveProject(Project project) {
    this.datastore().save(project);
  }

  private MongodbQuery<Project> getProjectQuery(final String projectKey) {
    return this.query().where(QProject.project.key.eq(projectKey));
  }

  private Release getOpenedRelease(Predicate mysemaPredicate) {
    return getReleaseQuery()
        .where(mysemaPredicate)
        .singleResult();
  }

  private List<Release> listReleases() {
    return getReleaseQuery().list();
  }

  private MorphiaQuery<Release> getReleaseQuery() {
    return new MorphiaQuery<Release>(morphia(), datastore(), QRelease.release);
  }

}
