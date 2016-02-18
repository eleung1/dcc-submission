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
package org.icgc.dcc.submission.validation.primary.core;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableList;

@Getter
public class RestrictionTypeSchema {

  public enum ParameterType {
    NUMBER, TEXT, FIELD_REFERENCE
  }

  private final List<FieldRestrictionParameter> parameters;

  public RestrictionTypeSchema(FieldRestrictionParameter... parameters) {
    this.parameters = ImmutableList.copyOf(parameters);
  }

  @RequiredArgsConstructor
  @Getter
  public static class FieldRestrictionParameter {

    private final String key;
    private final ParameterType type;
    private final String description;
    private final boolean repeated;

    public FieldRestrictionParameter(String key, ParameterType type) {
      this(key, type, null, false);
    }

    public FieldRestrictionParameter(String key, ParameterType type, String description) {
      this(key, type, description, false);
    }

  }

}
