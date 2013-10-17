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
package org.icgc.dcc.submission.web.resource;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.web.util.Authorizations.isOmnipotentUser;
import static org.icgc.dcc.submission.web.util.Responses.UNPROCESSABLE_ENTITY;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import lombok.val;

import org.icgc.dcc.submission.dictionary.DictionaryService;
import org.icgc.dcc.submission.dictionary.DictionaryValidator;
import org.icgc.dcc.submission.dictionary.DictionaryValidator.DictionaryObservations;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.DictionaryState;
import org.icgc.dcc.submission.web.model.ServerErrorCode;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.icgc.dcc.submission.web.util.ResponseTimestamper;
import org.icgc.dcc.submission.web.util.Responses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("dictionaries")
public class DictionaryResource {

  /**
   * Custom HTTP headers for validation.
   */
  private static final String VALIDATION_ERROR_HEADER = "X-Validation-Error";
  private static final String VALIDATION_WARNING_HEADER = "X-Validation-Warning";

  private static final Logger log = LoggerFactory.getLogger(DictionaryResource.class);

  @Inject
  private DictionaryService dictionaries;

  private final boolean validate = true;

  @GET
  public Response getDictionaries() {
    /* no authorization check necessary */

    log.debug("Getting dictionaries");
    List<Dictionary> dictionaries = this.dictionaries.list();
    if (dictionaries == null) {
      dictionaries = newArrayList();
    }

    return Response.ok(dictionaries).build();
  }

  @GET
  @Path("{version}")
  public Response getDictionary(@PathParam("version")
  String version) {
    /* no authorization check necessary */

    log.debug("Getting dictionary: {}", version);
    Dictionary dict = this.dictionaries.getFromVersion(version);
    if (dict == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, version)).build();
    }
    return ResponseTimestamper.ok(dict).build();
  }

  /**
   * See {@link DictionaryService#addDictionary(Dictionary)} for details.
   */
  @POST
  public Response addDictionary(@Valid
  Dictionary dict, @Context
  SecurityContext securityContext) {
    log.info("Adding dictionary: {}", dict == null ? null : dict.getVersion());
    if (isOmnipotentUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    val observations = validateDictionary(dict);
    if (observations.hasErrors()) {
      StringBuilder errors = new StringBuilder("The request entity had the following errors:\n");
      for (val error : observations.getErrors()) {
        errors.append("  * ").append(error).append('\n');
      }

      return Response.status(UNPROCESSABLE_ENTITY)
          .header(VALIDATION_ERROR_HEADER, errors)
          .build();
    }

    this.dictionaries.addDictionary(dict);

    val url = UriBuilder.fromResource(DictionaryResource.class).path(dict.getVersion()).build();

    if (observations.hasWarnings()) {
      StringBuilder warnings = new StringBuilder("Created, but request entity had the following warnings:\n");
      for (val error : observations.getErrors()) {
        warnings.append("  * ").append(error).append('\n');
      }

      return Response
          .created(url)
          .header(VALIDATION_WARNING_HEADER, warnings)
          .build();
    }

    return Response.created(url).build();
  }

  @PUT
  @Path("{version}")
  public Response updateDictionary(@PathParam("version")
  String version, @Valid
  Dictionary newDictionary,
      @Context
      Request req, @Context
      SecurityContext securityContext) {
    checkArgument(version != null);
    checkArgument(newDictionary != null);
    checkArgument(newDictionary.getVersion() != null);

    log.info("Updating dictionary: {} with {}", version, newDictionary.getVersion());
    if (isOmnipotentUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    Dictionary oldDictionary = this.dictionaries.getFromVersion(version);
    if (oldDictionary == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, version)).build();
    } else if (oldDictionary.getState() != DictionaryState.OPENED) { // TODO: move check to dictionaries.update()
                                                                     // instead
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.RESOURCE_CLOSED, version)).build();
    } else if (newDictionary.getVersion() == null) {
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.MISSING_REQUIRED_DATA, "dictionary version")).build();
    } else if (newDictionary.getVersion().equals(version) == false) {
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NAME_MISMATCH, version, newDictionary.getVersion()))
          .build();
    }

    ResponseTimestamper.evaluate(req, oldDictionary);

    val observations = validateDictionary(newDictionary);
    if (observations.hasErrors()) {
      StringBuilder errors = new StringBuilder("The request entity had the following errors:\n");
      for (val error : observations.getErrors()) {
        errors.append("  * ").append(error).append('\n');
      }

      return Response.status(UNPROCESSABLE_ENTITY)
          .header(VALIDATION_ERROR_HEADER, errors)
          .build();
    }

    this.dictionaries.update(newDictionary);

    if (observations.hasWarnings()) {
      StringBuilder warnings = new StringBuilder("Created, but request entity had the following warnings:\n");
      for (val error : observations.getErrors()) {
        warnings.append("  * ").append(error).append('\n');
      }

      return Response
          .status(Status.NO_CONTENT)
          .header(VALIDATION_WARNING_HEADER, warnings)
          .build();
    }

    return Response
        .status(Status.NO_CONTENT)
        .build(); // http://stackoverflow.com/questions/797834/should-a-restful-put-operation-return-something
  }

  private DictionaryObservations validateDictionary(Dictionary dict) {
    if (validate) {
      DictionaryValidator validator = new DictionaryValidator(dict, dictionaries.listCodeList());
      return validator.validate();
    } else {
      val empty = Collections.<DictionaryValidator.DictionaryObservation> emptySet();
      return new DictionaryObservations(empty, empty);
    }
  }

}
