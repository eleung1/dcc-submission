/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.fs;

import static org.icgc.dcc.submission.core.security.Authorizations.hasSpecificProjectPrivilege;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.Submission;
import org.springframework.security.core.Authentication;

@Slf4j
@AllArgsConstructor
public class ReleaseFileSystem {

  /**
   * System files directory name.
   */
  public static final String SYSTEM_FILES_DIR_NAME = ".system";

  /**
   * Dependencies.
   */
  @NonNull
  private final SubmissionFileSystem submissionFileSystem;
  @NonNull
  private final Release release;
  @NonNull
  private final Map<String, Submission> submissionsByProjectKey;
  private final Authentication authentication;

  public ReleaseFileSystem(SubmissionFileSystem submissionFileSystem, Release release,
      Map<String, Submission> submissionsByProjectKey) {
    this(submissionFileSystem, release, submissionsByProjectKey, null);
  }

  public SubmissionDirectory getSubmissionDirectory(@NonNull String projectKey) {
    val allowed = hasPrivileges(projectKey);
    if (!allowed) {
      throw new SubmissionFileSystemException(
          "User '%s' with principal '%s' does not have permission to access project '%s'",
          authentication, authentication == null ? null : authentication.getName(), projectKey);
    }

    val submission = submissionsByProjectKey.get(projectKey);
    if (submission == null) {
      throw new ReleaseException("There is no project '%s' associated with release '%s'",
          projectKey, release.getName());
    }

    return new SubmissionDirectory(submissionFileSystem, this, release, projectKey, submission);
  }

  public void setUpNewReleaseFileSystem(
      String newReleaseName,
      @NonNull ReleaseFileSystem previous,
      @NonNull Iterable<String> projectKeys) {
    log.info("Setting up new release file system for: '{}'", newReleaseName);

    // Shorthands
    val fileSystem = submissionFileSystem.getFileSystem();
    val next = this;

    submissionFileSystem.createReleaseDirectory(newReleaseName);

    for (val projectKey : projectKeys) {
      // Copy "release_(n-1)/projectKey/" to "release_(n)/projectKey/"
      copySubmissionDir(projectKey, previous, next, fileSystem);
    }

    // Copy "release_(n-1)/.system/" to "release_(n)/.system/"
    copySystemDir(previous, next, fileSystem);
  }

  public void resetValidationFolder(@NonNull String projectKey) {
    val validationStringPath = submissionFileSystem.buildValidationDirStringPath(release.getName(), projectKey);
    submissionFileSystem.removeDirIfExist(validationStringPath);
    submissionFileSystem.createDirIfDoesNotExist(validationStringPath);
    log.info("Emptied directory '{}' for project '{}'", validationStringPath, projectKey);
  }

  public boolean isReadOnly() {
    return ReleaseState.COMPLETED == release.getState();
  }

  public SubmissionFileSystem getSubmissionFileSystem() {
    return submissionFileSystem;
  }

  public Release getRelease() {
    return release;
  }

  public Path getReleaseDirectory() {
    return new Path(this.submissionFileSystem.getRootStringPath(), this.release.getName());
  }

  protected Path getSystemDirPath() {
    return new Path(this.getReleaseDirectory(), SYSTEM_FILES_DIR_NAME);
  }

  public boolean isSystemDirectory(Path path) {
    return getSystemDirPath().getName().equals(path.getName());
  }

  private boolean isApplication() {
    return authentication == null;
  }

  private boolean hasPrivileges(String projectKey) {
    return isApplication() || hasSpecificProjectPrivilege(authentication, projectKey);
  }

  private static void copySubmissionDir(String projectKey, ReleaseFileSystem previous, ReleaseFileSystem next,
      FileSystem fileSystem) {
    val previousSubmissionDir = getSubmissionDir(previous, projectKey);
    val nextSubmissionDir = getSubmissionDir(next, projectKey);

    HadoopUtils.cp(fileSystem, previousSubmissionDir, nextSubmissionDir);
  }

  private static void copySystemDir(ReleaseFileSystem previous, ReleaseFileSystem next, FileSystem fileSystem) {
    val sourceSystemDir = previous.getSystemDirPath();
    val targetSystemDir = next.getSystemDirPath();

    HadoopUtils.cp(fileSystem, sourceSystemDir, targetSystemDir);
  }

  private static Path getSubmissionDir(ReleaseFileSystem releaseFileSystem, String projectKey) {
    val submissionDirectory = releaseFileSystem.getSubmissionDirectory(projectKey);
    val text = submissionDirectory.getSubmissionDirPath();

    return new Path(text);
  }

}
