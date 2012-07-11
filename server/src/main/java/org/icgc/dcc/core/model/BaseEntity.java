package org.icgc.dcc.core.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

@Entity
public class BaseEntity extends Timestamped implements HasId {

  @Id
  @JsonIgnore
  protected ObjectId id;

  protected BaseEntity() {
    this.created = new Date();
  }

  @Override
  public ObjectId getId() {
    return id;
  }
}
