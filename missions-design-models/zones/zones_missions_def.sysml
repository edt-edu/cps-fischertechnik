package ZoneMissions {
    private import ZonesSystem::ZonesMessages::*;

    state def ZoneMissionCBNominal {
        doc
        /* Mission for managing zone acquirement on Conveyor Belt between VGR1 and VGR2 for nominal mission */

        in vacuumGripper1 : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in vacuumGripper2 : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;

        action def sendAcquireResponseEventMessage {
            in request : AcquireRequestEventMessage;

            send new AcquireResponseEventMessage() to request.sender;
        }

        entry;
        then IdleFree;

        state IdleFree;
        accept AcquireRequestEventMessage do action sendAcquireResponseEventMessage then IdleBusy;

        state IdleBusy;
        accept ReleaseRequestEventMessage then IdleFree;
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
