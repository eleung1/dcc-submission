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
package org.icgc.dcc.submission.core.util;

import java.util.Iterator;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.FileSchema;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

/**
 * Simple {@link FileSchema} file instance line parser.
 */
@RequiredArgsConstructor
@ToString
public class FileLineParser {

  /**
   * Separator between fields.
   */
  private static final String FIELD_SEPARATOR = "\t";

  /**
   * Splits fields in to a {@code String} iterable.
   */
  private static final Splitter FIELD_SPLITTER = Splitter.on(FIELD_SEPARATOR);

  @NonNull
  private final FileSchema schema;

  public Map<String, String> parser(String line) {
    val record = ImmutableMap.<String, String> builder();

    val values = split(line);
    for (val fieldName : schema.getFieldNames()) {
      val fieldValue = values.next();
      record.put(fieldName, fieldValue);
    }

    return record.build();
  }

  private Iterator<String> split(String line) {
    return FIELD_SPLITTER.split(line).iterator();
  }

}