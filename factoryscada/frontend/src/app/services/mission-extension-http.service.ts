import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  IMissionExtensionConfiguration,
  IMissionCommandResponse,
  IMissionLogs,
} from '../models/i-mission-extension';

@Injectable({
  providedIn: 'root',
})
export class MissionExtensionHttpService {
  private readonly baseUrl = `${window.location.protocol}//${window.location.hostname}:9000/api/mission-extension`;

  constructor(private readonly http: HttpClient) { }

  getConfiguration(): Observable<IMissionExtensionConfiguration> {
    return this.http.get<IMissionExtensionConfiguration>(`${this.baseUrl}/configuration`);
  }

  startMission(missionName: string): Observable<IMissionCommandResponse> {
    return this.http.post<IMissionCommandResponse>(
      `${this.baseUrl}/${encodeURIComponent(missionName)}/start`,
      {}
    );
  }

  stopMission(missionName: string): Observable<IMissionCommandResponse> {
    return this.http.post<IMissionCommandResponse>(
      `${this.baseUrl}/${encodeURIComponent(missionName)}/stop`,
      {}
    );
  }

  stopActiveMission(): Observable<IMissionCommandResponse> {
    return this.http.post<IMissionCommandResponse>(`${this.baseUrl}/stop-active`, {});
  }

  getLogs(): Observable<IMissionLogs> {
    return this.http.get<IMissionLogs>(`${this.baseUrl}/logs`);
  }
}
