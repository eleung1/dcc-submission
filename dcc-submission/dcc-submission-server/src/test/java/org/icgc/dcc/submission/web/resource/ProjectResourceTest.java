package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.glassfish.grizzly.http.util.Header.Authorization;
import static org.glassfish.jersey.internal.util.Base64.encodeAsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;

import javax.ws.rs.core.Application;

import lombok.val;

import org.icgc.dcc.submission.core.AbstractDccModule;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.repository.ProjectRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Module;

@RunWith(MockitoJUnitRunner.class)
public class ProjectResourceTest extends ResourceTest {

  private final String UNAUTH_USER = "nobody";
  private final String AUTH_ALLOWED_USER = "richard";
  private final String AUTH_NOT_ALLOWED_USER = "ricardo";
  private final String ADMIN_USER = "admin";

  private static final String AUTH_HEADER = Authorization.toString();

  private String getAuthValue(String username) {
    return "X-DCC-Auth " + encodeAsString(username + ":" + username + "spasswd");
  }

  private ProjectRepository projectRepository;

  private Project projectOne;

  private Project projectTwo;

  @Override
  protected Application configure() {

    projectOne = new Project("PRJ1", "Project One");
    projectOne.setUsers(Sets.newHashSet(AUTH_ALLOWED_USER));
    projectOne.setGroups(Sets.newHashSet(AUTH_ALLOWED_USER));

    projectTwo = new Project("PRJ2", "Project Two");
    projectTwo.setUsers(Sets.newHashSet(ADMIN_USER));
    projectTwo.setGroups(Sets.newHashSet(ADMIN_USER));

    projectRepository = mock(ProjectRepository.class);
    when(projectRepository.findProject(projectOne.getKey())).thenReturn(projectOne);
    when(projectRepository.findProject(projectTwo.getKey())).thenReturn(projectTwo);
    when(projectRepository.findProjects()).thenReturn(Sets.newHashSet(projectOne, projectTwo));

    return super.configure();
  }

  @Override
  protected Collection<? extends Module> configureModules() {
    return ImmutableList.of(new AbstractDccModule() {

      @Override
      protected void configure() {
        bind(ProjectRepository.class).toInstance(projectRepository);
      }

    });
  }

  @Test
  public void testGetProjectsWhenNotAuthorized() {
    val reponse =
        target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(UNAUTH_USER)).get();
    verifyZeroInteractions(projectRepository);
    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("");
  }

  @Test
  public void testGetProjectsWhenAuthorized() {
    val reponse =
        target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(AUTH_ALLOWED_USER)).get();
    verify(projectRepository, atLeast(1)).findProjects();
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class))
        .isEqualTo("[{\"key\":\"PRJ1\",\"name\":\"Project One\"}]");
  }

  @Test
  public void testGetProjectsWhenAuthorizedAsAdmin() {
    val reponse = target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(ADMIN_USER)).get();
    verify(projectRepository, atLeast(1)).findProjects();
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class))
        .isEqualTo("[{\"key\":\"PRJ2\",\"name\":\"Project Two\"},{\"key\":\"PRJ1\",\"name\":\"Project One\"}]");
  }

  @Test
  public void testGetProjectWhenNotAuthorized() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(UNAUTH_USER)).get();
    verifyZeroInteractions(projectRepository);
    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("");
  }

  @Test
  public void testGetProjectWhenAuthorizedWithoutAccess() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(AUTH_NOT_ALLOWED_USER))
            .get();
    verify(projectRepository, never()).findProject(any(String.class));
    assertThat(reponse.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"code\":\"NoSuchEntity\",\"parameters\":[\"PRJ1\"]}");
  }

  @Test
  public void testGetProjectWhenAuthorizedWithAccess() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(AUTH_ALLOWED_USER)).get();
    verify(projectRepository).findProject(projectOne.getKey());
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
  }

  @Test
  public void testGetProjectWhenAuthorizedAsAdmin() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(ADMIN_USER)).get();
    verify(projectRepository).findProject(projectOne.getKey());
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
  }

  @Test
  public void testGetProjectWhenAuthorizedDoesNotExist() {
    val reponse =
        target().path("projects/DNE").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(ADMIN_USER)).get();
    assertThat(reponse.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"code\":\"NoSuchEntity\",\"parameters\":[\"DNE\"]}");
  }
}
