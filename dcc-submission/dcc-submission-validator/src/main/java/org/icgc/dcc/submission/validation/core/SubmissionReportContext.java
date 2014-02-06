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
package org.icgc.dcc.submission.validation.core;

import static com.google.common.base.Throwables.propagate;
import static org.icgc.dcc.submission.validation.cascading.TupleState.createTupleError;

import java.io.IOException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;

/**
 * Wraps and "adapts" a {@link SubmissionReport}.
 */
@Value
@RequiredArgsConstructor
@Slf4j
public class SubmissionReportContext implements ReportContext {

  /**
   * Adaptee.
   */
  @NonNull
  SubmissionReport submissionReport;

  /**
   * Total number of errors encountered at a point in time.
   */
  @NonFinal
  int errorCount;

  public SubmissionReportContext() {
    this(new SubmissionReport());
  }

  @Override
  public void reportSummary(String fileName, String name, String value) {
    val schemaReport = resolveSchemaReport(fileName);
    val summaryReport = new SummaryReport(name, value);

    schemaReport.addSummaryReport(summaryReport);
  }

  @Override
  public void reportField(String fileName, FieldReport fieldReport) {
    addFieldReport(fileName, fieldReport);
  }

  @Override
  public void reportError(Error error) {
    val tupleError = createTupleError(
        error.getType(),
        error.getNumber(),
        error.getFieldNames().toString(),
        error.getValue(),
        error.getLineNumber(),
        error.getParams());

    addErrorTuple(error.getFileName(), tupleError);
  }

  @Override
  public boolean hasErrors() {
    return errorCount > 0;
  }

  @Override
  public void reportLineNumbers(Path path) {
    val fileName = path.getName();
    val schemaReport = submissionReport.getSchemaReport(fileName);
    val missing = schemaReport == null;
    if (missing) {
      // This could happen for optional files (which don't report statistics) that don't have errors
      log.warn("No schema report found for name '{}' with path '{}'. Skipping...", path.getName(), path);

      val emptySchemaReport = new SchemaReport();
      emptySchemaReport.setName(fileName);
      submissionReport.addSchemaReport(emptySchemaReport);

      return;
    }

    for (val errorReport : schemaReport.getErrors()) {
      try {
        errorReport.updateLineNumbers(path);
      } catch (IOException e) {
        log.error("Exception updating line numbers for: '{}'", path);
        propagate(e);
      }
    }
  }

  private void addErrorTuple(String fileName, TupleError tupleError) {
    errorCount++;

    val schemaReport = resolveSchemaReport(fileName);
    addErrorTuple(schemaReport, tupleError);
  }

  private void addErrorTuple(SchemaReport schemaReport, TupleError tupleError) {
    val errorReports = schemaReport.getErrors();
    for (val errorReport : errorReports) {
      val exists = errorReport.getErrorType() == tupleError.getType() &&
          errorReport.getNumber() == tupleError.getNumber();

      if (exists) {
        // Reuse, no need to continue
        errorReport.updateReport(tupleError);

        return;
      }
    }

    // Seed on first use
    val errorReport = new ErrorReport(tupleError);
    schemaReport.addError(errorReport);
  }

  private void addFieldReport(String fileName, FieldReport fieldReport) {
    val schemaReport = resolveSchemaReport(fileName);
    addFieldReport(schemaReport, fieldReport);
  }

  private void addFieldReport(SchemaReport schemaReport, FieldReport fieldReport) {
    schemaReport.addFieldReport(fieldReport);
  }

  private SchemaReport resolveSchemaReport(String fileName) {
    SchemaReport schemaReport = submissionReport.getSchemaReport(fileName);
    if (schemaReport == null) {
      schemaReport = new SchemaReport();
      schemaReport.setName(fileName);

      submissionReport.addSchemaReport(schemaReport);
    }

    return schemaReport;
  }

}
