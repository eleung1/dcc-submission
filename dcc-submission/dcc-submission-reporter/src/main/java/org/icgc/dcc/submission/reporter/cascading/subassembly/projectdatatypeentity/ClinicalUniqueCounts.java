/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.reporter.cascading.subassembly.projectdatatypeentity;

import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.submission.reporter.OutputType.DONOR;
import static org.icgc.dcc.submission.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.submission.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.NamingPipe;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy.UniqueCountByData;
import org.icgc.dcc.hadoop.cascading.operation.BaseFilter;
import org.icgc.dcc.submission.reporter.OutputType;
import org.icgc.dcc.submission.reporter.Reporter;

import cascading.flow.FlowProcess;
import cascading.operation.FilterCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.tuple.Fields;

public class ClinicalUniqueCounts extends SubAssembly {

  public static Pipe allDonors(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {
    return donors(
        filterAllTypes(
            preComputationTable,
            TYPE_FIELD),
        countByFields);
  }

  /**
   * This one is used in the other table as well (hence the public).
   */
  static Pipe donors(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {
    return new ClinicalUniqueCounts(
        preComputationTable,
        DONOR,
        countByFields,
        DONOR_ID_FIELD);
  }

  static Pipe specimens(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {
    return new ClinicalUniqueCounts(
        preComputationTable,
        SPECIMEN,
        countByFields,
        SPECIMEN_ID_FIELD);
  }

  static Pipe samples(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {
    return new ClinicalUniqueCounts(
        preComputationTable,
        SAMPLE,
        countByFields,
        SAMPLE_ID_FIELD);
  }

  private ClinicalUniqueCounts(
      @NonNull final Pipe preComputationTable,
      @NonNull final OutputType outputType,
      @NonNull final Fields countByFields,
      @NonNull final Fields clinicalIdField) {
    setTails(process(preComputationTable, outputType, countByFields, clinicalIdField));
  }

  private static Pipe process(
      @NonNull final Pipe preComputationTable,
      @NonNull final OutputType outputType,
      @NonNull final Fields countByFields,
      @NonNull final Fields clinicalIdCountField) {
    checkFieldsCardinalityOne(clinicalIdCountField);

    return new NamingPipe(
        outputType,

        new UniqueCountBy(UniqueCountByData.builder()

            .pipe(preComputationTable)
            .uniqueFields(countByFields.append(clinicalIdCountField))
            .countByFields(countByFields)
            .resultCountField(getCountFieldCounterpart(clinicalIdCountField))

            .build()));
  }

  private static Each filterAllTypes(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields typeField) {
    return new Each(
        preComputationTable,
        checkFieldsCardinalityOne(typeField),
        new BaseFilter<Void>() {

          @Override
          public boolean isRemove(
              @SuppressWarnings("rawtypes") FlowProcess flowProcess,
              FilterCall<Void> filterCall) {
            val type = filterCall.getArguments().getString(TYPE_FIELD);

            return Reporter.isAllTypes(type);
          }

        });
  }

}
