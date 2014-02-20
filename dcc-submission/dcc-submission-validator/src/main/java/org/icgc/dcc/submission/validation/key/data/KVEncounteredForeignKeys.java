/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.key.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import lombok.RequiredArgsConstructor;

/**
 * TODO: inclusive delegate possible with lombok?
 */
@RequiredArgsConstructor
public final class KVEncounteredForeignKeys {

  private final Set<KVKey> encounteredFks = newHashSet(); // TODO: change to array (optimization)?

  public void addEncounteredForeignKey(KVKey fk) {
    encounteredFks.add(checkNotNull(fk));
  }

  public void addEncounteredForeignKeys(KVEncounteredForeignKeys surjectionEncountered) {
    encounteredFks.addAll(surjectionEncountered.encounteredFks);
  }

  public boolean noneEncountered() {
    return encounteredFks.isEmpty();
  }

  public boolean encountered(KVKey encounteredKeys) {
    return encounteredFks.contains(encounteredKeys);
  }

  public long getSize() {
    return encounteredFks.size();
  }

  @Override
  public String toString() {
    return String.format("KVEncounteredForeignKeys(encounteredFks=%s)", encounteredFks.size());
  }

}
