"""
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
"""

define [
  'handlebars'
  'chaplin'
  'lib/utils'
], (Handlebars, Chaplin, utils) ->
  'use strict'

  # View helpers (Handlebars in this case)
  # --------------------------------------

  # Shortcut to the mediator
  mediator = Chaplin.mediator

  # Add application-specific Handlebars helpers
  # -------------------------------------------

  # Choose block by user login status
  Handlebars.registerHelper 'if_logged_in', (options) ->
    console.log mediator.user
    if mediator.user
      options.fn(this)
    else
      options.inverse(this)

  Handlebars.registerHelper 'is_admin', (options) ->
    "admin" in mediator.user.get "roles"

  Handlebars.registerHelper 'if_admin', (options) ->
    if is_admin
      options.fn(this)
    else
      options.inverse(this)
  
  Handlebars.registerHelper 'if_opened', (state, options) ->
    if state is 'OPENED'
      options.fn(this)
    else
      options.inverse(this)

  # Return a Unreleased if no release date
  Handlebars.registerHelper 'release_action', (state) ->
    return false unless state is 'OPENED'
    new Handlebars.SafeString """
    <button
      class="btn btn-primary btn-small"
      id="complete-release-popup-button"
      data-toggle="modal"
      href="#complete-release-popup">
      Complete
    </button>
    """

  # Return a Unreleased if no release date
  Handlebars.registerHelper 'release_date', (date) ->
    return new Handlebars.SafeString '<em>Unreleased</em>' unless date
    Handlebars.helpers.date.call(this, date)
    
  # Make a date out of epoc
  Handlebars.registerHelper 'date', (date) ->
    return false unless date
    new Handlebars.SafeString moment(date).format("YYYY-MM-DD")

  null