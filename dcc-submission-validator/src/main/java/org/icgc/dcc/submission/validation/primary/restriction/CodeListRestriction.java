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
package org.icgc.dcc.submission.validation.primary.restriction;

import java.util.List;
import java.util.Set;

import org.icgc.dcc.submission.core.report.ErrorType;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.PlanElement;
import org.icgc.dcc.submission.validation.primary.core.RestrictionContext;
import org.icgc.dcc.submission.validation.primary.core.RestrictionType;
import org.icgc.dcc.submission.validation.primary.core.RestrictionTypeSchema;
import org.icgc.dcc.submission.validation.primary.core.RestrictionTypeSchema.FieldRestrictionParameter;
import org.icgc.dcc.submission.validation.primary.core.RestrictionTypeSchema.ParameterType;
import org.icgc.dcc.submission.validation.primary.core.RowBasedPlanElement;
import org.icgc.dcc.submission.validation.primary.planner.PlanningException;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

public class CodeListRestriction implements RowBasedPlanElement {

  /**
   * Name of the restriction.
   */
  public static final String NAME = "codelist";

  /**
   * Name of the codelist used by the restriction.
   */
  public static final String FIELD = "name"; // TODO: rename

  private final String field;

  private final String codeListName;

  private final Set<String> codes;

  private final Set<String> values;

  protected CodeListRestriction(String field, CodeList codeList) {
    this.field = field;
    this.codeListName = codeList.getName();
    List<Term> terms = codeList.getTerms();
    codes = Sets.newHashSet(Iterables.transform(terms, new com.google.common.base.Function<Term, String>() {

      @Override
      public String apply(Term term) {
        return term.getCode();
      }
    }));
    values = Sets.newHashSet(Iterables.transform(terms, new com.google.common.base.Function<Term, String>() {

      @Override
      public String apply(Term term) {
        return term.getValue();
      }
    }));
  }

  @Override
  public String describe() {
    return String.format("%s[%s:%s]", NAME, field, codeListName);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new InCodeListFunction(codes, values), Fields.REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionContext context;

    @Inject
    public Type(RestrictionContext context) {
      this.context = context;
    }

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema(//
        new FieldRestrictionParameter(FIELD, ParameterType.TEXT, "Name of codeList against which to check the value",
            true));

    @Override
    public String getType() {
      return NAME;
    }

    @Override
    public FlowType flowType() {
      return FlowType.ROW_BASED;
    }

    @Override
    public boolean builds(String name) {
      return getType().equals(name);
    }

    @Override
    public RestrictionTypeSchema getSchema() {
      return schema;
    }

    @Override
    public PlanElement build(String projectKey, Field field, Restriction restriction) {
      String codeListName = restriction.getConfig().getString(FIELD);
      Optional<CodeList> codeList = context.getCodeList(codeListName);
      if (codeList.isPresent() == false) {
        throw new PlanningException("Could not find codeList " + codeListName);
      }
      return new CodeListRestriction(field.getName(), codeList.get());
    }
  }

  @SuppressWarnings("rawtypes")
  public static class InCodeListFunction extends BaseOperation implements Function {

    private final Set<String> codes;

    private final Set<String> values;

    protected InCodeListFunction(Set<String> codes, Set<String> values) {
      super(2, Fields.ARGS);
      this.codes = codes;
      this.values = values;
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry tupleEntry = functionCall.getArguments();
      Object object = tupleEntry.getObject(0);
      String value = object == null ? null : object.toString();
      if (value != null && codes.contains(value) == false && values.contains(value) == false) { // TODO: see note in
                                                                                                // DCC-904
        Object fieldName = tupleEntry.getFields().get(0);
        ValidationFields.state(tupleEntry).reportError(ErrorType.CODELIST_ERROR, fieldName.toString(), value);
      }
      functionCall.getOutputCollector().add(tupleEntry.getTupleCopy());
    }

  }

}
