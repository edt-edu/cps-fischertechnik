package ZoneMissions {
    private import Common::*;
    private import ZonesSystem::*;
    private import ZonesSystem::ZonesMessages::*;

    state def ZoneMissionCBNominal {
        doc
        /* Mission for managing zone acquirement on Conveyor Belt between VGR1 and VGR2 for nominal mission */

        // The zone adapter is the channel shared with the grippers: they send their acquire / release requests to
        // it and wait for the acquire response on it (see VacuumGripper missions: "... to zoneCB", "via zoneCB").
        // A request received while the zone is busy is kept, and answered when the zone is released.
        @MainMachineMetadata about zone;
        in zone : ZonesSystem::Zone;

        entry;
        then IdleFree;

        state IdleFree;
        accept AcquireRequestEventMessage via zone
            do send new AcquireResponseEventMessage() to zone
            then IdleBusy;

        state IdleBusy;
        accept ReleaseRequestEventMessage via zone then IdleFree;
        accept AcquireRequestEventMessage via zone then IdleBusyRequested;

        state IdleBusyRequested;
        accept ReleaseRequestEventMessage via zone
            do send new AcquireResponseEventMessage() to zone
            then IdleBusy;
    }

    state def ZoneMissionMPSNominal {
        doc
        /* Mission for managing zone acquirement on the Multi Processing Station between VGR2 and the MPS for nominal mission */

        // The zone adapter is the channel shared with the grippers: they send their acquire / release requests to
        // it and wait for the acquire response on it (see VacuumGripper2NominalMission: "... to zoneMPS", "via zoneMPS").
        // A request received while the zone is busy is kept, and answered when the zone is released.
        @MainMachineMetadata about zone;
        in zone : ZonesSystem::Zone;

        entry;
        then IdleFree;

        state IdleFree;
        accept AcquireRequestEventMessage via zone
            do send new AcquireResponseEventMessage() to zone
            then IdleBusy;

        state IdleBusy;
        accept ReleaseRequestEventMessage via zone then IdleFree;
        accept AcquireRequestEventMessage via zone then IdleBusyRequested;

        state IdleBusyRequested;
        accept ReleaseRequestEventMessage via zone
            do send new AcquireResponseEventMessage() to zone
            then IdleBusy;
    }

    // doc
    // /* Mission for managing zone acquirement on Conveyor Belt between VGR1 and VGR2 for nominal mission */
    // state def ZoneMissionCBNominal {
    // in vacuumGripper1 : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
    // in vacuumGripper2 : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;

    // entry;
    // then IdleFree;

    // state IdleFree;
    // accept AcquireRequestEventMessage via vacuumGripper1
    // do send new AcquireResponseEventMessage() to vacuumGripper1
    // then IdleBusyVGR;
    // accept AcquireRequestEventMessage via vacuumGripper2
    // do send new AcquireResponseEventMessage() to vacuumGripper2
    // then IdleBusyCB;

    // state IdleBusyVGR;
    // accept ReleaseRequestEventMessage via vacuumGripper1 then IdleFree;

    // state IdleBusyCB;
    // accept ReleaseRequestEventMessage via vacuumGripper2 then IdleFree;
    // }
} // package ZoneMissions
