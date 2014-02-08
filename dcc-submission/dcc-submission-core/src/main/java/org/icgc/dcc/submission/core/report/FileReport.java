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
package org.icgc.dcc.submission.core.report;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newTreeSet;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.mongodb.morphia.annotations.Embedded;

@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
public class FileReport implements Comparable<FileReport> {

  String fileName;
  FileState fileState = FileState.NOT_VALIDATED;
  List<SummaryReport> summaryReports = newLinkedList();
  List<FieldReport> fieldReports = newLinkedList();
  Set<ErrorReport> errorReports = newTreeSet();

  public FileReport(@NonNull String fileName) {
    this.fileName = fileName;
  }

  public void addSummaryReport(@NonNull SummaryReport summaryReport) {
    summaryReports.add(summaryReport);
  }

  public void addFieldReport(@NonNull FieldReport fieldReport) {
    fieldReports.add(fieldReport);
  }

  public void addErrorReport(@NonNull ErrorReport errorReport) {
    errorReports.add(errorReport);
  }

  public void addError(@NonNull Error error) {
    for (val errorReport : errorReports) {
      if (errorReport.isReported(error)) {
        errorReport.addColumn(error);

        return;
      }
    }

    val errorReport = new ErrorReport(error.getType(), error.getNumber(), error.getMessage());
    errorReport.addColumn(error);

    errorReports.add(errorReport);
  }

  @Override
  public int compareTo(@NonNull FileReport other) {
    return fileName.compareTo(other.fileName);
  }

}
