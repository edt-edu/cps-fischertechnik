package com.example.runtime.rtc.exec;

public final class RuntimeGuards {
  private RuntimeGuards() {
  }

  public static RuntimeGuard always() {
    return event -> true;
  }
}
