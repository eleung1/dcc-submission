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
package org.icgc.dcc.submission.normalization.steps;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Iterator;

import org.icgc.dcc.submission.normalization.steps.PreMarking.PreMarker;
import org.icgc.dcc.submission.validation.cascading.CascadingTestUtils;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.operation.Function;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class PreMarkingTest extends CascadingTestCase {

  @Test
  public void test_cascading_PreMaskingMarker() {
    Function<?> function = new PreMarker();

    Fields inputFields = new Fields("f1", "f2");
    String dummyValue = "dummy";
    TupleEntry[] entries = new TupleEntry[] {
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue)),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue)),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue))
    };
    Fields resultFields = Masking.NORMALIZER_MASKING_FIELD;

    Iterator<TupleEntry> iterator = CascadingTestUtils.invokeFunction(function, entries, resultFields);
    for (int i = 0; i < 3; i++) {
      assertThat(iterator.hasNext());
      TupleEntry entry = iterator.next();
      Object object = entry.getObject(resultFields);
      assertThat(object)
          .isEqualTo(Masking.OPEN.getTupleValue());
    }
    assertFalse(iterator.hasNext());
  }

}
