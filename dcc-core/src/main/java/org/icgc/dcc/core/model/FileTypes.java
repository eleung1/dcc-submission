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
package org.icgc.dcc.core.model;

import static lombok.AccessLevel.PRIVATE;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Utilities for working with ICGC file types.
 * <p>
 * For experimental feature types, see {@link FeatureTypes} instead.
 */
@NoArgsConstructor(access = PRIVATE)
public final class FileTypes {

  /**
   * TODO: migrate all constants below to this enum (DCC-1452).
   */
  public enum FileType implements SubmissionDataType, SubmissionFileType {
    DONOR_TYPE("donor"),
    SPECIMEN_TYPE("specimen"),
    SAMPLE_TYPE("sample"),

    BIOMARKER("biomarker"),
    FAMILY("family"),
    EXPOSURE("exposure"),
    SURGERY("surgery"),
    THERAPY("therapy");

    private FileType(String typeName) {
      this.typeName = typeName;
    }

    @Getter
    private final String typeName;

    public boolean isDonor() {
      return this == DONOR_TYPE;
    }

    /**
     * Returns an enum matching the type like "donor", "specimen", ...
     */
    public static FileType fromTypeName(String typeName) {
      return valueOf(typeName.toUpperCase() + TYPE_SUFFIX);
    }
  }

  public static final String DONOR_TYPE = "donor";
  public static final String SPECIMEN_TYPE = "specimen";
  public static final String SAMPLE_TYPE = "sample";

}
