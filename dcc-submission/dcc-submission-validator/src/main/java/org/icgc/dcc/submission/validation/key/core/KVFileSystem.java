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
package org.icgc.dcc.submission.validation.key.core;

import static org.icgc.dcc.hadoop.fs.HadoopUtils.checkExistence;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.EXISTING_FILE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.INCREMENTAL_FILE;

import java.io.IOException;
import java.io.InputStream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.validation.key.enumeration.KVExperimentalDataType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType;

@RequiredArgsConstructor
@Slf4j
public final class KVFileSystem {

  @NonNull
  private final FileSystem fileSystem;
  private final Path oldReleaseDir;
  @NonNull
  private final Path newReleaseDir;

  public static final String TO_BE_REMOVED_FILE_NAME = "TO_BE_REMOVED";

  public InputStream open(Path path) throws IOException {
    return fileSystem.open(path);
  }

  public Path getDataFilePath(KVSubmissionType submissionType, KVFileType fileType) {
    val basePath = submissionType == EXISTING_FILE ? oldReleaseDir : newReleaseDir;
    Path path = new Path(basePath, fileType.toString().toLowerCase() + ".txt");
    log.info("Searching for '{}'", path);
    return path;
  }

  public Path getToBeRemovedFilePath() {
    return new Path(newReleaseDir, TO_BE_REMOVED_FILE_NAME + ".txt");
  }

  public boolean hasToBeRemovedFile() {
    return hasFile(getToBeRemovedFilePath());
  }

  public boolean hasExistingData() {
    return hasExistingClinicalData();
  }

  public boolean hasExistingClinicalData() {
    return hasFile(getDataFilePath(EXISTING_FILE, DONOR));
  }

  public boolean hasIncrementalClinicalData() {
    return hasFile(getDataFilePath(INCREMENTAL_FILE, DONOR));
  }

  public boolean hasIncrementalData(KVExperimentalDataType dataType) {
    return hasFile(getDataFilePath(INCREMENTAL_FILE, dataType.getTaleTellerFileType()));
  }

  public boolean hasExistingData(KVExperimentalDataType dataType) {
    return hasFile(getDataFilePath(EXISTING_FILE, dataType.getTaleTellerFileType()));
  }

  private boolean hasFile(Path filePath) {
    return checkExistence(fileSystem, filePath);
  }
}
