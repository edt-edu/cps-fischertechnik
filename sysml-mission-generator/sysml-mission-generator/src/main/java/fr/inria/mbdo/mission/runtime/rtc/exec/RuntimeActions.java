package fr.inria.mbdo.mission.runtime.rtc.exec;

public final class RuntimeActions {
  private RuntimeActions() {
  }

  public static RuntimeAction noop() {
    return event -> {
    };
  }
}
