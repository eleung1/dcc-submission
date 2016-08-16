/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.MISSING_REQUIRED_DATA;
import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.icgc.dcc.submission.core.model.Status;
import org.icgc.dcc.submission.http.jersey.PATCH;
import org.icgc.dcc.submission.service.SystemService;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.icgc.dcc.submission.web.util.Responses;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint for systemService related operations.
 * 
 * @see http://stackoverflow.com/questions/6433480/restful-actions-services-that-dont-correspond-to-an-entity
 * @see http://stackoverflow.com/questions/8660003/restful-design-of-a-resource-with-binary-states
 * @see http://stackoverflow.com/questions/8914852/rest-interface-design-for-machine-control
 * @see http://stackoverflow.com/questions/6776198/rest-model-state-transitions
 * @see http
 * ://stackoverflow.com/questions/5591348/how-to-implement-a-restful-resource-for-a-state-machine-or-finite-automata
 */
@Slf4j
@Path("/")
@Consumes("application/json")
public class SystemResource {

  @Autowired
  private SystemService systemService;

  @GET
  @Path("/systems")
  public Response getStatus(

      @Context SecurityContext securityContext

  ) {
    log.info("Getting status...");
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    Status status = systemService.getStatus();

    return Response.ok(status).build();
  }

  @PATCH
  @Path("/systems")
  public Response patch(

      @Context SecurityContext securityContext,

      JsonNode state

  ) {
    log.info("Setting SFTP state to {}...", state);
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    JsonNode active = state.path("active");
    if (active.isMissingNode()) {
      return status(BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(MISSING_REQUIRED_DATA))
          .build();
    }

    if (active.asBoolean()) {
      systemService.enable();
    } else {
      systemService.disable();
    }

    Status status = systemService.getStatus();

    return ok(status).build();
  }

}
