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
package org.icgc.dcc.submission.normalization.steps;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.hadoop.cascading.Fields2.fields;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.normalization.NormalizationStep;
import org.icgc.dcc.submission.normalization.OptionalStep;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

/**
 * TODO
 */
@RequiredArgsConstructor
@Slf4j
public class RedundantObservationRemoval implements NormalizationStep, OptionalStep {

  /**
   * Short name for the step.
   */
  private static final String SHORT_NAME = "duplicates";

  // private static final Fields ANALYSIS_ID_FIELD = new Fields(SUBMISSION_OBSERVATION_ANALYSIS_ID);

  /**
   * The list of field names on which the GROUP BY should take place, and that will allow detecting duplicate
   * observations.
   */
  private final ImmutableList<String> group;

  /**
   * Name of the field on which to perform the secondary sort.
   */
  private final String secondarySortFieldName;

  @Override
  public boolean isEnabled(Object config) {
    return true; // TODO
  }

  @Override
  public String name() {
    return SHORT_NAME;
  }

  @Override
  public Pipe extend(Pipe pipe) {
    final class FilterRedundantObservationBuffer extends BaseOperation<Void> implements Buffer<Void> {

      private FilterRedundantObservationBuffer() {
        super(ARGS);
      }

      @Override
      public void operate(
          @SuppressWarnings("rawtypes")
          FlowProcess flowProcess,
          BufferCall<Void> bufferCall) {

        val group = bufferCall.getGroup();
        val tuples = bufferCall.getArgumentsIterator();
        checkState(tuples.hasNext(), "There should always be at least one item for a given group, none for '{}'", group);
        val first = tuples.next().getTupleCopy();

        val duplicates = tuples.hasNext();
        if (duplicates) {
          while (tuples.hasNext()) {
            val duplicate = tuples.next().getTuple();
            log.info("Found a duplicate of '{}' (group '{}'): ", // Should be rare enough an event
                new Object[] { first, group, duplicate });
          }
          // TODO: add report
        } else {
          log.debug("No duplicates found for '{}'", group);
        }

        bufferCall
            .getOutputCollector()
            .add(first);
      }
    }

    pipe =
        new GroupBy(
            pipe,
            groupByFields(),
            secondarySortFields());

    return new Every(
        pipe,
        ALL,
        new FilterRedundantObservationBuffer(),
        Fields.REPLACE); // TODO: or replace??
  }

  private Fields groupByFields() {
    return fields(group);
  }

  private Fields secondarySortFields() {
    return new Fields(secondarySortFieldName);
  }

}
