package org.icgc.dcc.submission.reporter.cascading;

import static cascading.cascade.CascadeDef.cascadeDef;
import static cascading.flow.FlowDef.flowDef;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Maps.transformValues;
import static org.icgc.dcc.core.util.Jackson.formatPrettyJson;
import static org.icgc.dcc.core.util.Optionals.ABSENT_STRING;
import static org.icgc.dcc.submission.reporter.Reporter.getFilePath;

import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.hadoop.cascading.Cascades;
import org.icgc.dcc.hadoop.cascading.CascadingContext;
import org.icgc.dcc.hadoop.cascading.Flows;
import org.icgc.dcc.hadoop.cascading.taps.GenericTaps;
import org.icgc.dcc.hadoop.cascading.taps.LocalTaps;
import org.icgc.dcc.hadoop.util.HadoopConstants;
import org.icgc.dcc.hadoop.util.HadoopProperties;
import org.icgc.dcc.submission.reporter.OutputType;
import org.icgc.dcc.submission.reporter.Reporter;
import org.icgc.dcc.submission.reporter.ReporterInput;

import cascading.cascade.Cascade;
import cascading.flow.FlowConnector;
import cascading.pipe.Pipe;
import cascading.tap.Tap;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

@Slf4j
public class ReporterConnector {

  private static final String CONCURRENCY = String.valueOf(5);

  private final CascadingContext cascadingContext;
  private final String outputDirPath;

  public ReporterConnector(
      final boolean local,
      @NonNull final String outputDirPath) {
    this.outputDirPath = outputDirPath;
    this.cascadingContext = local ?
        CascadingContext.getLocal() :
        CascadingContext.getDistributed();
    log.info(cascadingContext.getConnectors().describe());
  }

  public Cascade connectPreComputationCascade(
      @NonNull final ReporterInput reporterInput,
      @NonNull final String releaseName,
      @NonNull final Pipe preComputationTable,
      @NonNull final Map<?, ?> hadoopProperties) {

    val maxConcurrentFlows = getConcurrency();
    log.info("maxConcurrentFlows: '{}'", maxConcurrentFlows);
    log.info("hadoopProperties: '{}'", hadoopProperties);
    for (val projectKey : reporterInput.getProjectKeys()) {
      log.info(formatPrettyJson(reporterInput.getPipeNameToFilePath(projectKey)));

      // TODO: same for outputs
    }

    val cascadeDef = cascadeDef()
        .setName(Cascades.getName(Reporter.CLASS))
        .setMaxConcurrentFlows(maxConcurrentFlows);

    val flowDef = flowDef();
    for (val projectKey : reporterInput.getProjectKeys()) {
      flowDef.addSources(getRawInputTaps(reporterInput, projectKey));
    }
    flowDef.addTailSink(preComputationTable, getRawIntermediateOutputTap(releaseName));
    cascadeDef.addFlow(
        getFlowConnector(hadoopProperties)
            .connect(flowDef));
    HadoopProperties.setHadoopUserNameProperty();

    log.info("Connecting pre-computation cascade");
    return cascadingContext
        .getConnectors()
        .getCascadeConnector(hadoopProperties)
        .connect(cascadeDef);
  }

  public Cascade connectFinalCascade(
      @NonNull final String releaseName,
      @NonNull final Set<String> projectKeys,
      @NonNull final Pipe preComputationTable,
      @NonNull final Map<String, Pipe> projectDataTypeEntities,
      @NonNull final Map<String, Pipe> projectSequencingStrategies,
      @NonNull final Map<?, ?> hadoopProperties) {

    val maxConcurrentFlows = getConcurrency();
    log.info("maxConcurrentFlows: '{}'", maxConcurrentFlows);
    log.info("hadoopProperties: '{}'", hadoopProperties);

    val cascadeDef = cascadeDef()
        .setName(Cascades.getName(Reporter.CLASS))
        .setMaxConcurrentFlows(maxConcurrentFlows);

    for (val projectKey : projectKeys) {
      val projectDataTypeEntity = projectDataTypeEntities.get(projectKey);
      val projectSequencingStrategy = projectSequencingStrategies.get(projectKey);
      cascadeDef.addFlow(
          getFlowConnector(hadoopProperties).connect(
              flowDef()
                  .addSource(
                      preComputationTable,
                      getRawIntermediateOutputTap(releaseName))
                  .addTailSink(
                      projectDataTypeEntity,
                      getRawOutputProjectDataTypeEntityTap(projectDataTypeEntity.getName(), releaseName, projectKey))
                  .addTailSink(
                      projectSequencingStrategy,
                      getRawOutputProjectSequencingStrategyTap(projectSequencingStrategy.getName(), releaseName,
                          projectKey))
                  .setName(Flows.getName(Reporter.CLASS, projectKey))));
    }

    log.info("Connecting final cascade");
    HadoopProperties.setHadoopUserNameProperty();
    return cascadingContext
        .getConnectors()
        .getCascadeConnector(hadoopProperties)
        .connect(cascadeDef);
  }

  private Integer getConcurrency() {
    return Integer.valueOf(firstNonNull(
        // To ease benchmarking until we find the sweet spot
        System.getProperty("DCC_REPORT_CONCURRENCY"),
        CONCURRENCY));
  }

  private FlowConnector getFlowConnector(@NonNull final Map<?, ?> hadoopProperties) {
    return cascadingContext
        .getConnectors()
        .getFlowConnector(ImmutableMap.builder()

            .putAll(hadoopProperties)
            .putAll(
                HadoopProperties.enableIntermediateMapOutputCompression(
                    HadoopProperties.setAvailableCodecs(newLinkedHashMap()),
                    HadoopConstants.LZO_CODEC_PROPERTY_VALUE))

            .build());
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private Tap getRawIntermediateOutputTap(@NonNull final String releaseName) {
    return GenericTaps.RAW_CASTER.apply(getIntermediateTap(releaseName));
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private Map<String, Tap> getRawInputTaps(
      @NonNull final ReporterInput reporterInput,
      @NonNull final String projectKey) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getInputTaps(

        // get pipe to path map for the project/file type combination
        reporterInput.getPipeNameToFilePath(projectKey)),

        GenericTaps.RAW_CASTER);
  }

  private Map<String, Tap<?, ?, ?>> getInputTaps(
      @NonNull final Map<String, String> pipeNameToFilePath) {

    return transformValues(
        pipeNameToFilePath,
        new Function<String, Tap<?, ?, ?>>() {

          @Override
          public Tap<?, ?, ?> apply(final String path) {
            return cascadingContext
                .getTaps()
                .getDecompressingTsvWithHeader(path);
          }

        });
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private Tap getRawOutputProjectDataTypeEntityTap(
      @NonNull final String tailName,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    return GenericTaps.RAW_CASTER.apply(getOutputProjectDataTypeEntityTap(tailName, releaseName, projectKey));
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private Tap getRawOutputProjectSequencingStrategyTap(
      @NonNull final String tailName,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    return GenericTaps.RAW_CASTER.apply(getOutputProjectSequencingStrategyTap(tailName, releaseName, projectKey));
  }

  private Tap<?, ?, ?> getOutputProjectDataTypeEntityTap(
      @NonNull final String tailName,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    val outputFilePath = getFilePath(
        outputDirPath, OutputType.DONOR, releaseName, Optional.of(projectKey));
    return cascadingContext
        .getTaps()
        .getNoCompressionTsvWithHeader(outputFilePath);
  }

  private Tap<?, ?, ?> getOutputProjectSequencingStrategyTap(
      @NonNull final String tailName,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    val outputFilePath = getFilePath(
        outputDirPath, OutputType.SEQUENCING_STRATEGY, releaseName, Optional.of(projectKey));
    return cascadingContext
        .getTaps()
        .getNoCompressionTsvWithHeader(outputFilePath);
  }

  private Tap<?, ?, ?> getIntermediateTap(@NonNull final String releaseName) {
    val outputFilePath = getFilePath(outputDirPath, OutputType.PRE_COMPUTATION, releaseName, ABSENT_STRING);
    return cascadingContext.getTaps().getNoCompressionTsvWithHeader(outputFilePath);
  }

}
