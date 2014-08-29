package org.icgc.dcc.submission.reporter;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.Files.createTempDir;
import static org.icgc.dcc.core.model.Dictionaries.getMapping;
import static org.icgc.dcc.core.model.Dictionaries.getPatterns;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.core.util.Extensions.TSV;
import static org.icgc.dcc.core.util.Jackson.getRootObject;
import static org.icgc.dcc.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldName;
import static org.icgc.dcc.hadoop.fs.FileSystems.getFileSystem;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.model.Identifiable;
import org.icgc.dcc.core.model.Identifiable.Identifiables;
import org.icgc.dcc.core.util.Jackson;
import org.icgc.dcc.hadoop.cascading.Pipes;
import org.icgc.dcc.hadoop.dcc.SubmissionInputData;
import org.icgc.dcc.hadoop.fs.FileSystems;
import org.icgc.dcc.submission.reporter.cascading.ReporterConnector;
import org.icgc.dcc.submission.reporter.cascading.subassembly.PreComputation;
import org.icgc.dcc.submission.reporter.cascading.subassembly.ProjectSequencingStrategy;
import org.icgc.dcc.submission.reporter.cascading.subassembly.projectdatatypeentity.ProjectDataTypeEntity;

import cascading.pipe.Pipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

@Slf4j
public class Reporter {

  public static final Class<Reporter> CLASS = Reporter.class;

  public static final String ORPHAN_TYPE = "orphan";

  /**
   * Also encompasses any orphan clinical data there may be.
   */
  public static final String ALL_TYPES = "all";

  public static boolean isAllTypes(@NonNull final String type) {
    return ALL_TYPES.equalsIgnoreCase(type);
  }

  public static String report(
      @NonNull final String releaseName,
      @NonNull final Optional<Set<String>> projectKeys,
      @NonNull final String defaultParentDataDir,
      @NonNull final String projectsJsonFilePath,
      @NonNull final URL dictionaryFilePath,
      @NonNull final URL codeListsFilePath,
      @NonNull final Map<?, ?> hadoopProperties) {

    val dictionaryRoot = getRootObject(dictionaryFilePath);
    val codeListsRoot = Jackson.getRootArray(codeListsFilePath);

    val reporterInput = ReporterInput.from(
        SubmissionInputData.getMatchingFiles(
            getFileSystem(hadoopProperties),
            defaultParentDataDir,
            projectsJsonFilePath,
            getPatterns(dictionaryRoot)));

    return process(
        releaseName,
        projectKeys.isPresent() ?
            projectKeys.get() :
            reporterInput.getProjectKeys(),
        reporterInput,
        getSequencingStrategyMapping(
            dictionaryRoot,
            codeListsRoot),
        hadoopProperties);
  }

  public static String process(
      @NonNull final String releaseName,
      @NonNull final Set<String> projectKeys,
      @NonNull final ReporterInput reporterInput,
      @NonNull final Map<String, String> mapping,
      @NonNull final Map<?, ?> hadoopProperties) {
    log.info("Gathering reports for '{}.{}': '{}' ('{}')",
        new Object[] { releaseName, projectKeys, reporterInput, mapping });

    // Main processing
    val projectDataTypeEntities = Maps.<String, Pipe> newLinkedHashMap();
    val projectSequencingStrategies = Maps.<String, Pipe> newLinkedHashMap();
    for (val projectKey : projectKeys) {
      val preComputationTable = new PreComputation(
          releaseName, projectKey, reporterInput.getMatchingFilePathCounts(projectKey));
      val projectDataTypeEntity = new ProjectDataTypeEntity(releaseName, projectKey, preComputationTable);
      val projectSequencingStrategy = new ProjectSequencingStrategy(
          releaseName, projectKey, preComputationTable, mapping.keySet());

      projectDataTypeEntities.put(projectKey, projectDataTypeEntity);
      projectSequencingStrategies.put(projectKey, projectSequencingStrategy);
    }

    val tempDirPath = createTempDir().getAbsolutePath();
    val connectCascade = new ReporterConnector(
        FileSystems.isLocal(hadoopProperties),
        tempDirPath)
        .connectCascade(
            reporterInput,
            releaseName,
            projectDataTypeEntities,
            projectSequencingStrategies,
            hadoopProperties);

    log.info("Running cascade");
    connectCascade.complete();

    log.info("Output dir: '{}'", tempDirPath);
    return tempDirPath;
  }

  public static String getHeadPipeName(String projectKey, FileType fileType, int fileNumber) {
    return Pipes.getName(
        Identifiables.fromString(projectKey),
        fileType,
        Identifiables.fromInteger(fileNumber));
  }

  public static String getOutputFilePath(
      String outputDirPath, Identifiable type, String releaseName, String projectKey) {
    return PATH.join(outputDirPath, getOutputFileName(type, releaseName, projectKey));
  }

  public static String getOutputFileName(Identifiable type, String releaseName, String projectKey) {
    return EXTENSION.join(
        type.getId(),
        releaseName,
        projectKey,
        TSV);
  }

  private static Map<String, String> getSequencingStrategyMapping(
      @NonNull final JsonNode dictionaryRoot,
      @NonNull final JsonNode codeListsRoot) {
    val sequencingStrategyMapping = getMapping(
        dictionaryRoot,
        codeListsRoot,
        SSM_M_TYPE, // TODO: add check mapping is the same for all meta files (it should)
        getFieldName(SEQUENCING_STRATEGY_FIELD));
    checkState(sequencingStrategyMapping.isPresent(),
        "Expecting codelist to exists for: '%s.%s'",
        SSM_M_TYPE, SEQUENCING_STRATEGY_FIELD);

    return sequencingStrategyMapping.get();
  }

}
