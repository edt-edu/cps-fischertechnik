package fr.inria.mbdo.mission.runtime.rtc.exec;

import java.util.Optional;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

public interface RuntimeDefinition {
  RuntimeState initialState();

  Optional<RuntimeTransition> findTransition(RuntimeState from, Event event);

  Optional<RuntimeTransition> findCompletion(RuntimeState from);

  boolean isDeferred(RuntimeState from, Event event);
}
