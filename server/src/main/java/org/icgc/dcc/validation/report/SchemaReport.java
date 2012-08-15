package org.icgc.dcc.validation.report;

import java.util.List;

import org.icgc.dcc.validation.cascading.TupleState;

import com.google.code.morphia.annotations.Embedded;

@Embedded
public class SchemaReport {

  protected String name;

  protected List<FieldReport> fieldReports;

  protected List<TupleState> errors;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<FieldReport> getFieldReports() {
    return fieldReports;
  }

  public void setFieldReports(List<FieldReport> fieldReports) {
    this.fieldReports = fieldReports;
  }

  public List<TupleState> getErrors() {
    return errors;
  }

  public void setErrors(List<TupleState> errors) {
    this.errors = errors;
  }

  public FieldReport getFieldReport(String field) {
    for(FieldReport report : this.fieldReports) {
      if(report.getName().equals(field)) {
        return report;
      }
    }
    return null;
  }
}
