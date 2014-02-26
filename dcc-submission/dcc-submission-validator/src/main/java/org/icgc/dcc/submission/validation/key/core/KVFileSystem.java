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

import static com.google.common.base.Optional.of;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.DONOR;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;

import com.google.common.base.Optional;

@RequiredArgsConstructor
@Slf4j
public final class KVFileSystem {

  @Getter
  @NonNull
  private final FileSystem fileSystem;
  @NonNull
  private final Collection<DataType> dataTypes;
  @NonNull
  private final Dictionary dictionary; // TODO: move out of this class (pass where needed instead)
  @NonNull
  private final Path submissionDirPath;
  @NonNull
  private final Path systemDirPath;

  public InputStream open(Path path) throws IOException {
    return fileSystem.open(path);
  }

  public Optional<List<Path>> getDataFilePaths(KVFileType fileType) {
    // Selective validation filtering
    val requested = dataTypes.contains(fileType.getFileType().getDataType());
    if (!requested) {
      return Optional.<List<Path>> absent();
    }

    val filePattern = getFileSchema(fileType).getPattern();
    val basePath = fileType.isSystem() ? systemDirPath : submissionDirPath;

    log.info("Listing '{}' with filter '{}'", basePath, filePattern);
    val filePaths = HadoopUtils.lsFile(fileSystem, basePath, compile(filePattern));
    return filePaths.isEmpty() ? Optional.<List<Path>> absent() : of(filePaths);
  }

  public boolean hasClinicalData() {
    return getDataFilePaths(DONOR).isPresent();
  }

  public boolean hasDataType(KVExperimentalDataType dataType) {
    return getDataFilePaths(dataType.getDataTypePresenceIndicator()).isPresent();
  }

  private FileSchema getFileSchema(KVFileType fileType) {
    val targetName = fileType.getFileType().getTypeName();
    for (val fileSchema : dictionary.getFiles()) {
      if (targetName.equals(fileSchema.getName())) {
        return fileSchema;
      }
    }

    throw new IllegalArgumentException(
        format("No file schema found for file type: '%s' ('%s')", fileType, targetName));
  }

}
