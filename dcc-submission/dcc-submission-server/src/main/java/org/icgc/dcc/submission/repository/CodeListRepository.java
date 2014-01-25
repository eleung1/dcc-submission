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

package org.icgc.dcc.submission.repository;

import java.util.List;

import lombok.NonNull;

import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.QCodeList;
import org.icgc.dcc.submission.dictionary.model.Term;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.inject.Inject;

public class CodeListRepository extends AbstractRepository<CodeList, QCodeList> {

  @Inject
  public CodeListRepository(@NonNull Morphia morphia, @NonNull Datastore datastore) {
    super(morphia, datastore, QCodeList.codeList);
  }

  public List<CodeList> findCodeLists() {
    return list();
  }

  public CodeList findCodeListByName(@NonNull String codeListName) {
    return uniqueResult(_.name.eq(codeListName));
  }

  public void saveCodeLists(List<CodeList> codeLists) {
    save(codeLists);
  }

  public void updateCodeList(String codeListName, CodeList updatedCodeList) {
    update(
        select()
            .filter("name", codeListName),
        updateOperations()
            .set("label", updatedCodeList.getLabel()));
  }

  public void addCodeListTerm(String codeListName, Term newTerm) {
    update(
        select()
            .filter("name", codeListName),
        updateOperations()
            .add("terms", newTerm));
  }

}
