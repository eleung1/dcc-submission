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

import static org.icgc.dcc.hadoop.fs.FileSystems.getLocalFileSystem;

import java.io.InputStream;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.hadoop.cascading.connector.CascadingConnectors;
import org.icgc.dcc.hadoop.cascading.taps.CascadingTaps;
import org.icgc.dcc.submission.validation.cascading.LocalJsonScheme;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;
import org.icgc.dcc.submission.validation.primary.core.FlowType;

import cascading.scheme.local.TextDelimited;
import cascading.tap.Tap;
import cascading.tap.local.FileTap;
import cascading.tuple.Fields;

@Slf4j
public class LocalSubmissionPlatformStrategy extends BaseSubmissionPlatformStrategy {

  public LocalSubmissionPlatformStrategy(
      @NonNull final Map<String, String> hadoopProperties,
      @NonNull final Path source,
      @NonNull final Path output,
      @NonNull final Path system) {
    super(hadoopProperties, getLocalFileSystem(), source, output, system);
  }

  @Override
  protected CascadingTaps getTaps() {
    return CascadingTaps.LOCAL;
  }

  @Override
  protected CascadingConnectors getConnectors() {
    return CascadingConnectors.LOCAL;
  }

  @Override
  protected Map<?, ?> augmentFlowProperties(@NonNull final Map<?, ?> properties) {
    return properties; // Nothing to add in local mode
  }

  @Override
  public Tap<?, ?, ?> getReportTap(String fileName, FlowType type, String reportName) {
    return new FileTap(new LocalJsonScheme(), getReportPath(fileName, type, reportName).toUri().getPath());
  }

  @Override
  @SneakyThrows
  public InputStream readReportTap(String fileName, FlowType type, String reportName) {
    val reportPath = getReportPath(fileName, type, reportName);
    log.info("Streaming through report: '{}'", reportPath);
    return fileSystem.open(reportPath);
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path) {
    return new FileTap(new TextDelimited(true, FIELD_SEPARATOR), path.toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path, Fields fields) {
    return new FileTap(new TextDelimited(fields, true, FIELD_SEPARATOR), path.toUri().getPath());
  }

  @Override
  public Tap<?, ?, ?> getSourceTap(String fileName) {
    return CascadingTaps.LOCAL.getDecompressingLinesNoHeader(
        getFilePath(fileName),
        new Fields(ValidationFields.OFFSET_FIELD_NAME),
        new Fields("line"));
  }

}