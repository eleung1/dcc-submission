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
package org.icgc.dcc.submission.normalization;

import lombok.Value;
import lombok.experimental.Builder;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.normalization.steps.ConfidentialFieldsRemoval;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Common context object passed to all {@link NormalizationStep}s.
 */
public interface NormalizationContext {

  /**
   * Returns the list of fields that are marked as "controlled" (region of residence for instance).
   */
  ImmutableMap<String, ImmutableList<String>> getControlledFields();

  @Value
  @Builder
  static final class DefaultNormalizationContext implements NormalizationContext {

    /**
     * See {@link NormalizationContext#getControlledFields()}.
     */
    private final ImmutableMap<String, ImmutableList<String>> controlledFields;

    /**
     * Creates the default {@link NormalizationContext}.
     */
    static NormalizationContext getNormalizationContext(Dictionary dictionary) {
      return DefaultNormalizationContext
          .builder()
          .controlledFields(ConfidentialFieldsRemoval.getControlledFields(dictionary))
          .build();
    }
  }
}
