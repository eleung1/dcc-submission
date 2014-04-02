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
package org.icgc.dcc.submission.dictionary.model;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.submission.core.util.Constants.CodeListRestriction_FIELD;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;

import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.model.BaseEntity;
import org.icgc.dcc.submission.core.model.HasName;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryElement;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryVisitor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PrePersist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Describes a dictionary that contains {@code FileSchema}ta and that may be used by some releases
 */
@Entity
@ToString(of = { "version", "state" })
public class Dictionary extends BaseEntity implements HasName, DictionaryElement {

  @NotBlank
  @Indexed(unique = true)
  private String version;

  private DictionaryState state;

  @Valid
  private List<FileSchema> files;

  public Dictionary() {
    super();
    this.state = DictionaryState.OPENED;
    this.files = new ArrayList<FileSchema>();
  }

  public Dictionary(@NonNull String version) {
    this();
    this.version = version;
  }

  @Override
  public void accept(@NonNull DictionaryVisitor dictionaryVisitor) {
    dictionaryVisitor.visit(this);
    for (FileSchema fileSchema : files) {
      fileSchema.accept(dictionaryVisitor);
    }
  }

  @PrePersist
  public void updateTimestamp() {
    lastUpdate = new Date();
  }

  public void close() {
    this.state = DictionaryState.CLOSED;
  }

  @Override
  @JsonIgnore
  public String getName() {
    return this.version;
  }

  public String getVersion() {
    return version;
  }

  public DictionaryState getState() {
    return state;
  }

  public List<FileSchema> getFiles() {
    return files;
  }

  public void setVersion(@NonNull String version) {
    this.version = version;
  }

  public void setState(@NonNull DictionaryState state) {
    this.state = state;
  }

  public void setFiles(@NonNull List<FileSchema> files) {
    this.files = files;
  }

  /**
   * Returns a {@link FileSchema} matching {@link FileType} provided.
   */
  @JsonIgnore
  public FileSchema getFileSchema(@NonNull FileType type) {
    val optional = getFileSchemaByName(type.getTypeName());
    checkState(optional.isPresent(), "Coun't find type '%s' in dictionary", type);
    return optional.get();
  }

  /**
   * Optionally returns a {@link FileSchema} matching the file schema name provided.
   * <p>
   * TODO: phase out in favour of {@link #getFileSchema(FileType)}.
   */
  @JsonIgnore
  public Optional<FileSchema> getFileSchemaByName(@NonNull final String fileSchemaName) {
    return tryFind(this.files, new Predicate<FileSchema>() {

      @Override
      public boolean apply(FileSchema input) {
        return input.getName().equals(fileSchemaName);
      }

    });
  }

  /**
   * Optionally returns a {@link FileSchema} for which the file name provided would be matching the pattern.
   */
  @JsonIgnore
  public Optional<FileSchema> getFileSchemaByFileName(@NonNull String fileName) {
    val optional = Optional.<FileSchema> absent();
    for (val fileSchema : files) {
      if (fileSchema.matches(fileName)) {
        return Optional.of(fileSchema);
      }
    }

    return optional;
  }

  /**
   * Optionally returns a {@link FileType} for which the file name provided would match the pattern.
   */
  @JsonIgnore
  public Optional<FileType> getFileType(@NonNull String fileName) {
    val fileSchema = getFileSchemaByFileName(fileName);
    return fileSchema.isPresent() ?
        Optional.of(FileType.from(
            fileSchema.get().getName())) :
        Optional.<FileType> absent();
  }

  /**
   * Returns the list of {@code FileSchema} names
   */
  @JsonIgnore
  public List<String> getFileSchemaNames() {
    return ImmutableList.<String> copyOf(transform(files, new Function<FileSchema, String>() {

      @Override
      public String apply(FileSchema input) {
        return input.getName();
      }

    }));
  }

  /**
   * Returns the list of {@code FileSchema} file patterns.
   */
  @JsonIgnore
  public List<String> getFilePatterns() {
    return ImmutableList.<String> copyOf(transform(files, new Function<FileSchema, String>() {

      @Override
      public String apply(FileSchema input) {
        return input.getPattern();
      }

    }));
  }

  /**
   * Returns a non-null String matching the file pattern for the given {@link FileType}.
   */
  @JsonIgnore
  public String getFilePattern(@NonNull FileType type) {
    for (val fileSchema : files) {
      val match = type.getTypeName().equals(fileSchema.getName());
      if (match) {
        return fileSchema.getPattern();
      }
    }

    throw new IllegalStateException("No file schema found for type '" + type + "'");
  }

  /**
   * Returns a list of {@link FileSchema}s for a given {@link FeatureType}.
   */
  @JsonIgnore
  public List<FileSchema> getFileSchemata(@NonNull final FeatureType featureType) {
    val filter = filter(files, new Predicate<FileSchema>() {

      @Override
      public boolean apply(FileSchema input) {
        FileType type = FileType.from(input.getName());
        return type.getDataType() == featureType;
      }
    });

    return ImmutableList.<FileSchema> copyOf(filter);
  }

  /**
   * Returns an iterable of {@link FileSchema}s for a given {@link DataType}.
   */
  @JsonIgnore
  public Iterable<FileSchema> getFileSchemata(@NonNull final Iterable<? extends DataType> dataTypes) {
    val set = ImmutableSet.<FileSchema> builder();
    for (val fileSchema : files) {
      val match = contains(dataTypes, fileSchema.getDataType());
      if (match) {
        set.add(fileSchema);
      }
    }

    return set.build();
  }

  public boolean hasFileSchema(@NonNull String fileName) {
    for (val fileSchema : files) {
      if (fileSchema.getName().equals(fileName)) {
        return true;
      }
    }

    return false;
  }

  public void addFile(@NonNull FileSchema file) {
    files.add(file);
  }

  @JsonIgnore
  public Set<String> getCodeListNames() {
    // TODO: Add corresponding unit test(s) - see DCC-905
    val set = ImmutableSet.<String> builder();
    for (val fileSchema : getFiles()) {
      for (val field : fileSchema.getFields()) {
        for (val restriction : field.getRestrictions()) {
          if (restriction.getType() == RestrictionType.CODELIST) {
            val config = restriction.getConfig();
            String codeListName = config.getString(CodeListRestriction_FIELD);

            set.add(codeListName);
          }
        }
      }
    }
    return set.build();
  }

  public boolean usesCodeList(@NonNull final String codeListName) {
    return getCodeListNames().contains(codeListName);
  }

  /**
   * Returns a mapping of {@link FileType} to file pattern.
   */
  @JsonIgnore
  public Map<FileType, String> getPatterns() {
    val map = new ImmutableMap.Builder<FileType, String>();
    for (val fileSchema : files) {
      map.put(FileType.from(fileSchema.getName()), fileSchema.getPattern());
    }

    return map.build();
  }

  @JsonIgnore
  public List<FileType> getFileTypes() {
    return newArrayList(transform(files, new Function<FileSchema, FileType>() {

      @Override
      public FileType apply(FileSchema input) {
        return input.getFileType();
      }

    }));
  }

  /**
   * Returns the list of unique {@link DataType}s present in the dictionary (ordered by declaration).
   */
  @JsonIgnore
  public List<DataType> getDataTypes() {
    return newArrayList(newLinkedHashSet(transform(
        getFileTypes(),
        new Function<FileType, DataType>() {

          @Override
          public DataType apply(FileType fileType) {
            return fileType.getDataType();
          }

        })));
  }

  /**
   * Returns the list of feature types defined in the dictionary.
   */
  @JsonIgnore
  public List<FeatureType> getFeatureTypes() {
    return newArrayList(transform(

        // Only get file types indicators of a feature type
        filter(
            getFileTypes(),
            new Predicate<FileType>() {

              @Override
              public boolean apply(FileType fileType) {
                val dataType = fileType.getDataType();
                return !dataType.isFeatureType()
                    || isPresenceIndicatorOf(fileType, dataType.asFeatureType());
              }

              private boolean isPresenceIndicatorOf(FileType fileType, FeatureType featureType) {
                return featureType.getDataTypePresenceIndicator() == fileType;
              }

            }),

        // Transform them into their corresponding feature type
        new Function<FileType, FeatureType>() {

          @Override
          public FeatureType apply(FileType fileType) {
            val dataType = fileType.getDataType();
            checkState(dataType.isFeatureType(),
                "Expecting a '%s' at this point, instead got: '%s'",
                FeatureType.class.getSimpleName(), dataType);
            return dataType.asFeatureType();
          }

        }));
  }

  /**
   * Returns the list of {@link FileType} that constitutes a branch for the given {@link FeatureType} in the relation
   * tree (ordered by referenced types).
   * <p>
   * For instance for {@link FeatureType#SSM_TYPE}, one would obtain: [{@link FileType#SSM_M_TYPE},
   * {@link FileType#SSM_P_TYPE}], because {@link FileType#SSM_P_TYPE} references {@link FileType#SSM_M_TYPE}.
   */
  @JsonIgnore
  public List<FileType> getFileTypesReferencedBranch(FeatureType featureType) {
    checkState(false, "WIP");
    return null; // FIXME: not ready for prime time
  }

}
