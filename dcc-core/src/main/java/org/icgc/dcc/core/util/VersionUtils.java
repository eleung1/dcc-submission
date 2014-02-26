/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.core.util;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Maps.fromProperties;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map;
import java.util.Properties;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class VersionUtils {

  private static final String LOCAL_VERSION = "?";

  private static final Map<String, String> SCM_INFO = loadScmInfo();

  public static Map<String, String> getScmInfo() {
    return SCM_INFO;
  }

  private static final String VERSION = firstNonNull(
      VersionUtils.class.getPackage().getImplementationVersion(),
      LOCAL_VERSION);

  public static String getVersion() {
    return VERSION;
  }

  public static String getApiVersion() {
    return "v" + VERSION.split("\\.")[0];
  }

  public static String getCommitId() {
    return firstNonNull(SCM_INFO.get("git.commit.id.abbrev"), "unknown");
  }

  public static String getCommitMessageShort() {
    return firstNonNull(SCM_INFO.get("git.commit.message.short"), "unknown");
  }

  private static Map<String, String> loadScmInfo() {
    Properties properties = new Properties();
    try {
      properties.load(VersionUtils.class.getClassLoader().getResourceAsStream("git.properties"));
    } catch (Exception e) {
      // Local build
    }

    return fromProperties(properties);
  }

}
