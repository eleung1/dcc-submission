/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.server.dictionary;

import static org.icgc.dcc.submission.server.test.Tests.codeLists;
import static org.icgc.dcc.submission.server.test.Tests.dictionary;

import org.icgc.dcc.submission.server.dictionary.DictionaryValidator;
import org.junit.Before;
import org.junit.Test;

import lombok.SneakyThrows;
import lombok.val;

public class DictionaryValidatorFunctionalTest {

  DictionaryValidator validator;

  @Before
  @SneakyThrows
  public void setUp() {
    this.validator = new DictionaryValidator(dictionary(), codeLists());
  }

  @Test
  public void testDictionary() {
    val violations = validator.validate();

    System.out.println("**** WARNINGS ****");
    for (val warning : violations.getWarnings()) {
      System.out.println(warning);
    }

    System.out.println("**** ERRORS ****");
    for (val error : violations.getErrors()) {
      System.out.println(error);
    }
  }

}
