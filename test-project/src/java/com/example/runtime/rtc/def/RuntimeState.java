package com.example.runtime.rtc.def;

import java.util.List;
import com.example.runtime.rtc.exec.RuntimeAction;

public record RuntimeState(
    String name,
    List<RuntimeTransition> transitions,
    List<RuntimeAction> entryActions,
    List<RuntimeAction> exitActions,
    boolean isFinal
) {
}
