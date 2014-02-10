"""
* Copyright 2012(c) The Ontario Institute for Cancer Research.
* All rights reserved.
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
"""


mediator = require 'mediator'
View = require 'views/base/view'
Model = require 'models/base/model'
NextRelease = require 'models/next_release'
template = require 'views/templates/submission/validate_submission'
utils = require 'lib/utils'

module.exports = class ValidateSubmissionView extends View
  template: template
  template = null

  container: '#page-container'
  containerMethod: 'append'
  autoRender: true
  tagName: 'div'
  className: "modal hide fade"
  id: 'validate-submission-popup'

  selectNone: ->
    @dataTypeTable.fnClearTable()
    for f, idx in @dataTypes
      f.selected = false unless f.dataType == "CLINICAL_CORE_TYPE"
      @dataTypeTable.fnAddData @dataTypes[idx]

  
  selectAll: ->
    @dataTypeTable.fnClearTable()
    for f, idx in @dataTypes
      f.selected = true
      @dataTypeTable.fnAddData @dataTypes[idx]

  toggleFeature: (e) ->
    feature = $(e.currentTarget).data('feature-type')
    idx = _.pluck(@dataTypes, 'dataType').indexOf(feature)
    @dataTypes[idx].selected = not @dataTypes[idx].selected

    # For some reason update seem to adjust td width,
    # just clear and rebuild as there are only a handful of datatypes
    @dataTypeTable.fnClearTable()
    for f, idx in @dataTypes
      @dataTypeTable.fnAddData @dataTypes[idx]


  initialize: ->
    console.debug "ValidateSubmissionView#initialize", @options
    datatype = @options.datatype
    @model = new Model @options.submission.getAttributes()
    @model.set({email: mediator.user.get("email")}, {silent: true})

    @dataTypeTable = null

    @report = @model.get "report"

    # Only need shallow copy
    @dataTypes = _.clone( @report.dataTypeReports )
    console.log @dataTypes

    # Default to all selected
    @dataTypes.forEach (d)->
      d.selected = true


    # Make clinical related files go first
    @dataTypes = _.sortBy @dataTypes, (dataType)->
      switch dataType.dataType
        when "CLINICAL_CORE_TYPE"
          return 0
        when "CLINICAL_OPTIONAL_TYPE"
          return 1
        else
          return 10

    # Pre-filter if datatype is provided
    if datatype
      @dataTypes.forEach (f)->
        f.selected = false unless (f.dataType == "CLINICAL_CORE_TYPE" or f.dataType == datatype)

    release = new NextRelease()
    release.fetch
      success: (data) =>
        @model.set 'queue', data.get('queue').length

    super

    @modelBind 'change', @render
    @delegate 'click', '#validate-submission-button', @validateSubmission
    @delegate 'click', '#toggle-feature', @toggleFeature
    @delegate 'click', '#feature-all', @selectAll
    @delegate 'click', '#feature-clear', @selectNone

  render: ->
    super

    # Create a feature type selection table
    aoColumns = [
      {
         sTitle: "Data Types To Valdiate"
         mData: (source) ->
           displayName = utils.translateDataType(source.dataType)
           if source.dataType != "CLINICAL_CORE_TYPE"
             if source.selected == false
               """
               <span id="toggle-feature"
                  style="cursor:pointer"
                  data-feature-type="#{source.dataType}">
                  <i class="icon icon-check-empty"></i> #{displayName}
               </span>
               """
             else
               """
               <span id="toggle-feature"
                  style="cursor:pointer"
                  data-feature-type="#{source.dataType}">
                  <i class="icon icon-check"></i> #{displayName}
               </span>
               """
           else
             """
             <span data-feature-type="#{source.dataType}">
               <i class="icon icon-check"></i> #{displayName}
             </span>
             """
      }
      {
        sTitle: "State"
        mData: (source) =>
          state = source.dataTypeState
          if state
            ui_state = state.replace("_", " ")
            switch state
              when "INVALID"
                return "<span class='invalid'>"+ui_state+"</span>"
              when "VALID"
                return "<span class='valid'>"+ui_state+"</span>"
              else
                return "<span>#{state}</span>"
          else
            return "<span>Not Validated</span>"
      }
    ]

    @dataTypeTable = $("#validate-file-types").dataTable
      bDestroy: true
      bPaginate: false
      bFilter: false
      bSort:false
      bInfo: false
      sAjaxSource: ""
      sAjaxDataProp: ""
      fnRowCallback: (nRow, aData, iDisplayIndex, iDisplayIndexFull) ->
        if aData.dataType == "CLINICAL_CORE_TYPE"
          $(nRow).css {'color': '#999', 'font-style': 'italic'}
      aoColumns: aoColumns
      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @dataTypes

     
  validateSubmission: (e) ->
    #console.debug "ValidateSubmissionView#completeRelease", @model
    emails = @.$("#emails")
    alert = @.$('#email-error')
    val = @.$("#emails").val()
    mediator.user.set "email", val

    if not val or not val.match /.+@.+\..+/i
      if alert.length
        alert.text("You must enter at least one valid email
           address before submitting submission for Validation!")
      else
        emails
          .before("<div id='email-error' class='error'>You must enter at least
            one valid email address before submitting submission for
            Validation!</div>")
      return


    nextRelease = new NextRelease()

    # Grab the selected datatypes
    dataTypesToValidate = _.filter @dataTypes, (f) -> f.selected == true
    dataTypeParams = _.pluck dataTypesToValidate, 'dataType'

    nextRelease.queue [{
      key: @options.submission.get("projectKey")
      emails: @.$('#emails').val().split(',')
      dataTypes: dataTypeParams
    }],
    success: =>
      @$el.modal 'hide'
      mediator.publish "validateSubmission"
      mediator.publish "notify", "Submission for Project "+
        "#{@model.get('projectName')} has been queued for Validation."

