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
package org.icgc.dcc.submission.validation.service;

import static cascading.stats.CascadingStats.Status.FAILED;
import static cascading.stats.CascadingStats.Status.STOPPED;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;
import static org.icgc.dcc.submission.validation.report.Outcome.PASSED;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.InvalidStateException;
import org.icgc.dcc.submission.release.ReleaseService;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.validation.FilePresenceException;
import org.icgc.dcc.submission.validation.Plan;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.report.Outcome;
import org.icgc.dcc.submission.validation.report.SchemaReport;
import org.icgc.dcc.submission.validation.report.SubmissionReport;
import org.icgc.dcc.submission.validation.report.ValidationErrorReport;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeListener;
import cascading.stats.CascadingStats.Status;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.typesafe.config.Config;

/**
 * Manages validation queue:<br>
 * - Launches validation for queued submissions<br>
 * - Updates submission states upon termination of the validation process
 */
@Slf4j
public class ValidationQueueManagerService extends AbstractScheduledService {

  /**
   * Period at which the service polls for an open release and for enqueued projects there is one.
   */
  private static final int POLLING_PERIOD_SECONDS = 1;

  /**
   * Default value for maximum number of concurrent validations.
   */
  private static final int DEFAULT_MAX_VALIDATING = 1;

  private static final String MAX_VALIDATING_CONFIG_PARAM = "validator.max_simultaneous";

  private final int maxValidating;
  private final ReleaseService releaseService;
  private final ValidationService validationService;
  private final MailService mailService;

  private final Map<String, Plan> validationSlots = new ConcurrentHashMap<String, Plan>();

  /**
   * To keep track of parallel validations. Volatility is guaranteed according to
   * http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/atomic/package-summary.html.
   */
  private final AtomicInteger parallelValidationCount = new AtomicInteger();

  @Inject
  public ValidationQueueManagerService(final ReleaseService releaseService, ValidationService validationService,
      MailService mailService, Config config) {
    this.releaseService = checkNotNull(releaseService);
    this.validationService = checkNotNull(validationService);
    this.mailService = mailService;

    this.maxValidating = getMaxValidating(config);
  }

  public void killValidation(String projectKey) throws InvalidStateException {
    synchronized (validationSlots) {
      val plan = validationSlots.get(projectKey);
      if (plan != null) {
        log.info("Setting 'killed' flag validation for project: {}, plan: {}", projectKey, plan);
        plan.kill();
        log.info("Stopping cascade for project: {}", projectKey);
        plan.stopCascade();
        log.info("Successfully killed validation for project: {}", projectKey);
      } else {
        log.info("Validation plan not found for project validation: {}", projectKey);
      }

      log.info("Resetting database and file system state for killed project validation: {}...", projectKey);
      releaseService.deleteQueuedRequest(projectKey);
    }
  }

  @Override
  protected void runOneIteration() throws Exception {
    // TODO: SUBM-1 - Ideally we wouldn't do that every time (but requires creating yet a separate thread for that)
    // TODO: If first time, send out email (wait for SUBM-1 to be done)
    pollOpenRelease();
    pollQueue();
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(POLLING_PERIOD_SECONDS, POLLING_PERIOD_SECONDS, SECONDS);
  }

  private void pollOpenRelease() {
    long count;
    do {
      sleepUninterruptibly(POLLING_PERIOD_SECONDS, SECONDS);
      count = releaseService.countOpenReleases();
    } while (count == 0);

    checkState(count == 1, "Expecting one and only one '%s' release, instead getting '%s'",
        OPENED, count);
  }

  /**
   * Kicks off processing for the next project if there is one and handles potential errors.
   */
  private void pollQueue() {
    log.debug("Polling queue...");
    Optional<QueuedProject> optionalNextProject = absent();
    Optional<Throwable> criticalThrowable = absent();
    try {
      val release = resolveOpenRelease();
      optionalNextProject = release.nextInQueue();
      if (optionalNextProject.isPresent() && hasEmptyValidationSlot()) {
        processNext(release, optionalNextProject.get());
      }
    } catch (FilePresenceException e) { // TODO: DCC-1820
      try {
        handleMalformedValidation(e);
      } catch (Throwable t) {
        log.info("Caught an unexpected {} upon trying to abort validation", t.getClass().getSimpleName());
        criticalThrowable = Optional.fromNullable(t);
      }
    } catch (Throwable t) {
      // Exception thrown within the run method are not logged otherwise (NullPointerException
      // for instance)
      log.info("Caught an unexpected {}", t.getClass().getSimpleName());
      criticalThrowable = Optional.fromNullable(t);
    } finally {
      // When a scheduled job throws an exception to the executor, all future runs of the job are cancelled. Thus, we
      // should never throw an exception to our executor otherwise a server restart is necessary.
      if (criticalThrowable.isPresent()) {
        processCriticalThrowable(
            criticalThrowable.get(),
            optionalNextProject);
      }
    }
  }

  /**
   * Handles the next item in the queue if there are any.
   * <p>
   * Note that a checked exception {@code FilePresenceException} may be thrown and that section 14.19 of the JLS
   * guarantees that the lock will be released in that case as well.
   * 
   * @throws FilePresenceException
   */
  private void processNext(Release release, QueuedProject project) throws FilePresenceException { // TODO: DCC-1820;
    log.info("Processing next project in queue: '{}'", project);
    mailService.sendProcessingStarted(project.getKey(), project.getEmails());

    releaseService.dequeueToValidating(project);
    Plan plan = validationService.prepareValidation(release, project, new ValidationCascadeListener());

    log.info("Adding to validation slots: '{}'", project);
    validationSlots.put(project.getKey(), plan);

    /**
     * Note that emptying of the .validation directory happens right before launching the cascade in
     * {@link Plan#startCascade()}
     */
    log.info("Starting validation: '{}'", project);
    validationService.startValidation(plan); // non-blocking
  }

  private Release resolveOpenRelease() {
    val release = releaseService.resolveNextRelease().getRelease();
    checkState(release.getState() == OPENED, "Release is expected to be '%s'", OPENED);
    return release;
  }

  /**
   * Triggered by our application.
   */
  private void handleMalformedValidation(FilePresenceException e) {
    val plan = e.getPlan();
    checkState(!plan.hasFileLevelErrors(),
        "Unexpected by design since this should be the condition for throwing the FatalPlanningException");

    Map<String, TupleState> fileLevelErrors = plan.getFileLevelErrors();
    log.info("There are file-level errors (fatal ones):\n\t{}", fileLevelErrors);

    QueuedProject queuedProject = checkNotNull(plan.getQueuedProject());
    String projectKey = queuedProject.getKey();
    log.info("About to dequeue project key {}", projectKey);
    resolveSubmission(queuedProject, SubmissionState.INVALID);

    SubmissionReport report = new SubmissionReport();

    List<SchemaReport> schemaReports = new ArrayList<SchemaReport>();
    for (String schema : fileLevelErrors.keySet()) {
      SchemaReport schemaReport = new SchemaReport();
      Iterator<TupleState.TupleError> es = fileLevelErrors.get(schema).getErrors().iterator();
      List<ValidationErrorReport> errReport = newArrayList();
      while (es.hasNext()) {
        errReport.add(new ValidationErrorReport(es.next()));
      }
      schemaReport.addErrors(errReport);
      schemaReport.setName(schema);
      schemaReports.add(schemaReport);
    }

    report.setSchemaReports(schemaReports);
    setSubmissionReport(projectKey, report);
  }

  /**
   * Triggered by cascading (via listener on cascade).
   */
  private void handleUnexpectedTermination(Plan plan) {
    log.error("Unexpected termination of project '{}' validaton. Resolving to {} submission state.",
        plan.getProjectKey(), ERROR);
    resolveSubmission(plan.getQueuedProject(), ERROR);
  }

  /**
   * Triggered by cascading (via listener on cascade).
   */
  private void handleCompletedValidation(Plan plan) {
    checkArgument(plan != null);
    QueuedProject queuedProject = plan.getQueuedProject();
    String projectKey = queuedProject.getKey();
    Status cascadeStatus = plan.getCascade().getCascadeStats().getStatus();

    log.info("Cascade completed for project {} with cascade status: {}, killed: {}",
        new Object[] { projectKey, cascadeStatus, plan.isKilled() });
    if (FAILED == cascadeStatus) {
      log.error("Validation failed: cascade completed with a {} status; About to dequeue project key {}",
          cascadeStatus, projectKey);
      resolveSubmission(queuedProject, ERROR);
    } else if (STOPPED == cascadeStatus) {
      log.info("Validation successfully stopped: {}", projectKey);
    } else {
      log.info("Gathering report for project {}", projectKey);
      SubmissionReport report = new SubmissionReport();
      Outcome outcome = plan.collect(report);
      log.info("Gathered report for project {}", projectKey);

      // Resolving submission
      if (outcome == PASSED) {
        resolveSubmission(queuedProject, VALID);
      } else {
        resolveSubmission(queuedProject, INVALID);
      }

      setSubmissionReport(projectKey, report);

      log.info("Validation finished normally for project {}, time spent on validation is {} seconds", projectKey,
          plan.getDuration() / 1000.0);
    }
  }

  private void setSubmissionReport(String projectKey, SubmissionReport report) {
    log.info("Starting report collecting on project {}", projectKey);

    Release release = resolveOpenRelease();
    Submission submission = this.releaseService.getSubmission(release.getName(), projectKey);
    submission.setReport(report);

    // persist the report to DB
    // DCC-799: Runtime type will be SubmissionReport. Static type is Object to untangle cyclic dependencies between
    // dcc-submission-server and dcc-submission-core.
    this.releaseService
        .updateSubmissionReport(release.getName(), projectKey, (SubmissionReport) submission.getReport());
    log.info("Report collecting finished on project {}", projectKey);
  }

  private void processCriticalThrowable(Throwable t, Optional<QueuedProject> optionalNextProject) {
    log.error("A critical error occured while processing the validation queue", t);

    if (optionalNextProject.isPresent()) {
      QueuedProject project = optionalNextProject.get();
      try {
        resolveSubmission(project, ERROR);
      } catch (Throwable t2) {
        log.error("a critical error occured while attempting to dequeue project " + project.getKey(), t2);
      }
    } else {
      log.error("next project in queue not present, could not dequeue nor set submission state to {}",
          ERROR);
    }
  }

  /**
   * Submission resolution can happen in 4 manners:<br/>
   * - 1. cascading listener's onComplete(): VALID or INVALID<br/>
   * - 2. catching of {@code FilePresenceException} (typically a missing required file): INVALID<br/>
   * - 3. cascading listener's onStopping(): ERROR<br/>
   * - 4. catching of an unknown exception: ERROR<br/>
   */
  private void resolveSubmission(QueuedProject project, SubmissionState state) {
    String key = project.getKey();
    log.info("Resolving project '{}' to submission state '{}'", key, state);
    releaseService.resolve(key, state);

    decrementParallelValidationCount();
    if (project.getEmails().isEmpty() == false) {
      this.email(project, state);
    }

    log.info("Resolved {}", key);
  }

  /**
   * Determines if runnable and increments counter if it is the case.
   * <p>
   * Whether there is a spot available for validation to occur (increment and give green light) or whether the
   * submission should remain in the queue.
   * <p>
   * Counterpart operation to <code>{@link ValidationQueueManagerService#decrementParallelValidationCount()}</code>
   */
  private synchronized boolean hasEmptyValidationSlot() {
    int original = parallelValidationCount.intValue();
    if (original < maxValidating) {
      checkState(parallelValidationCount.incrementAndGet() <= maxValidating, "by design: %s",
          parallelValidationCount.intValue());
    }
    return original < parallelValidationCount.intValue();
  }

  /**
   * Decrements the parallel validation counter.
   * <p>
   * Counterpart operation to <code>{@link ValidationQueueManagerService#hasEmptyValidationSlot()}</code>
   */
  private synchronized void decrementParallelValidationCount() {
    checkState(parallelValidationCount.decrementAndGet() >= 0);
  }

  private void email(QueuedProject project, SubmissionState state) { // TODO: SUBM-5
    val release = resolveOpenRelease();

    Set<Address> addresses = newHashSet();
    for (String email : project.getEmails()) {
      try {
        val address = new InternetAddress(email);
        addresses.add(address);
      } catch (AddressException e) {
        log.error("Illegal Address: " + e);
      }
    }

    if (!addresses.isEmpty()) {
      mailService.sendValidated(release.getName(), project.getKey(), state, addresses);
    }
  }

  private static int getMaxValidating(Config config) {
    return config.hasPath(MAX_VALIDATING_CONFIG_PARAM) ?
        config.getInt(MAX_VALIDATING_CONFIG_PARAM) :
        DEFAULT_MAX_VALIDATING;
  }

  class ValidationCascadeListener implements CascadeListener {

    Plan plan;

    public void setPlan(Plan plan) {
      this.plan = plan;
    }

    @Override
    public void onStarting(Cascade cascade) {
      // cascade.start() called
      log.info("{}: onStarting, plan: {}", getName(), plan);
    }

    @Override
    public void onStopping(Cascade cascade) {
      // cascade.stop() called
      log.info("{}: onStopping, plan: {}", getName(), plan);

      if (!plan.isKilled()) {
        handleUnexpectedTermination(plan);
      }
    }

    @Override
    public boolean onThrowable(Cascade cascade, Throwable throwable) {
      log.error(getName() + ": onThrowable:", throwable);

      // No-op for now; false indicates that the throwable was not handled and needs to be re-thrown
      val handled = false;
      return handled;
    }

    @Override
    public void onCompleted(Cascade cascade) {
      // Always called, regardless of cascade status and thrown throwables
      log.info("{}: onCompleted, plan: {}", getName(), plan);

      synchronized (validationSlots) {
        handleCompletedValidation(plan);
        validationSlots.remove(plan);
      }
    }

    private String getName() {
      return format("%s [%s]", this.getClass().getSimpleName(), plan.getProjectKey());
    }
  }

}
