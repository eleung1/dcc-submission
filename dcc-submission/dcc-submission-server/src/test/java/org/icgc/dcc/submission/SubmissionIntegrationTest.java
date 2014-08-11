/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not,see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES,INCLUDING,BUT NOT LIMITED TO,THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,INDIRECT,                               
 * INCIDENTAL,SPECIAL,EXEMPLARY,OR CONSEQUENTIAL DAMAGES (INCLUDING,BUT NOT LIMITED                          
 * TO,PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,DATA,OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,WHETHER                              
 * IN CONTRACT,STRICT LIABILITY,OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE,EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.core.util.FsConfig.FS_ROOT;
import static org.icgc.dcc.submission.TestUtils.$;
import static org.icgc.dcc.submission.TestUtils.CODELISTS_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.DICTIONARIES_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.NEXT_RELEASE_ENPOINT;
import static org.icgc.dcc.submission.TestUtils.PROJECTS_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.QUEUE_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.RELEASES_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.SEED_CODELIST_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.SEED_DICTIONARIES_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.SIGNOFF_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.TEST_CONFIG;
import static org.icgc.dcc.submission.TestUtils.TEST_CONFIG_FILE;
import static org.icgc.dcc.submission.TestUtils.UPDATE_RELEASE_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.VALIDATION_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.addScript;
import static org.icgc.dcc.submission.TestUtils.asDetailedSubmission;
import static org.icgc.dcc.submission.TestUtils.asRelease;
import static org.icgc.dcc.submission.TestUtils.asReleaseView;
import static org.icgc.dcc.submission.TestUtils.asString;
import static org.icgc.dcc.submission.TestUtils.codeListsToString;
import static org.icgc.dcc.submission.TestUtils.dataTypesToString;
import static org.icgc.dcc.submission.TestUtils.delete;
import static org.icgc.dcc.submission.TestUtils.dictionary;
import static org.icgc.dcc.submission.TestUtils.dictionaryToString;
import static org.icgc.dcc.submission.TestUtils.dictionaryVersion;
import static org.icgc.dcc.submission.TestUtils.get;
import static org.icgc.dcc.submission.TestUtils.post;
import static org.icgc.dcc.submission.TestUtils.put;
import static org.icgc.dcc.submission.TestUtils.replaceDictionaryVersion;
import static org.icgc.dcc.submission.dictionary.util.Dictionaries.writeDictionary;
import static org.icgc.dcc.submission.fs.ReleaseFileSystem.SYSTEM_FILES_DIR_NAME;
import static org.icgc.dcc.submission.release.model.ReleaseState.COMPLETED;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.QUEUED;
import static org.icgc.dcc.submission.release.model.SubmissionState.SIGNED_OFF;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALIDATING;
import static org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy.FILE_NAME_SEPARATOR;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.INVALID_STATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.FileTypes;
import org.icgc.dcc.core.util.AppUtils;
import org.icgc.dcc.submission.config.ConfigModule;
import org.icgc.dcc.submission.core.PersistenceModule;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.GuiceJUnitRunner;
import org.icgc.dcc.submission.fs.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.sftp.Sftp;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongodb.morphia.Datastore;

import com.dumbster.smtp.SimpleSmtpServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.jcraft.jsch.SftpException;

@Slf4j
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ConfigModule.class, PersistenceModule.class })
public class SubmissionIntegrationTest extends BaseIntegrationTest {

  /**
   * Switch that will change environments from "local" if {@code true} and "hadoop" if {@code false}.
   */
  private static final boolean LOCAL = true;

  /**
   * Test file system.
   */
  private static final String INTEGRATION_TEST_DIR = "/fixtures/submission";
  private static final String DESTINATION_DIR_NAME = "dcc_root_dir";
  private static final String FS_DIR = "src/test/resources" + INTEGRATION_TEST_DIR + "/" + DESTINATION_DIR_NAME;

  /**
   * Projects.
   * 
   * If changing project names,must also change their directory counterparts under:
   * <p>
   * {@link file:///src/test/resources/fixtures/submission/fs/release1}.
   */
  private static final String PROJECT1_KEY = "project.1";
  private static final String PROJECT1 = _("{name:'Project One',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT1_KEY);
  private static final String PROJECT2_KEY = "project.2";
  private static final String PROJECT2 = _("{name:'Project Two',key:'%s',users:['admin','brett'],groups:['admin']}",
      PROJECT2_KEY);
  private static final String PROJECT3_KEY = "project.3";
  private static final String PROJECT3 = _("{name:'Project Three',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT3_KEY);
  private static final String PROJECT4_KEY = "project.4";
  private static final String PROJECT4 = _("{name:'Project Four',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT4_KEY);
  private static final String PROJECT5_KEY = "project.5";
  private static final String PROJECT5 = _("{name:'Project Five',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT5_KEY);
  private static final String PROJECT6_KEY = "project.6";
  private static final String PROJECT6 = _("{name:'Project Six',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT6_KEY);
  private static final String PROJECT7_KEY = "project.7";
  private static final String PROJECT7 = _("{name:'Project Seven',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT7_KEY);

  /**
   * Dictionaries.
   */
  private static final String SECOND_DICTIONARY = dictionaryToString();
  private static final String SECOND_DICTIONARY_VERSION = dictionaryVersion(SECOND_DICTIONARY);
  private static final String FIRST_DICTIONARY_VERSION = SECOND_DICTIONARY_VERSION + "-zero";
  private static final String FIRST_DICTIONARY = replaceDictionaryVersion(
      SECOND_DICTIONARY,
      SECOND_DICTIONARY_VERSION,
      FIRST_DICTIONARY_VERSION);
  private static final String SECOND_DICTIONARY_ARRAY = "[" + SECOND_DICTIONARY + "]";
  private static final String FIRST_DICTIONARY_ARRAY = "[" + FIRST_DICTIONARY + "]";

  /**
   * Releases.
   */
  private static final String INITITAL_RELEASE_NAME = "release1";
  private static final String INITIAL_RELEASE_ENDPOINT = RELEASES_ENDPOINT + "/" + INITITAL_RELEASE_NAME;
  private static final String INITIAL_RELEASE_SUBMISSIONS_ENDPOINT = INITIAL_RELEASE_ENDPOINT + "/submissions";

  private static final String INITIAL_RELEASE = "{"
      + "name: '" + INITITAL_RELEASE_NAME + "',"
      + "state: 'OPENED',"
      + "submissions: [],"
      + "dictionaryVersion: '" + FIRST_DICTIONARY_VERSION + "'}";
  private static final String NEXT_RELEASE_NAME = "release2";
  private static final String NEXT_RELEASE = "{"
      + "name:'" + NEXT_RELEASE_NAME + "', "
      + "dictionaryVersion: '" + SECOND_DICTIONARY_VERSION // FIXME: this shouldn't be pass (see DCC-1916)
      + "'}";

  /**
   * Messages.
   * 
   * @see http://stackoverflow.com/questions/1368163/is-there-a-standard-domain-for-testing-throwaway-email
   */
  private static final String PROJECT_TO_SIGN_OFF = "['" + PROJECT1_KEY + "']";
  private static final String PROJECT_DATA_TYPES = dataTypesToString();
  private static final String PROJECTS_TO_ENQUEUE = "["
      + "{key:'" + PROJECT1_KEY + "',emails:['project1@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT2_KEY + "',emails:['project2@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT3_KEY + "',emails:['project3@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT4_KEY + "',emails:['project4@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT5_KEY + "',emails:['project5@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT6_KEY + "',emails:['project6@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT7_KEY + "',emails:['project7@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "}"
      + "]";

  private static final String PROJECTS_TO_ENQUEUE2 = "["
      + "{key:'" + PROJECT2_KEY + "',emails:['project2@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT3_KEY + "',emails:['project3@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT4_KEY + "',emails:['project4@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT5_KEY + "',emails:['project5@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT7_KEY + "',emails:['project7@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "}]";

  /**
   * Submission file system.
   */
  private static final String PROJECT1_VALIDATION_DIR = INITITAL_RELEASE_NAME + "/" + PROJECT1_KEY + "/.validation";
  private static final String DCC_ROOT_DIR = TEST_CONFIG.getString(FS_ROOT);

  /**
   * Test utilities.
   */
  @Inject
  private Datastore datastore;
  private SimpleSmtpServer smtpServer;
  private MiniHadoop hadoop;
  private FileSystem fileSystem;

  @Rule
  public Sftp sftp = new Sftp("admin", "adminspasswd", false);

  @Before
  public void setUp() throws IOException {
    banner("Setting up ...");

    if (LOCAL) {
      status("init", "Setting up local environment...");
      fileSystem = FileSystem.get(new Configuration());

      status("init", "Deleting local root filesystem...");
      fileSystem.delete(new Path(DCC_ROOT_DIR), true);
    } else {
      // Setup Hadoop infrastructure
      status("init", "Setting up Hadoop environment...");
      hadoop = new MiniHadoop(new Configuration(), 1, 1, new File("/tmp/hadoop"));
      fileSystem = hadoop.getFileSystem();

      // Config overrides for {@code Main} consumption
      val jobConf = hadoop.createJobConf();
      System.setProperty("fs.url", jobConf.get("fs.defaultFS"));
      System.setProperty("hadoop.fs.defaultFS", jobConf.get("fs.defaultFS"));
      System.setProperty("hadoop.mapred.job.tracker", jobConf.get("mapred.job.tracker"));
      AppUtils.setTestEnvironment();
    }

    status("init", "Dropping database...");
    datastore.getDB().dropDatabase();

    status("init", "Starting SMTP server...");
    smtpServer = SimpleSmtpServer.start(TEST_CONFIG.getInt("mail.smtp.port"));

    status("init", "Starting submission server...");
    Main.main("external", TEST_CONFIG_FILE.getAbsolutePath());
  }

  @After
  @SneakyThrows
  public void tearDown() {
    if (true) {
      return;
    }

    log.info(repeat("-", 100));
    log.info("Tearing down ...");
    log.info(repeat("-", 100));

    status("shutdown", "Closing REST client...");
    client.close();
    status("shutdown", "REST client closed.");

    status("shutdown", "Shutting down SMTP server...");
    smtpServer.stop();
    status("shutdown", "SMTP server shut down.");

    status("shutdown", "Shutting down submission server...");
    Main.shutdown();
    status("shutdown", "Submission server shut down.");

    if (hadoop != null) {
      status("shutdown", "Shutting down hadoop...");
      hadoop.close();
    }

    banner("Shut down.");
  }

  @Test
  public void testSystem() throws Exception {
    status("test", "Starting test...");
    try {
      seedSystem();
      adminCreatesRelease();
      userSubmitsFiles();
      userValidates();
      adminTweaksCodeListAndTerms();
      adminRevalidates();
      adminPerformsRelease();
      adminUpdatesDictionary();
      adminUpdatesRelease();
      dumpTestDictionary();
    } catch (Exception e) {
      status("test", "Caught exception: " + e);
      throw e;
    }

    status("test", "Finished test.");
  }

  private void seedSystem() throws IOException {
    status("seed", "Seeding dictionary 1 ({})...", FIRST_DICTIONARY_VERSION);
    post(client, SEED_DICTIONARIES_ENDPOINT, FIRST_DICTIONARY_ARRAY);

    status("seed", "Seeding dictionary 2 ({} from dcc-resources)...", SECOND_DICTIONARY_VERSION);
    post(client, SEED_DICTIONARIES_ENDPOINT, SECOND_DICTIONARY_ARRAY);

    status("seed", "Seeding code lists...");
    post(client, SEED_CODELIST_ENDPOINT, codeListsToString());
  }

  private void adminCreatesRelease() throws Exception, IOException {
    status("admin", "Creating initial release...");
    createInitialRelease();
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates());

    status("admin", "Adding projects...");
    addProjects();
    checkRelease(
        INITITAL_RELEASE_NAME,
        FIRST_DICTIONARY_VERSION,
        OPENED,
        hasSubmisisonStates(
            NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED));

    status("admin", "Updating OPEN dictionary...");
    updateDictionary(
        FIRST_DICTIONARY, FIRST_DICTIONARY_VERSION, NO_CONTENT.getStatusCode());
  }

  @SneakyThrows
  private void userSubmitsFiles() throws IOException {
    val source = new Path(FS_DIR);
    val destination = new Path(DCC_ROOT_DIR);
    status("user", "SFTP transferring files from '{}' to '{}'...", source, destination);

    boolean manipulatedFiles = false;

    try {
      sftp.connect();
      for (val releaseDir : new File(FS_DIR).listFiles()) {
        for (val projectDir : releaseDir.listFiles()) {
          val releaseName = projectDir.getParentFile().getName();
          for (val file : projectDir.listFiles()) {
            val projectName = file.getParentFile().getName();

            // Cannot submit system files via SFTP
            val system = projectName.equals(SYSTEM_FILES_DIR_NAME);
            if (system) {
              val path = new Path(destination, releaseName + "/" + SYSTEM_FILES_DIR_NAME + "/" + file.getName());
              status("user", "Installing system file from '{}' to '{}'...", file, path);
              fileSystem.copyFromLocalFile(new Path(file.getAbsolutePath()), path);
            } else {
              val path = projectName + "/" + file.getName();
              status("user", "SFTP transferring file from '{}' to '{}'...", file, path);
              sftp.put(path, file);

              if (!manipulatedFiles) {
                // Simulate some additional SFTP file interactions
                userManipulatesFiles(file, path);

                manipulatedFiles = true;
              }
            }
          }
        }
      }
    } finally {
      sftp.disconnect();
    }

    val list = fileSystem.listFiles(destination, true);
    while (list.hasNext()) {
      log.info("Copied: {}", list.next().getPath());
    }
  }

  private void userManipulatesFiles(File file, String path) throws SftpException {
    sftp.rename(path, path + ".bak");
    sftp.rename(path + ".bak", path);
    sftp.rm(path);
    sftp.put(path, file);
  }

  private void userValidates() throws Exception {
    // Triggers validations
    enqueueProjects(PROJECTS_TO_ENQUEUE, NO_CONTENT);

    // TODO: Can't do this unless we can support a test method verifying a project is in either INVALID or NOT_VALIDATED
    // states due lack of synchronization with the validation scheduler (i.e. results are unpredictable):

    // status("user","Awaiting validation for project '{}'...",PROJECT2_KEY);
    // awaitValidatingState(PROJECT2_KEY);
    //
    // status("user","Cancelling validation for project '{}'...",PROJECT2_KEY);
    // cancelValidation(PROJECT2_KEY,OK);

    checkValidations();
  }

  private void adminTweaksCodeListAndTerms() throws IOException, Exception {
    status("admin", "Adding offending code list...");
    addInvalidCodeList();

    status("admin", "Adding valid code list...");
    addValidCodeLists();

    status("admin", "Adding code list term...");
    addCodeListTerms();
  }

  private void adminRevalidates() throws Exception {
    // Re-enqueue them since they have been reset by adding the term
    enqueueProjects(PROJECTS_TO_ENQUEUE2, NO_CONTENT);

    status("admin", "Checking validated submission 1...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT1_KEY, VALID);

    status("admin", "Checking validated submission 2...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT2_KEY, INVALID);

    status("admin", "Checking validated submission 3...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT3_KEY, INVALID);

    status("admin", "Checking validated submission 4...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT4_KEY, INVALID);

    status("admin", "Checking validated submission 5...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT5_KEY, INVALID);

    status("admin", "Checking validated submission 6...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT6_KEY, VALID);

    status("admin", "Checking validated submission 7...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT7_KEY, INVALID);

    // TODO: Make it such that adding a term fixed one of the submissions
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates(VALID, INVALID, INVALID, INVALID, INVALID, VALID, INVALID));
  }

  private void adminPerformsRelease() throws Exception {
    releaseInitialRelease();
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, COMPLETED,
        hasSubmisisonStates(SIGNED_OFF));
    checkRelease(NEXT_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates(NOT_VALIDATED, INVALID, INVALID, INVALID, INVALID, VALID, INVALID));
  }

  private void adminUpdatesDictionary() throws Exception, IOException {
    status("admin", "Updating CLOSED dictionary...");
    updateDictionary(
        FIRST_DICTIONARY, FIRST_DICTIONARY_VERSION, BAD_REQUEST.getStatusCode());

    status("admin", "Updating OPENED dictionary...");
    updateDictionary(
        dictionaryToString(), SECOND_DICTIONARY_VERSION, NO_CONTENT.getStatusCode());

    status("admin", "Adding Script restriction #1 to OPENED dictionary");
    Dictionary dictionary =
        addScript(dictionary(), FileTypes.FileType.SSM_M_TYPE.getTypeName(),
            "note",
            "if (note == null) { return true; } else { return note != \"script_error_here\";}",
            "Note field cannot be 'script_error_here'");

    status("admin", "Adding Script restriction #2 to OPENED dictionary");
    dictionary =
        addScript(dictionary, FileTypes.FileType.SSM_M_TYPE.getTypeName(),
            "note",
            "if (note == null) { return true; } else { return note.indexOf('_') == -1; }",
            "Note field cannot contain the underscore(_) character");

    status("admin", "Updating to new dictionary with script restrictions");
    updateDictionary(
        dictionaryToString(dictionary), SECOND_DICTIONARY_VERSION, NO_CONTENT.getStatusCode());
  }

  private void adminUpdatesRelease() throws Exception {
    updateRelease(NEXT_RELEASE);
    checkRelease(
        NEXT_RELEASE_NAME,
        SECOND_DICTIONARY_VERSION,
        OPENED,
        hasSubmisisonStates(NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED,
            NOT_VALIDATED));
  }

  private void createInitialRelease() throws Exception {
    val response = put(client, RELEASES_ENDPOINT, INITIAL_RELEASE);
    assertEquals(OK.getStatusCode(), response.getStatus());

    val release = asRelease(response);
    assertEquals(INITITAL_RELEASE_NAME, release.getName());
  }

  private void addProjects() throws IOException {
    status("admin", "Adding project 1...");
    val response1 = post(client, PROJECTS_ENDPOINT, PROJECT1);
    assertEquals(CREATED.getStatusCode(), response1.getStatus());

    status("admin", "Adding project 2...");
    val response2 = post(client, PROJECTS_ENDPOINT, PROJECT2);
    assertEquals(CREATED.getStatusCode(), response2.getStatus());

    status("admin", "Adding project 3...");
    val response3 = post(client, PROJECTS_ENDPOINT, PROJECT3);
    assertEquals(CREATED.getStatusCode(), response3.getStatus());

    status("admin", "Adding project 4...");
    val response4 = post(client, PROJECTS_ENDPOINT, PROJECT4);
    assertEquals(CREATED.getStatusCode(), response4.getStatus());

    status("admin", "Adding project 5...");
    val response5 = post(client, PROJECTS_ENDPOINT, PROJECT5);
    assertEquals(CREATED.getStatusCode(), response5.getStatus());

    status("admin", "Adding project 6...");
    val response6 = post(client, PROJECTS_ENDPOINT, PROJECT6);
    assertEquals(CREATED.getStatusCode(), response6.getStatus());

    status("admin", "Adding project 7...");
    val response7 = post(client, PROJECTS_ENDPOINT, PROJECT7);
    assertEquals(CREATED.getStatusCode(), response7.getStatus());
  }

  private void enqueueProjects(String projectsToEnqueue, Status expectedStatus) throws Exception {
    status("user", "Getting queued projects...");
    Response response = get(client, QUEUE_ENDPOINT);
    String queued = asString(response);
    status("user", "Received queued projects: {}", queued);

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals("[]", queued);

    status("user", "Enqueuing projects...");
    response = post(client, QUEUE_ENDPOINT, projectsToEnqueue);
    assertEquals(expectedStatus.getStatusCode(), response.getStatus());
    if (expectedStatus != NO_CONTENT) {
      JsonNode expected = $("{code:'" + INVALID_STATE.getFrontEndString() + "',parameters:['" + VALID + "']}");
      JsonNode actual = $(response);
      assertEquals(expected + " != " + actual, expected, actual);
    }
  }

  @SuppressWarnings("unused")
  private void cancelValidation(String projectKey, Status expectedStatus) throws Exception {
    val response = delete(client, VALIDATION_ENDPOINT + "/" + projectKey);
    assertEquals(expectedStatus.getStatusCode(), response.getStatus());
  }

  private void addInvalidCodeList() throws IOException {
    // Ensure codelist is present
    status("admin", "Getting code lists...");
    Response response = get(client, CODELISTS_ENDPOINT);
    assertEquals(OK.getStatusCode(), response.getStatus());
    val codeListName = "appendix_B10";
    assertTrue(asString(response).contains(codeListName));

    // Attempt to add it again
    status("admin", "Adding invalid code list...");
    response = post(client, CODELISTS_ENDPOINT, "[{name:'someName'},{name:'" + codeListName + "'}]");
    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  private void addValidCodeLists() throws IOException {
    status("admin", "Adding valid code lists...");
    val response = post(client, CODELISTS_ENDPOINT, "[{name:'someName'},{name:'someNewName'}]");
    assertEquals(CREATED.getStatusCode(), response.getStatus());
  }

  private void addCodeListTerms() throws Exception {
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates(VALID, INVALID, INVALID, INVALID, INVALID, VALID, INVALID));

    // TODO: Get codelist dynamically
    status("admin", "Adding code list terms...");
    val codeListName = "GLOBAL.0.platform.v1/terms";
    val response = post(client, CODELISTS_ENDPOINT + "/" + codeListName,
        "[{code:'1000',value:'new value 1'},{code:'10001',value:'new value 2'}]");
    assertEquals(CREATED.getStatusCode(), response.getStatus());

    // Only the INVALID ones should have been reset (DCC-851)
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates(VALID, NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED, VALID, NOT_VALIDATED));
  }

  private void releaseInitialRelease() {
    // Attempts releasing (expect failure)
    status("admin", "Releasing attempt 1 (should fail)...");
    Response response = post(client, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus()); // no signed off projects

    // Sign off
    status("user", "Signing off project {}", PROJECT_TO_SIGN_OFF);
    response = post(client, SIGNOFF_ENDPOINT, PROJECT_TO_SIGN_OFF);
    assertEquals(OK.getStatusCode(), response.getStatus());

    // Attempt releasing again
    status("admin", "Releasing attempt 2 (should pass)...");
    response = post(client, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(asString(response), OK.getStatusCode(), response.getStatus());

    // Attempt releasing one too many times
    status("admin", "Releasing attempt 3 (should fail)...");
    response = post(client, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  private void updateDictionary(String dictionary, String dictionaryVersion, int expectedStatus)
      throws Exception {
    val updatedSecondDictionary = dictionary.replace("Unique identifier for the donor",
        "Unique identifier for the donor (update" + 1 + ")");
    assertTrue(dictionary, dictionary.equals(updatedSecondDictionary) == false);

    status("admin", "Updating dictionary...");
    val response = put(client, DICTIONARIES_ENDPOINT + "/" + dictionaryVersion, updatedSecondDictionary);
    assertEquals(response.getHeaders().toString(), expectedStatus, response.getStatus());
  }

  private void updateRelease(String updatedRelease) throws Exception {
    val response = put(client, UPDATE_RELEASE_ENDPOINT, updatedRelease);
    assertEquals(OK.getStatusCode(), response.getStatus());

    val release = asRelease(response);
    assertEquals(NEXT_RELEASE_NAME, release.getName());
  }

  /**
   * TODO: improve this to make expectedSubmissionStates a map rather than a list (else order of project names could
   * break the test)
   * <p>
   * TODO: reuse checkValidatedSubmission() to while at it (since it's smarter and can poll)
   */
  private void checkRelease(String releaseName, String dictionaryVersion, ReleaseState expectedReleaseState,
      List<SubmissionState> expectedSubmissionStates) {
    status("admin", "Getting release '{}'...", releaseName);
    val response = get(client, RELEASES_ENDPOINT + "/" + releaseName);
    assertEquals(OK.getStatusCode(), response.getStatus());

    val releaseView = asReleaseView(response);
    assertNotNull(releaseView);

    assertEquals(dictionaryVersion, releaseView.getDictionaryVersion());
    assertEquals(expectedReleaseState, releaseView.getState());
    assertEquals(ImmutableList.<String> of(), releaseView.getQueue());
    assertEquals(expectedSubmissionStates.size(), releaseView.getSubmissions().size());

    int i = 0;
    for (val submission : releaseView.getSubmissions()) {
      assertEquals(submission.getProjectKey(), expectedSubmissionStates.get(i++), submission.getState());
    }
  }

  private void checkValidations() {
    status("user", "Getting release 1...");
    val response = get(client, INITIAL_RELEASE_ENDPOINT);
    assertEquals(OK.getStatusCode(), response.getStatus());

    status("user", "Checking validated submission 1...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT1_KEY, VALID);

    status("user", "Checking validated submission 2...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT2_KEY, INVALID);

    status("user", "Checking validated submission 3...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT3_KEY, INVALID);

    status("user", "Checking validated submission 4...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT4_KEY, INVALID);

    status("user", "Checking validated submission 5...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT5_KEY, INVALID);

    status("user", "Checking validated submission 6...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT6_KEY, VALID);

    status("user", "Checking validated submission 7...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT7_KEY, INVALID);

    // TODO: Do the negation of following for the projects the failed primary validation

    // Project 1
    assertEmptyFile(fileSystem,
        DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/donor.txt.bz2.internal" + FILE_NAME_SEPARATOR + "errors.json");
    assertEmptyFile(fileSystem,
        DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/specimen.txt.gz.internal" + FILE_NAME_SEPARATOR + "errors.json");
  }

  private void checkValidatedSubmission(String release, String project, SubmissionState expectedSubmissionState) {
    DetailedSubmission detailedSubmission;
    do {
      sleepUninterruptibly(2, SECONDS);

      status("user", "Polling submission status...");
      val response = get(client, INITIAL_RELEASE_SUBMISSIONS_ENDPOINT + "/" + project);
      detailedSubmission = asDetailedSubmission(response);
      status("user", "Received submission status: {}", detailedSubmission);

      assertEquals(OK.getStatusCode(), response.getStatus());
    } while (detailedSubmission.getState() == QUEUED || detailedSubmission.getState() == VALIDATING);

    assertEquals(project, expectedSubmissionState, detailedSubmission.getState());
  }

  @SuppressWarnings("unused")
  private void awaitValidatingState(String project) {
    DetailedSubmission detailedSubmission;
    do {
      sleepUninterruptibly(2, SECONDS);

      status("user", "Polling submission validation status...");
      val response = get(client, INITIAL_RELEASE_SUBMISSIONS_ENDPOINT + "/" + project);
      detailedSubmission = asDetailedSubmission(response);
      status("user", "Received submission validation status: {}", detailedSubmission);

      assertEquals(OK.getStatusCode(), response.getStatus());
    } while (detailedSubmission.getState() == QUEUED);
  }

  private static List<SubmissionState> hasSubmisisonStates(SubmissionState... states) {
    return newArrayList(states);
  }

  private void dumpTestDictionary() {
    writeDictionary(
        dictionary(),
        "/tmp/dictionary_submission_integration_test.json");
  }

  private static void banner(String message) {
    // Bruce:
    log.info(repeat("-", 100));
    log.info(message);
    log.info(repeat("-", 100));
  }

  private static void status(String phase, String message, Object... args) {
    log.info("[" + phase + "] " + message, args);
  }

}
