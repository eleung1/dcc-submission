package org.icgc.dcc;/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.icgc.dcc.dao.GeneDao;
import org.icgc.dcc.dao.impl.GeneDaoImpl;

import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.mongodb.Mongo;

public class DataPortalModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Mongo.class).toInstance(mongo());
    bind(Client.class).toInstance(esClient());
    bind(GeneDao.class).to(GeneDaoImpl.class);
  }

  private Mongo mongo() {
    try {
      return new Mongo();
    } catch(UnknownHostException e) {
      Throwables.propagate(e);
    }

    return null;
  }

  private Client esClient() {
    // TODO: inject "hcn50.res.oicr.on.ca"
    return new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
  }

}
