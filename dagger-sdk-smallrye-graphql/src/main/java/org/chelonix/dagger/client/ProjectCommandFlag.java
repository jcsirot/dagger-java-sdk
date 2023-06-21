// This class has been generated by dagger-java-sdk. DO NOT EDIT.
package org.chelonix.dagger.client;

import java.lang.InterruptedException;
import java.lang.String;
import java.util.concurrent.ExecutionException;

/**
 * A flag accepted by a project command.
 */
public class ProjectCommandFlag {
  private QueryContext queryContext;

  private String description;

  private String name;

  /**
   * Empty constructor for JSON-B deserialization
   */
  protected ProjectCommandFlag() {
  }

  ProjectCommandFlag(QueryContext queryContext) {
    this.queryContext = queryContext;
  }

  /**
   * <p>Documentation for what this flag sets.</p>
   */
  public String description() throws InterruptedException, ExecutionException,
      DaggerQueryException {
    if (this.description != null) {
      return description;
    }
    QueryContext ctx = this.queryContext.chain("description");
    return ctx.executeQuery(String.class);
  }

  /**
   * <p>The name of the flag.</p>
   */
  public String name() throws InterruptedException, ExecutionException, DaggerQueryException {
    if (this.name != null) {
      return name;
    }
    QueryContext ctx = this.queryContext.chain("name");
    return ctx.executeQuery(String.class);
  }
}
