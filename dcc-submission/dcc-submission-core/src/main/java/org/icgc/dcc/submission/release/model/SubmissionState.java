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
package org.icgc.dcc.submission.release.model;

import static lombok.AccessLevel.PRIVATE;
import lombok.Delegate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.submission.core.state.State;
import org.icgc.dcc.submission.core.state.States;

/**
 * Canonical set of allowed behavioral states for a submission.
 * <p>
 * Uses a delegate to allow for:
 * <ul>
 * <li>the Bridge Pattern (abstraction = SubmissionState, implementer = State)</li>
 * <li>source partitioning the implementation</li>
 * <li>polymorphism of states</li>
 * <li>semantic hierarchical states / substates</li>
 * </ul>
 * 
 * @see http://en.wikipedia.org/wiki/Bridge_pattern
 * @see http://www.javacodegeeks.com/2011/02/state-pattern-domain-driven-design.html
 */
@RequiredArgsConstructor(access = PRIVATE)
public enum SubmissionState implements State {

  // In (typical) sequence order:
  NOT_VALIDATED(States.NOT_VALIDATED),
  QUEUED(States.QUEUED),
  VALIDATING(States.VALIDATING),
  ERROR(States.ERROR),
  INVALID(States.INVALID),
  VALID(States.VALID),
  SIGNED_OFF(States.SIGNED_OFF);

  /**
   * Delegate
   */
  @NonNull
  @Delegate
  private final State delegate;

  public static SubmissionState getDefaultState() {
    return NOT_VALIDATED;
  }

}
