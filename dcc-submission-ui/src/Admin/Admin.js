import React, { Component } from 'react';
import {observable} from 'mobx';
import {observer} from 'mobx-react';
import moment from 'moment';
import pluralize from 'pluralize';

import systems from '~/systems';
import ActionButton from '~/common/components/ActionButton/ActionButton';

import ReleaseModel from '~/Release/ReleaseModel';

import { fetchQueue, clearQueue } from '~/services/nextRelease/queue';

import './Admin.css';

@observer
class Admin extends Component {
  @observable nextRelease;
  @observable validationQueue = [];

  @observable errorMessages = {
    lock: '',
    release: '',
  };

  async componentWillMount() {

    systems.fetch();
    this.nextRelease = new ReleaseModel();
    this.nextRelease.fetch({shouldFetchUpcomingRelease: true});

    this.loadValidationQueue();
    console.log('why no load');
  }

  loadValidationQueue = async () => {
    this.validationQueue = await fetchQueue();
    // this.validationQueue = ["project.7","project.6","project.5","project.4","project.3"];
  };

  clearValidationQueue = async () => {
    await clearQueue();
    await this.loadValidationQueue();
  }

  handleClickLockUnlockButton = async () => {
    try {
      if (systems.isReleaseLocked) {
        await systems.unlockRelease();
      } else {
        await systems.lockRelease();
      }
    } catch (e) {
      console.log('caught an error', e.message);
      this.errorMessages.lock = e.message;
    }
  };

  handleClickPerformRelease = async () => {
    try {
      await this.nextRelease.performRelease();
      this.handlePerformReleaseSuccess();
    } catch (e) {
      console.log('caught an error', e.message);
      this.errorMessages.release = e.message;
      this.releaseErrorMessage = e.message;
    }
  };

  handlePerformReleaseSuccess = () => {
    console.log('release successfully performed');
    this.nextRelease.fetch({shouldFetchUpcomingRelease: true});
  }

  render () {
    const lockMessage = systems.isReleaseLocked 
      ? `Click on "Unlock Submissions" to allow uploading and validation on non-admin accounts.`
      : `Click on "Lock Submissions" to prevent uploading and validation on non-admin accounts.`;
    return (
      <div className="Admin container">
          <h1>Admin</h1>

          <header>
            <h2>
              Upcoming Release -
              <span>{this.nextRelease.name}</span>
            </h2>
            <ul className="terms">
              <li>
                <span className="terms__term">Created</span>
                <span className="terms__value">{moment(this.nextRelease.created, 'x').fromNow()}</span>
              </li>
              <li>
                <span className="terms__term">Last Updated</span>
                <span className="terms__value">{moment(this.nextRelease.lastUpdate, 'x').fromNow()}</span>
              </li>
            </ul>
          </header>
          
          <div>
            { (this.errorMessages.lock || this.errorMessages.release) ? (
              <div className="alert alert-danger">
                {this.errorMessages.lock}
                {this.errorMessages.release}
              </div>
            ) : ''}
            <div>{lockMessage}</div>
          </div>
          <button
            type="submit"
            className={`btn ${systems.isReleaseLocked ? 'btn-primary' : 'btn-danger'}`}
            onClick={this.handleClickLockUnlockButton}
          >{systems.isReleaseLocked ? 'Unlock' : 'Lock'}</button>

          <ActionButton
            onClick={() => this.handleClickPerformRelease()}
            className={`btn release-now-btn`}
          >
            <i className="fa fa-rocket"/>
            Release Now
          </ActionButton>

          <h2>Validation</h2>
          {
            this.validationQueue.length
              ? this.validationQueue.map(projectKey => (
                <li key={projectKey}>
                  <span className="label label-default">{projectKey}</span>
                </li>
              ))
              : <div>Validation queue is empty</div>
          }

          <button
            type="submit"
            className={`btn btn-danger`}
            onClick={this.clearValidationQueue}
            disabled={!this.validationQueue.length}
          >
            Clear Queue
          </button>

          <h2>SFTP</h2>
          <ul>
            <li>
              There { pluralize('is', systems.activeSftpSessions) } <strong>{ systems.activeSftpSessions }</strong> active SFTP { pluralize('session', systems.activeSftpSessions) }
              <ul>
                {systems.userSessions.map(user => (
                  <li key={user.userName}>
                    <span className="label label-default sftp-username">{user.userName}</span>
                    <span className="label label-primary sftp-filename">{ user.ioSessionMap.fileTransfer ? user.ioSessionMap.fileTransfer : 'idle' }</span>
                  </li>
                ))}
              </ul>
            </li>
          </ul>

          <h2>Users</h2>
          
      </div>
    );
  }
}

export default Admin;