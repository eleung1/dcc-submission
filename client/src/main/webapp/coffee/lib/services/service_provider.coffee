define (require) ->
  _ = require 'underscore'
  Chaplin = require 'chaplin'
  utils = require 'lib/utils'

  'use strict'

  class ServiceProvider

    # Mixin a Subscriber
    _(@prototype).extend Chaplin.Subscriber

    loading: false

    constructor: ->
      console.debug 'ServiceProvider#constructor'

      # Mixin a Deferred
      _(this).extend $.Deferred()

      utils.deferMethods
        deferred: this
        methods: ['triggerLogin', 'getLoginStatus']
        onDeferral: @load

    # Disposal
    # --------

    disposed: false

    dispose: ->
      #console.debug 'ServiceProvider#dispose'
      return if @disposed

      # Unbind handlers of global events
      @unsubscribeAllEvents()

      # Finished
      @disposed = true

      # You're frozen when your heart’s not open
      Object.freeze? this

  ###

    Standard methods and their signatures:

    load: ->
      # Load a script like this:
      utils.loadLib 'http://example.org/foo.js', @loadHandler, @reject

    loadHandler: =>
      # Init the library, then resolve
      ServiceProviderLibrary.init(foo: 'bar')
      @resolve()

    isLoaded: ->
      # Return a Boolean
      Boolean window.ServiceProviderLibrary and ServiceProviderLibrary.login

    # Trigger login popup
    triggerLogin: (loginContext) ->
      callback = _(@loginHandler).bind(this, loginContext)
      ServiceProviderLibrary.login callback

    # Callback for the login popup
    loginHandler: (loginContext, response) =>

      if response
        # Publish successful login
        mediator.publish 'loginSuccessful', {provider: this, loginContext}

        # Publish the session
        mediator.publish 'serviceProviderSession',
          provider: this
          userId: response.userId
          accessToken: response.accessToken
          # etc.

      else
        mediator.publish 'loginFail', {provider: this, loginContext}

    getLoginStatus: (callback = @loginStatusHandler, force = false) ->
      ServiceProviderLibrary.getLoginStatus callback, force

    loginStatusHandler: (response) =>
      return unless response
      mediator.publish 'serviceProviderSession',
        provider: this
        userId: response.userId
        accessToken: response.accessToken
        # etc.

  ###