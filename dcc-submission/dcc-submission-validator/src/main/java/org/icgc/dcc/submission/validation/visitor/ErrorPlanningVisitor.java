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
package org.icgc.dcc.submission.validation.visitor;

import static org.icgc.dcc.submission.validation.cascading.TupleStates.keepInvalidTuplesFilter;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.STATE_FIELD;

import java.io.FileNotFoundException;
import java.io.InputStream;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.PlanExecutionException;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.core.FlowType;
import org.icgc.dcc.submission.validation.core.ReportingPlanElement;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.report.ReportCollector;
import org.icgc.dcc.submission.validation.report.ReportContext;

import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Retain;

public class ErrorPlanningVisitor extends ReportingFlowPlanningVisitor {

  public ErrorPlanningVisitor(FlowType type) {
    super(type);
  }

  @Override
  public void visit(FileSchema fileSchema) {
    super.visit(fileSchema);
    collect(new ErrorsPlanElement(fileSchema, this.getFlowType()));
  }

  static class ErrorsPlanElement implements ReportingPlanElement {

    private final FileSchema fileSchema;

    private final FlowType flowType;

    public ErrorsPlanElement(FileSchema fileSchema, FlowType flowType) {
      this.fileSchema = fileSchema;
      this.flowType = flowType;
    }

    @Override
    public String getName() {
      return "errors";
    }

    @Override
    public String describe() {
      return String.format("errors");
    }

    @Override
    public Pipe report(Pipe pipe) {
      return new Retain(new Each(pipe, keepInvalidTuplesFilter()), STATE_FIELD);
    }

    public FileSchema getFileSchema() {
      return this.fileSchema;
    }

    public FlowType getFlowType() {
      return this.flowType;
    }

    @Override
    public ReportCollector getCollector() {
      return new ErrorReportCollector();
    }

    class ErrorReportCollector implements ReportCollector {

      @Override
      public void collect(PlatformStrategy strategy, ReportContext context) {
        try {
          Path path = strategy.path(getFileSchema());
          val fileName = path.getName();

          @Cleanup
          val reportInputStream = getReportInputStream(strategy);
          val tupleStates = getTupleStates(reportInputStream);

          while (tupleStates.hasNext()) {
            val tupleState = tupleStates.next();
            if (tupleState.isInvalid()) {
              for (val errorTuple : tupleState.getErrors()) {
                context.reportError(fileName, errorTuple);
              }
            }
          }

          context.reportLineNumbers(path);
        } catch (FileNotFoundException fnfe) {
          // There were no errors
        } catch (Exception e) {
          throw new PlanExecutionException(e);
        }
      }

      @SneakyThrows
      private InputStream getReportInputStream(PlatformStrategy strategy) {
        return strategy.readReportTap(getFileSchema(), getFlowType(), getName());
      }

      @SneakyThrows
      private MappingIterator<TupleState> getTupleStates(InputStream reportInputStream) {
        val reader = new ObjectMapper().reader().withType(TupleState.class);

        return reader.readValues(reportInputStream);
      }
    }

  }

}
