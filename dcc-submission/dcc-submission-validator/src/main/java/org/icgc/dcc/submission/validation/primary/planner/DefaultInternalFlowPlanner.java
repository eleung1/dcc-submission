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
package org.icgc.dcc.submission.validation.primary.planner;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.REPLACE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.validation.cascading.StructuralCheckFunction.LINE_FIELD_NAME;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.OFFSET_FIELD_NAME;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.cascading.ForbiddenValuesFunction;
import org.icgc.dcc.submission.validation.cascading.RemoveEmptyValidationLineFilter;
import org.icgc.dcc.submission.validation.cascading.RemoveHeaderFilter;
import org.icgc.dcc.submission.validation.cascading.StructuralCheckFunction;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.cascading.TupleStates;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;
import org.icgc.dcc.submission.validation.core.ErrorType;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.primary.DuplicateHeaderException;
import org.icgc.dcc.submission.validation.primary.PlanningFileLevelException;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.InternalPlanElement;
import org.icgc.dcc.submission.validation.primary.core.Key;
import org.icgc.dcc.submission.validation.primary.restriction.RequiredRestriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.flow.FlowDef;
import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Retain;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;

class DefaultInternalFlowPlanner extends BaseFileSchemaFlowPlanner implements InternalFlowPlanner {

  private static final Logger log = LoggerFactory.getLogger(DefaultInternalFlowPlanner.class);

  private final Pipe head;

  private Pipe structurallyValidTail;

  private Pipe structurallyInvalidTail;

  private final Map<Key, Pipe> trimmedTails = Maps.newHashMap();

  private StructuralCheckFunction structuralCheck;

  DefaultInternalFlowPlanner(FileSchema fileSchema) {
    super(fileSchema, FlowType.INTERNAL);
    this.head = new Pipe(fileSchema.getName());

    // apply system pipe
    applySystemPipes(this.head);
  }

  @Override
  public void apply(InternalPlanElement element) {
    checkArgument(element != null);
    log.info("[{}] applying element [{}]", getName(), element.describe());
    structurallyValidTail = element.extend(structurallyValidTail);
  }

  @Override
  public Key addTrimmedOutput(String... fields) {
    checkArgument(fields != null);
    checkArgument(fields.length > 0);

    String[] keyFields = // in order to obtain offset of referencing side
        ObjectArrays.concat(fields, ValidationFields.OFFSET_FIELD_NAME);

    Key key = new Key(getSchema(), keyFields);
    if (trimmedTails.containsKey(key) == false) {
      String[] preKeyFields = ObjectArrays.concat(fields, ValidationFields.STATE_FIELD_NAME);

      Pipe newHead = new Pipe(key.getName(), structurallyValidTail);
      Pipe tail = new Retain(newHead, new Fields(preKeyFields));
      tail = new Each(tail, ValidationFields.STATE_FIELD, new OffsetFunction(), Fields.SWAP);

      log.info("[{}] planned trimmed output with {}", getName(), Arrays.toString(key.getFields()));
      trimmedTails.put(key, tail);
    }
    return key;
  }

  @Override
  protected Pipe getTail(String basename) {
    Pipe valid = new Pipe(basename + "_valid", structurallyValidTail);
    Pipe invalid = new Pipe(basename + "_invalid", structurallyInvalidTail);
    return new Merge(valid, invalid);
  }

  @Override
  protected Pipe getStructurallyValidTail() {
    return structurallyValidTail;
  }

  @Override
  protected Pipe getStructurallyInvalidTail() {
    return structurallyInvalidTail;
  }

  @Override
  protected FlowDef onConnect(FlowDef flowDef, PlatformStrategy strategy) {
    checkState(structuralCheck != null);
    Tap<?, ?, ?> source = strategy.getSourceTap(getSchema());
    try {
      // TODO: address trick to know what the header contain: DCC-996
      Fields header = strategy.getFileHeader(getSchema());
      structuralCheck.declareFieldsPostPlanning(header);

    } catch (IOException e) {
      throw new PlanningException("Error processing file header");
    } catch (DuplicateHeaderException e) {
      String fileName = null;
      try {
        fileName = strategy.path(getSchema()).getName();
      } catch (FileNotFoundException fnfe) {
        throw new PlanningException(fnfe);
      } catch (IOException ioe) {
        throw new PlanningException(ioe);
      }
      throw new PlanningFileLevelException(fileName, ErrorType.DUPLICATE_HEADER_ERROR,
          e.getDuplicateHeaderFieldNames());
    }

    flowDef.addSource(head, source);

    for (Map.Entry<Key, Pipe> e : trimmedTails.entrySet()) {
      flowDef.addTailSink(e.getValue(), strategy.getTrimmedTap(e.getKey()));
    }
    return flowDef;
  }

  private void applySystemPipes(Pipe pipe) {
    pipe = new Each(pipe, new RemoveEmptyValidationLineFilter());
    pipe = new Each(pipe, new RemoveHeaderFilter());
    pipe = applyStructuralCheck(pipe);

    // TODO: DCC-1076 - Would be better done from within {@link RequiredRestriction}.
    pipe = new Each(pipe, ALL, new ForbiddenValuesFunction(computeRequiredFieldnames()), REPLACE);

    this.structurallyValidTail = new Each(pipe, TupleStates.keepStructurallyValidTuplesFilter());
    this.structurallyInvalidTail = new Each(pipe, TupleStates.keepStructurallyInvalidTuplesFilter());
  }

  private Pipe applyStructuralCheck(Pipe pipe) {
    structuralCheck = new StructuralCheckFunction(getSchema().getFieldNames()); // TODO: due for a splitting
    return new Each( // parse "line" into the actual expected fields
        pipe, new Fields(OFFSET_FIELD_NAME, LINE_FIELD_NAME), structuralCheck, Fields.SWAP);
  }

  /**
   * Returns the list of field names that have a {@link RequiredRestriction} set on them (irrespective of whether it's a
   * strict one or not).
   * <p>
   * TODO: DCC-1076 will render it unnecessary (everything would take place in {@link RequiredRestriction}).
   */
  private List<String> computeRequiredFieldnames() {
    List<String> requiredFieldnames = newArrayList();
    for (Field field : getSchema().getFields()) {
      if (field.hasRequiredRestriction()) {
        requiredFieldnames.add(field.getName());
      }
    }
    return copyOf(requiredFieldnames);
  }

  @SuppressWarnings("rawtypes")
  static class OffsetFunction extends BaseOperation implements Function {

    public OffsetFunction() {
      super(1, new Fields(ValidationFields.OFFSET_FIELD_NAME));
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry entry = functionCall.getArguments();
      TupleState state = ValidationFields.state(entry);
      functionCall.getOutputCollector().add(new Tuple(state.getOffset()));
    }
  }
}
