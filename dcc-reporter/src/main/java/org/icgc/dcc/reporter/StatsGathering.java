package org.icgc.dcc.reporter;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.REPLACE;
import static com.google.common.collect.Iterables.toArray;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.hadoop.cascading.TupleEntries.getFirstInteger;
import static org.icgc.dcc.reporter.OutputType.DONOR;
import static org.icgc.dcc.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.reporter.OutputType.SEQUENCING_STRATEGY;
import static org.icgc.dcc.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.reporter.ReporterFields.ANALYSIS_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import lombok.val;

import org.icgc.dcc.hadoop.cascading.SubAssemblies;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.CountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.CountBy.CountByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.GroupBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.GroupBy.GroupByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NamingPipe;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.pipe.Every;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.Unique;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.google.common.collect.ImmutableList;

public class StatsGathering extends SubAssembly {

  StatsGathering(Pipe preComputationTable) {
    setTails(toArray(
        process(preComputationTable),
        Pipe.class));
  }

  private static Iterable<Pipe> process(Pipe preComputationTable) {
    return ImmutableList.<Pipe> builder()

        .add(processDonors(preComputationTable))
        .add(processSpecimens(preComputationTable))
        .add(processSamples(preComputationTable))
        .add(processObservations(preComputationTable))
        // .add(processSequencingStrategies(preComputationTable))

        // For table 2
        .add(processSequencingStrategies(preComputationTable)) // TODO: encapsulate elsewhere?

        .build();
  }

  private static Pipe processDonors(Pipe preComputationTable) {
    return new NamingPipe(
        DONOR,

        //
        getUniqueCountPipe(
            preComputationTable,

            // Target field
            DONOR_ID_FIELD,

            // Group by fields
            PROJECT_ID_FIELD.append(TYPE_FIELD)));
  }

  private static Pipe processSpecimens(Pipe preComputationTable) {
    return new NamingPipe(
        SPECIMEN,
        getUniqueCountPipe(
            preComputationTable,
            SPECIMEN_ID_FIELD,
            PROJECT_ID_FIELD
                .append(TYPE_FIELD)));
  }

  private static Pipe processSamples(Pipe preComputationTable) {
    return new NamingPipe(
        SAMPLE,
        getUniqueCountPipe(
            preComputationTable,
            SAMPLE_ID_FIELD,
            PROJECT_ID_FIELD
                .append(TYPE_FIELD)));
  }

  private static Pipe processObservations(Pipe preComputationTable) {
    return new NamingPipe(
        OBSERVATION,
        getPreCountedUniqueCountPipe(
            preComputationTable,
            ANALYSIS_ID_FIELD,
            _ANALYSIS_OBSERVATION_COUNT_FIELD,
            PROJECT_ID_FIELD
                .append(TYPE_FIELD)));
  }

  /**
   * TODO: promote unique count by to {@link SubAssemblies}?
   */
  private static Pipe processSequencingStrategies(Pipe preComputationTable) {
    val countByFields = PROJECT_ID_FIELD.append(SEQUENCING_STRATEGY_FIELD);

    return

    //
    new NamingPipe(
        SEQUENCING_STRATEGY,

        //
        new CountBy(CountByData.builder()

            .pipe(

                //
                new Unique(

                    //
                    new Retain(
                        preComputationTable,
                        countByFields
                            .append(DONOR_ID_FIELD)
                    ),

                    ALL))

            .countByFields(countByFields)
            .resultField(getCountFieldCounterpart(SEQUENCING_STRATEGY_FIELD))

            .build()));
  }

  // see https://gist.github.com/ceteri/4459908
  private static Pipe getUniqueCountPipe(Pipe preComputationTable, Fields targetField, Fields groupByFields) {
    checkFieldsCardinalityOne(targetField);

    return

    //
    new CountBy(CountByData.builder()

        .pipe(

            //
            new Unique( // TODO: automatically retains?
                preComputationTable,

                // Unique fields
                targetField.append(groupByFields)))

        .countByFields(groupByFields)
        .resultField(getCountFieldCounterpart(targetField))

        .build());
  }

  private static Pipe getPreCountedUniqueCountPipe(
      Pipe preComputationTable, Fields targetField, Fields preCountField, Fields groupByFields) {
    checkFieldsCardinalityOne(targetField);
    checkFieldsCardinalityOne(preCountField);

    /**
     * TODO: cascading pre-defined buffer?
     */
    class Sum extends BaseOperation<Void> implements Buffer<Void> {

      public Sum() {
        super(ARGS);
      }

      @Override
      public void operate(
          @SuppressWarnings("rawtypes") FlowProcess flowProcess,
          BufferCall<Void> bufferCall) {

        long observationCount = 0;
        val entries = bufferCall.getArgumentsIterator();
        while (entries.hasNext()) {
          observationCount += getFirstInteger(entries.next());
        }
        bufferCall.getOutputCollector().add(new Tuple(observationCount));
      }

    }

    return

    //
    new Retain(

        //
        new Every(

            //
            new GroupBy(GroupByData.builder()

                .pipe(

                    //
                    new Unique(
                        preComputationTable,

                        //
                        groupByFields
                            .append(preCountField)
                            .append(targetField)))

                .groupByFields(groupByFields)

                .build()),

            //
            preCountField,

            //
            new Sum(),

            REPLACE),

        // Retain fields
        groupByFields
            .append(preCountField));
  }

}