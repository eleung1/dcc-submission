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
package org.icgc.dcc.submission.validation.norm.steps;

import static com.google.common.collect.ImmutableList.copyOf;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.norm.NormalizationContext;
import org.icgc.dcc.submission.validation.norm.NormalizationStep;
import org.icgc.dcc.submission.validation.norm.NormalizationConfig.OptionalStep;

import cascading.pipe.Pipe;
import cascading.pipe.assembly.Discard;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * May never be used.
 */
public final class ConfidentialFieldsRemoval implements NormalizationStep, OptionalStep {

  public static final String STEP_NAME = "confidential-fields";

  /**
   * TODO
   */
  public static ImmutableMap<String, ImmutableList<String>> getControlledFields(Dictionary dictionary) {
    val controlledFields = new ImmutableMap.Builder<String, ImmutableList<String>>();
    for (val fileSchema : dictionary.getFiles()) {
      controlledFields.put(
          fileSchema.getName(),
          copyOf(fileSchema.getControlledFieldNames()));
    }
    return controlledFields.build();
  }

  @Override
  public String shortName() {
    return STEP_NAME;
  }

  /**
   * TODO
   */
  @Override
  public Pipe extend(Pipe pipe, NormalizationContext context) {
    return new Discard(pipe, new Fields("TODO"));
  }

}
