import { Injectable } from '@angular/core';
import { RxStomp, RxStompConfig } from "@stomp/rx-stomp";

@Injectable({
  providedIn: 'root'
})
export class MyRxStompService extends RxStomp {

  // https://stomp-js.github.io/guide/rx-stomp/rx-stomp-with-angular.html

  myRxStompConfig: RxStompConfig = {
    brokerURL: `ws://${window.location.hostname}:9000/mbdo-factory-scada`,
    connectHeaders: {},
    reconnectDelay: 200,
    // Will log diagnostics on console
    // It can be quite verbose, not recommended in production
    // Skip this key to stop logging to console
    debug: (msg: string): void => {
      console.log(new Date(), msg);
    }
  };

  constructor() {
    super();
    this.configure(this.myRxStompConfig);
    this.activate();
  }
}
