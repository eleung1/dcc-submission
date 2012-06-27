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
package org.icgc.dcc.validation.plan;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Set;

import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.validation.FileSchemaDirectory;
import org.icgc.dcc.validation.PlanningVisitor;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.visitor.RelationPlanningVisitor;
import org.icgc.dcc.validation.visitor.RestrictionPlanningVisitor;
import org.icgc.dcc.validation.visitor.UniqueFieldsPlanningVisitor;
import org.icgc.dcc.validation.visitor.ValueTypePlanningVisitor;

import cascading.cascade.Cascade;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class DefaultPlanner implements Planner {

  private final List<PlanningVisitor> internalFlowVisitors;

  private final List<PlanningVisitor> externalFlowVisitors;

  private final CascadingStrategy cascadingStrategy;

  @Inject
  public DefaultPlanner(Set<RestrictionType> restrictionTypes, CascadingStrategy cascadingStrategy) {
    checkArgument(restrictionTypes != null);
    checkArgument(cascadingStrategy != null);
    this.cascadingStrategy = cascadingStrategy;
    internalFlowVisitors =
        ImmutableList.of(new ValueTypePlanningVisitor(), new UniqueFieldsPlanningVisitor(),
            new RestrictionPlanningVisitor(PlanPhase.INTERNAL, restrictionTypes));
    externalFlowVisitors =
        ImmutableList.of(new RelationPlanningVisitor(), new RestrictionPlanningVisitor(PlanPhase.EXTERNAL,
            restrictionTypes));
  }

  @Override
  public Cascade plan(FileSchemaDirectory directory, Dictionary dictionary) {
    Plan plan = new Plan();
    for(FileSchema fileSchema : dictionary.getFiles()) {
      if(directory.hasFile(fileSchema)) {
        plan.include(fileSchema, new DefaultInternalFlowPlanner(plan, fileSchema), new DefaultExternalFlowPlanner(plan,
            fileSchema));
      }
    }
    plan.apply(internalFlowVisitors);
    plan.apply(externalFlowVisitors);

    return plan.connect(cascadingStrategy);
  }

  @Override
  public CascadingStrategy getCascadingStrategy() {
    return cascadingStrategy;
  }

}
