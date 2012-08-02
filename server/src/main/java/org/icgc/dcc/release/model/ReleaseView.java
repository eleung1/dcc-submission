/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.release.model;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.release.ReleaseException;

/**
 * 
 */
public class ReleaseView extends Release {

  protected List<DetailedSubmission> submissions = new ArrayList<DetailedSubmission>();

  public ReleaseView() {
    super();
  }

  public ReleaseView(Release release, List<Project> projects) {
    super();
    this.name = release.name;
    this.state = release.state;
    this.queue = release.getQueue();
    this.releaseDate = release.releaseDate;
    this.dictionaryVersion = release.dictionaryVersion;
    for(Submission submission : release.getSubmissions()) {
      this.submissions.add(new DetailedSubmission(submission));
    }
    for(Project project : projects) {
      this.getDetailedSubmission(project.getKey()).setProjectName(project.getName());
    }
  }

  public DetailedSubmission getDetailedSubmission(String projectKey) {
    for(DetailedSubmission submission : submissions) {
      if(submission.getProjectKey().equals(projectKey)) {
        return submission;
      }
    }
    throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
        this.name));
  }

  public List<DetailedSubmission> getDetailedSubmissions() {
    return submissions;
  }

  public static class DetailedSubmission extends Submission {
    private String projectName;

    public DetailedSubmission() {
      super();
    }

    public DetailedSubmission(Submission submission) {
      super();
      this.projectKey = submission.projectKey;
      this.state = submission.state;
      this.report = submission.report;
    }

    public String getProjectName() {
      return projectName;
    }

    public void setProjectName(String projectName) {
      this.projectName = projectName;
    }
  }

}
