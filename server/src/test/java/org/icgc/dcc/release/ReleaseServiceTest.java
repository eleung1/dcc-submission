package org.icgc.dcc.release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;

import org.icgc.dcc.core.model.BaseEntity;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class ReleaseServiceTest {

  private Datastore datastore;

  private Dictionary dictionary;

  private DictionaryService dictionaryService;

  private ReleaseService releaseService;

  private Release release;

  final private String testDbName = "testDb";

  @Before
  public void setUp() {
    try {

      // use local host as test MongoDB for now
      Mongo mongo = new Mongo("localhost");
      Morphia morphia = new Morphia();
      morphia.map(BaseEntity.class);
      datastore = morphia.createDatastore(mongo, testDbName);

      // Clear out the test database before each test
      datastore.delete(datastore.createQuery(Release.class));
      datastore.delete(datastore.createQuery(Project.class));

      // Set up a minimal test case
      dictionary = new Dictionary();
      dictionary.setVersion("foo");

      dictionaryService = new DictionaryService(morphia, datastore);
      dictionaryService.add(dictionary);

      release = new Release();

      Project project1 = new Project("Project One", "p1");
      Project project2 = new Project("Project Two", "p2");
      Project project3 = new Project("Project Three", "p3");
      Submission validSubmission = new Submission();
      validSubmission.setState(SubmissionState.VALID);
      validSubmission.setProjectKey(project1.getKey());

      Submission notValidatedSubmission = new Submission();
      notValidatedSubmission.setState(SubmissionState.NOT_VALIDATED);
      notValidatedSubmission.setProjectKey(project2.getKey());

      Submission queuedSubmission = new Submission();
      queuedSubmission.setState(SubmissionState.QUEUED);
      queuedSubmission.setProjectKey(project3.getKey());

      release.getSubmissions().add(validSubmission);
      release.getSubmissions().add(notValidatedSubmission);
      release.getSubmissions().add(queuedSubmission);

      release.setDictionary(dictionary);

      // Create the releaseService and populate it with the initial release
      releaseService = new ReleaseService(morphia, datastore);
      releaseService.createInitialRelease(release);
    } catch(UnknownHostException e) {
      e.printStackTrace();

      fail(e.getMessage());
    } catch(MongoException e) {
      e.printStackTrace();

      fail(e.getMessage());
    } catch(NullPointerException e) {
      e.printStackTrace();

      fail(e.getMessage());
    }
  }

  @After
  public void tearDown() {
    datastore.delete(dictionary);
  }

  @Test
  public void test_getNextRelease_isCorrectRelease() {
    assertEquals(release.getId(), releaseService.getNextRelease().getRelease().getId());
    Release newRelease = addNewRelease();
    assertEquals(newRelease.getId(), releaseService.getNextRelease().getRelease().getId());
  }

  @Test
  public void test_getCompletedReleases_isCorrectSize() {
    assertEquals(0, releaseService.getCompletedReleases().size());
    addNewRelease();
    assertEquals(1, releaseService.getCompletedReleases().size());
  }

  @Test
  public void test_list_isCorrectSize() {
    assertEquals(1, releaseService.list().size());
    addNewRelease();
    assertEquals(2, releaseService.list().size());
  }

  @Test
  public void test_queueNotValidatedSubmission() {
    // System.out.println(release.getProjectKeys());
    // System.out.println(releaseService.getQueued());
    // releaseService.queue(release.getProjectKeys());
  }

  private Release addNewRelease() {
    Release newRelease = new Release();
    releaseService.getNextRelease().release(newRelease);
    return newRelease;
  }
}
