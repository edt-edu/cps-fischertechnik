package TestMachineSystem {
    private import ScalarValues::*;
    private import Common::*;
    private import Common::Messages::*;

    package TestMachineCommands {
        enum def TestCommandKind {
            A;
            B;
        }

        action def A {}

        action def B {}
    }

    package TestMachineMessages {
        private import TestMachineSystem::TestMachineCommands::*;

        item def EventMessage1 :> EventMessage;
        item def EventMessage2 :> EventMessage;

        item def CommandSuccessEventMessage :> EventMessage;
    }

    package TestMachine {
        private import TestMachineSystem::TestMachineCommands::*;

        part def TestMachineMachine specializes Machine {
            perform action actionA : A;
            perform action actionB : B;

            attribute currentCommand : TestCommandKind;
            attribute attA : Integer;
            attribute attB : Integer;
        }
    }
}
