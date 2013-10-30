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
package org.icgc.dcc.submission.validation.checker;

import java.util.List;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ErrorCode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class RowColumnChecker extends CompositeRowChecker {

  final private String DELIMITER = "\t";

  public RowColumnChecker(RowChecker rowChecker, boolean failFast) {
    super(rowChecker, failFast);
  }

  public RowColumnChecker(RowChecker rowChecker) {
    this(rowChecker, false);
  }

  @Override
  public List<FirstPassValidationError> performSelfCheck(FileSchema fileSchema, String line, long lineNumber) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    int expectedNumColumns = fileSchema.getFields().size();
    int actualNumColumns = ImmutableList.copyOf(line.split(DELIMITER, -1)).size();
    if (actualNumColumns != expectedNumColumns) errors.add(new FirstPassValidationError(getCheckLevel(),
        "Row does not match the expected number of columns: " + expectedNumColumns + ", actual: " + actualNumColumns,
        ErrorCode.STRUCTURALLY_INVALID_ROW_ERROR, new Object[] { expectedNumColumns }, lineNumber));
    return errors.build();
  }
}
