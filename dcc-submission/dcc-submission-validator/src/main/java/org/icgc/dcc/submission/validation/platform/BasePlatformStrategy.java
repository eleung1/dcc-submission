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
package org.icgc.dcc.submission.validation.platform;

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_ANALYZED_SAMPLE_ID;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_DONOR_ID;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_SPECIMEN_ID;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.readSmallTextFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.FileSchemaRole;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.validation.primary.core.FileSchemaDirectory;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.Key;

import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Slf4j
public abstract class BasePlatformStrategy implements PlatformStrategy {

  private static final Splitter ROW_SPLITTER = Splitter.on('\t');

  protected final FileSystem fileSystem;

  private final Path input;

  private final Path output;

  private final Path system;

  private final FileSchemaDirectory fileSchemaDirectory;

  private final FileSchemaDirectory systemDirectory;

  protected BasePlatformStrategy(FileSystem fileSystem, Path input, Path output, Path system) {
    this.fileSystem = fileSystem;
    this.input = input;
    this.output = output;
    this.system = system;
    this.fileSchemaDirectory = new FileSchemaDirectory(fileSystem, input);
    this.systemDirectory = new FileSchemaDirectory(fileSystem, system);
  }

  /**
   * TODO: phase out in favour of {@link #getSourceTap(FileType)}.
   */
  @Override
  @Deprecated
  public Tap<?, ?, ?> getSourceTap(FileSchema schema) {
    try {
      Path path = path(schema);
      Path resolvedPath = FileContext.getFileContext(fileSystem.getUri()).resolvePath(path);
      return tapSource(resolvedPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * TODO: phase out in favour of {@link #getSourceTap(FileType)}; Temporary: see DCC-1876
   */
  @Override
  public Tap<?, ?, ?> getSourceTap2(FileSchema schema) {
    try {
      Path path = path(schema);
      Path resolvedPath = FileContext.getFileContext(fileSystem.getUri()).resolvePath(path);
      return tapSource2(resolvedPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Tap<?, ?, ?> getFlowSinkTap(FileSchema schema, FlowType flowType) {
    return tap(new Path(output, String.format("%s.%s.tsv", schema.getName(), flowType)));
  }

  @Override
  public Tap<?, ?, ?> getTrimmedTap(Key key) {
    return tap(trimmedPath(key), new Fields(key.getFields()));
  }

  protected Path trimmedPath(Key key) {
    if (key.getSchema().getRole() == FileSchemaRole.SUBMISSION) {
      return new Path(output, key.getName() + ".tsv");
    } else if (key.getSchema().getRole() == FileSchemaRole.SYSTEM) {
      return new Path(new Path(system, DccFileSystem.VALIDATION_DIRNAME), key.getName() + ".tsv"); // TODO: should use
                                                                                                   // DccFileSystem
                                                                                                   // abstraction
    } else {
      throw new RuntimeException("Undefined File Schema Role " + key.getSchema().getRole());
    }
  }

  protected Path reportPath(FileSchema schema, FlowType type, String reportName) {
    return new Path(output, String.format("%s.%s%s%s.json", schema.getName(), type, FILE_NAME_SEPARATOR, reportName));
  }

  /**
   * Returns a tap for the given path.
   */
  protected abstract Tap<?, ?, ?> tap(Path path);

  protected abstract Tap<?, ?, ?> tap(Path path, Fields fields);

  /**
   * See {@link #getSourceTap(FileSchema)} comment
   */
  @Deprecated
  protected abstract Tap<?, ?, ?> tapSource(Path path);

  /**
   * See {@link #getSourceTap(FileSchema)} comment
   */
  protected abstract Tap<?, ?, ?> tapSource2(Path path);

  /**
   * FIXME: This should not be happening in here, instead it should delegate to the filesystem abstraction (see
   * DCC-1876).
   */
  @Override
  @SneakyThrows
  public Path path(final FileSchema fileSchema) {

    RemoteIterator<LocatedFileStatus> files;
    if (fileSchema.getRole() == FileSchemaRole.SUBMISSION) {
      files = fileSystem.listFiles(input, false);
    } else if (fileSchema.getRole() == FileSchemaRole.SYSTEM) {
      files = fileSystem.listFiles(system, false);
    } else {
      throw new RuntimeException("undefined File Schema Role " + fileSchema.getRole());
    }

    while (files.hasNext()) {
      LocatedFileStatus file = files.next();
      if (file.isFile()) {
        Path path = file.getPath();
        if (Pattern.matches(fileSchema.getPattern(), path.getName())) {
          return path;
        }
      }
    }
    throw new FileNotFoundException("no file for schema " + fileSchema.getName());
  }

  @Override
  public FileSchemaDirectory getFileSchemaDirectory() {
    return this.fileSchemaDirectory;
  }

  @Override
  public FileSchemaDirectory getSystemDirectory() {
    return this.systemDirectory;
  }

  protected List<String> checkDuplicateHeader(Iterable<String> header) {
    Set<String> headerSet = Sets.newHashSet();
    List<String> dupHeaders = Lists.newArrayList();

    for (String strHeader : header) {
      if (!headerSet.add(strHeader)) {
        dupHeaders.add(strHeader);
      }
    }
    return dupHeaders;
  }

  @Override
  public Map<String, String> getSampleToDonorMap(Dictionary dictionary) {

    val sampleToSpecimen = Maps.<String, String> newTreeMap();
    val sampleFileSchema = dictionary.getFileSchema(SAMPLE_TYPE);
    val samplePath = path(sampleFileSchema);
    val sampleSampleIdOrdinal = sampleFileSchema.getFieldOrdinal(SUBMISSION_ANALYZED_SAMPLE_ID).get();
    val sampleSpecimenIdOrdinal = sampleFileSchema.getFieldOrdinal(SUBMISSION_SPECIMEN_ID).get();
    boolean first = true;
    for (String row : readSmallTextFile(fileSystem, samplePath)) { // Clinical files are small
      if (!first) {
        val fields = Lists.<String> newArrayList(ROW_SPLITTER.split(row));
        val sampleId = fields.get(sampleSampleIdOrdinal);
        val specimenId = fields.get(sampleSpecimenIdOrdinal);
        checkState(!sampleToSpecimen.containsKey(sampleId));
        sampleToSpecimen.put(sampleId, specimenId);
      }
      first = false;
    }
    log.info("Sample to specimen mapping: {}", sampleToSpecimen);

    val specimenToDonor = Maps.<String, String> newTreeMap();
    val specimenFileSchema = dictionary.getFileSchema(SPECIMEN_TYPE);
    val specimenPath = path(specimenFileSchema);
    val specimenSpecimenIdOrdinal = specimenFileSchema.getFieldOrdinal(SUBMISSION_SPECIMEN_ID).get();
    val specimenDonorIdOrdinal = specimenFileSchema.getFieldOrdinal(SUBMISSION_DONOR_ID).get();
    first = true;
    for (String row : readSmallTextFile(fileSystem, specimenPath)) { // Clinical files are small
      if (!first) {
        val fields = Lists.<String> newArrayList(ROW_SPLITTER.split(row));
        val specimenId = fields.get(specimenSpecimenIdOrdinal);
        val donorId = fields.get(specimenDonorIdOrdinal);
        checkState(!specimenToDonor.containsKey(specimenId));
        specimenToDonor.put(specimenId, donorId);
      }
      first = false;
    }
    log.info("Specimen to donor mapping: {}", specimenToDonor);

    val sampleToDonor = Maps.<String, String> newTreeMap();
    for (val entry : sampleToSpecimen.entrySet()) {
      sampleToDonor.put(
          entry.getKey(),
          specimenToDonor.get(entry.getValue()));
    }
    log.info("Sample to donor mapping: {}", sampleToDonor);

    return sampleToDonor;
  }

}
