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
package org.icgc.dcc.submission.core.report.visitor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.DataTypeState;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileState;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.FileTypeState;
import org.icgc.dcc.submission.release.model.SubmissionState;

import com.google.common.collect.Iterables;

/**
 * Propagates state changes internally based on outside influence.
 */
@RequiredArgsConstructor
public class SetStateVisitor extends NoOpVisitor {

  @NonNull
  private final SubmissionState nextState;
  @NonNull
  private final Iterable<DataType> dataTypes;

  @Override
  public void visit(DataTypeReport dataTypeReport) {
    // Not affected by the state change
    if (!isTarget(dataTypeReport)) {
      return;
    }

    // Inherit state change
    if (nextState == SubmissionState.QUEUED) {
      dataTypeReport.setDataTypeState(DataTypeState.QUEUED);
    } else if (nextState == SubmissionState.VALIDATING) {
      dataTypeReport.setDataTypeState(DataTypeState.VALIDATING);
    } else if (nextState == SubmissionState.ERROR) {
      dataTypeReport.setDataTypeState(DataTypeState.ERROR);
    } else if (nextState == SubmissionState.SIGNED_OFF) {
      dataTypeReport.setDataTypeState(DataTypeState.SIGNED_OFF);
    } else {
      // No change
    }
  }

  @Override
  public void visit(FileTypeReport fileTypeReport) {
    if (!isTarget(fileTypeReport)) {
      // Not affected by the state change
      return;
    }

    // Inherit state change
    if (nextState == SubmissionState.QUEUED) {
      fileTypeReport.setFileTypeState(FileTypeState.QUEUED);
    } else if (nextState == SubmissionState.VALIDATING) {
      fileTypeReport.setFileTypeState(FileTypeState.VALIDATING);
    } else if (nextState == SubmissionState.ERROR) {
      fileTypeReport.setFileTypeState(FileTypeState.ERROR);
    } else if (nextState == SubmissionState.SIGNED_OFF) {
      fileTypeReport.setFileTypeState(FileTypeState.SIGNED_OFF);
    } else {
      // No change
    }
  }

  @Override
  public void visit(FileReport fileReport) {
    // Not affected by the state change
    if (!isTarget(fileReport)) {
      return;
    }

    // Inherit state change
    if (nextState == SubmissionState.QUEUED) {
      fileReport.setFileState(FileState.QUEUED);
    } else if (nextState == SubmissionState.VALIDATING) {
      fileReport.setFileState(FileState.VALIDATING);
    } else if (nextState == SubmissionState.ERROR) {
      fileReport.setFileState(FileState.ERROR);
    } else if (nextState == SubmissionState.SIGNED_OFF) {
      fileReport.setFileState(FileState.SIGNED_OFF);
    } else {
      // No change
    }
  }

  //
  // Helpers
  //

  private boolean isTarget(DataTypeReport dataTypeReport) {
    return isTarget(dataTypeReport.getDataType());
  }

  private boolean isTarget(@NonNull FileTypeReport fileTypeReport) {
    return isTarget(fileTypeReport.getFileType().getDataType());
  }

  private boolean isTarget(@NonNull FileReport fileReport) {
    return isTarget(fileReport.getFileType().getDataType());
  }

  private boolean isTarget(@NonNull DataType dataType) {
    return Iterables.contains(dataTypes, dataType);
  }

}
