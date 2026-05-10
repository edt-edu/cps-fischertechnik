import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
    IBetterMissionsConfiguration,
    IMissionCommandResponse,
} from '../models/i-better-missions';

@Injectable({
    providedIn: 'root',
})
export class BetterMissionsHttpService {
    private readonly baseUrl = `${window.location.protocol}//${window.location.hostname}:9000/api/better-missions`;

    constructor(private readonly http: HttpClient) { }

    getConfiguration(): Observable<IBetterMissionsConfiguration> {
        return this.http.get<IBetterMissionsConfiguration>(`${this.baseUrl}/configuration`);
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
}
