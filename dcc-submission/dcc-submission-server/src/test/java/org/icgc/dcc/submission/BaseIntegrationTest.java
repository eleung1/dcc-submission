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
package org.icgc.dcc.submission;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;

import org.apache.commons.io.FileUtils;

public class BaseIntegrationTest {

  static {
    // See http://stackoverflow.com/questions/7134723/hadoop-on-osx-unable-to-load-realm-info-from-scdynamicstore
    System.setProperty("java.security.krb5.realm", "OX.AC.UK");
    System.setProperty("java.security.krb5.kdc", "kdc0.ox.ac.uk:kdc1.ox.ac.uk");
  }

  protected final Client client = ClientFactory.newClient();

  protected static String _(String format, Object... args) {
    return String.format(format, args);
  }

  protected static void assertEmptyFile(String dir, String path) throws IOException {
    File errorFile = new File(dir, path);
    assertTrue("Expected file does not exist: " + path, errorFile.exists());
    assertTrue("Expected empty file: " + path, FileUtils.readFileToString(errorFile).isEmpty());
  }

}
