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
package org.icgc.dcc.submission.validation.first;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.step.FileCollisionChecker;
import org.icgc.dcc.submission.validation.first.step.FileCorruptionChecker;
import org.icgc.dcc.submission.validation.first.step.FileHeaderChecker;
import org.icgc.dcc.submission.validation.first.step.NoOpFileChecker;
import org.icgc.dcc.submission.validation.first.step.ReferentialFileChecker;

public interface FileChecker extends Checker {

  void check(String filename);

  SubmissionDirectory getSubmissionDirectory();

  Dictionary getDictionary();

  DccFileSystem getDccFileSystem();

  ValidationContext getValidationContext();

  boolean canContinue();

  /**
   * Made non-final for power mock.
   */
  @NoArgsConstructor(access = PRIVATE)
  class FileCheckers {

    static FileChecker getDefaultFileChecker(ValidationContext validationContext) {

      // Chaining multiple file checker
      return new FileHeaderChecker(
          new FileCorruptionChecker(
              new FileCollisionChecker(
                  new ReferentialFileChecker(
                      // TODO: Enforce Law of Demeter (do we need the whole dictionary for instance)?
                      new NoOpFileChecker(validationContext)))));
    }
  }
}