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
package org.icgc.dcc.generator.config;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import lombok.Getter;
import lombok.ToString;

import org.icgc.dcc.generator.model.ExperimentalFile;
import org.icgc.dcc.generator.model.OptionalFile;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@ToString
public class GeneratorConfig {

  @JsonProperty
  private String outputDirectory = "target/";

  @JsonProperty
  private Integer numberOfDonors = 500;

  @JsonProperty
  private Integer numberOfSpecimensPerDonor = 2;

  @JsonProperty
  private Integer numberOfSamplesPerSpecimen = 2;

  @JsonProperty
  private String leadJurisdiction = "au";

  @JsonProperty
  private String tumourType = "01";

  @JsonProperty
  private String institution = "001";

  @JsonProperty
  private String platform = "1";

  @JsonProperty
  private Long seed;

  @JsonProperty
  private List<OptionalFile> optionalFiles = newArrayList();

  @JsonProperty
  private List<ExperimentalFile> experimentalFiles = newArrayList();

}
