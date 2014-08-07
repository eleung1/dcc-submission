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
package org.icgc.dcc.hadoop.fs;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;

import java.net.URI;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.core.util.Protocol;

/**
 * Util methods for {@link FileSystem}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileSystems {

  @SneakyThrows
  public static FileSystem getLocalFileSystem() {
    return FileSystem.get(new URI(Protocol.FILE + "/"), new Configuration());
  }

  @SneakyThrows
  public static FileSystem getFileSystem(@NonNull final String fsUrl) {
    return getFileSystem(Configurations.addFsDefault(new Configuration(), fsUrl));
  }

  @SneakyThrows
  public static FileSystem getFileSystem(@NonNull final Configuration config) {
    return FileSystem.get(config);
  }

  public static boolean isLocal(@NonNull final Map<String, String> hadoopProperties) {
    checkState(hadoopProperties.containsKey(FS_DEFAULT_NAME_KEY));
    return Protocol.fromURI(hadoopProperties.get(FS_DEFAULT_NAME_KEY)).isFile();
  }

}
