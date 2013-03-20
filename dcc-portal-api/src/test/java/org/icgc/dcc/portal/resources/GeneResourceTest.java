/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.ResourceTest;
import org.icgc.dcc.portal.repositories.BaseRepository;
import org.icgc.dcc.portal.repositories.GeneProjectRepository;
import org.icgc.dcc.portal.repositories.GeneRepository;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.results.FindAllResults;
import org.icgc.dcc.portal.results.FindResults;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GeneResourceTest extends ResourceTest {

  private final static String RESOURCE = "/genes";

  @Mock
  private BaseRepository baseStore;

  @Mock
  private GeneRepository store;

  @Mock
  private GeneProjectRepository gp;

  @Mock
  private RequestSearchQuery requestSearchQuery;

  @Mock
  private FindResults findResults;

  @Mock
  private FindAllResults findAllResults;

  @Mock
  private JsonNode jsonNode;

  @Override
  protected final void setUpResources() {
    addResource(new GeneResource(store, gp));
  }

  @Test
  public final void test_Search() {
    ClientResponse response = client().resource(RESOURCE).get(ClientResponse.class);

    verify(store).findAll(any(RequestSearchQuery.class));
    assertThat(response.getStatus()).isEqualTo(ClientResponse.Status.OK.getStatusCode());
  }

  @Test
  public final void test_Get_404() {
    ClientResponse response = client().resource(RESOURCE).path("UNKNOWN_ID").get(ClientResponse.class);

    verify(store).find(anyString());
    assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
  }
}
