package fr.inria.mbdo.mission.runtime.rtc.exec;

public final class RuntimeGuards {
    private RuntimeGuards() {
    }

    public static RuntimeGuard always() {
        return event -> true;
    }
}
