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
package org.icgc.dcc.submission.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Set;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.validation.report.ErrorPlanningVisitor;
import org.icgc.dcc.submission.validation.report.SummaryPlanningVisitor;
import org.icgc.dcc.submission.validation.visitor.ExternalRestrictionPlanningVisitor;
import org.icgc.dcc.submission.validation.visitor.InternalRestrictionPlanningVisitor;
import org.icgc.dcc.submission.validation.visitor.RelationPlanningVisitor;
import org.icgc.dcc.submission.validation.visitor.UniqueFieldsPlanningVisitor;
import org.icgc.dcc.submission.validation.visitor.ValueTypePlanningVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class DefaultPlanner implements Planner {

  private static final Logger log = LoggerFactory.getLogger(DefaultPlanner.class);

  private final List<? extends PlanningVisitor<?>> planningVisitors;

  @Inject
  public DefaultPlanner(Set<RestrictionType> restrictionTypes) {
    checkArgument(restrictionTypes != null);
    planningVisitors = ImmutableList.of(
        // Internal
        new ValueTypePlanningVisitor(), // Must happen before RangeRestriction
        new UniqueFieldsPlanningVisitor(),
        new InternalRestrictionPlanningVisitor(restrictionTypes),
        // Reporting
        new SummaryPlanningVisitor(),
        new ErrorPlanningVisitor(FlowType.INTERNAL),
        // External
        new RelationPlanningVisitor(),
        new ExternalRestrictionPlanningVisitor(restrictionTypes),
        new ErrorPlanningVisitor(FlowType.EXTERNAL));
  }

  @Override
  public Plan plan(QueuedProject queuedProject, SubmissionDirectory submissionDirectory, CascadingStrategy strategy,
      Dictionary dictionary) {
    checkArgument(strategy != null);
    checkArgument(dictionary != null);

    FileSchemaDirectory systemDirectory = strategy.getSystemDirectory();

    Plan plan = new Plan(queuedProject, dictionary, strategy, submissionDirectory);
    for (FileSchema fileSchema : dictionary.getFiles()) {
      try {
        FileSchemaDirectory fileSchemaDirectory = strategy.getFileSchemaDirectory();
        String fileSchemaName = fileSchema.getName();
        if (fileSchemaDirectory.hasFile(fileSchema) || systemDirectory.hasFile(fileSchema)) {
          log.info("including flow planners for file schema {}", fileSchemaName);
          plan.include(fileSchema, new DefaultInternalFlowPlanner(fileSchema), new DefaultExternalFlowPlanner(plan,
              fileSchema));
        } else {
          log.info("file schema {} has no matching datafile in submission directory {}", fileSchemaName,
              fileSchemaDirectory.getDirectoryPath());
        }
      } catch (PlanningFileLevelException e) {
        plan.addFileLevelError(e);
      }
    }
    for (PlanningVisitor<?> visitor : planningVisitors) {
      try {
        visitor.apply(plan);
      } catch (PlanningFileLevelException e) {
        plan.addFileLevelError(e);
      }
    }

    return plan;
  }
}
