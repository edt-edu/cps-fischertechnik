package fr.inria.mbdo.mission.generators;

import lombok.Getter;
import org.eclipse.syson.sysml.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class SymbolIndex {
    private final Map<String, PartDefinition> parts = new HashMap<>();
    private final Map<String, EnumerationDefinition> enums = new HashMap<>();
    private final Map<String, ItemDefinition> items = new HashMap<>();
    private final Map<String, StateDefinition> states = new HashMap<>();
    private final Map<String, ActionDefinition> actions = new HashMap<>();

    public Optional<PartDefinition> resolvePart(String key) {
        return parts.containsKey(key) ? Optional.of(parts.get(key)) : Optional.empty();
    }

    public Optional<EnumerationDefinition> resolveEnum(String key) {
        return enums.containsKey(key) ? Optional.of(enums.get(key)) : Optional.empty();
    }

    public Optional<ItemDefinition> resolveItem(String key) {
        return items.containsKey(key) ? Optional.of(items.get(key)) : Optional.empty();
    }

    public Optional<StateDefinition> resolveState(String key) {
        return states.containsKey(key) ? Optional.of(states.get(key)) : Optional.empty();
    }

    public Optional<ActionDefinition> resolveAction(String key) {
        return actions.containsKey(key) ? Optional.of(actions.get(key)) : Optional.empty();
    }

    public Optional<Definition> resolveAny(String key) {
        if (parts.containsKey(key)) {
            return Optional.of(parts.get(key));
        } else if (enums.containsKey(key)) {
            return Optional.of(enums.get(key));
        } else if (items.containsKey(key)) {
            return Optional.of(items.get(key));
        } else if (states.containsKey(key)) {
            return Optional.of(states.get(key));
        } else if (actions.containsKey(key)) {
            return Optional.of(actions.get(key));
        }
        return Optional.empty();
    }

    public String toString() {
        return parts.entrySet().stream().map(entry -> "Part: " + entry.getKey() + " -> " + entry.getValue().getName())
                .collect(Collectors.joining("\n"))
                + "\n"
                + enums.entrySet().stream()
                        .map(entry -> "Enum: " + entry.getKey() + " -> " + entry.getValue().getName())
                        .collect(Collectors.joining("\n"))
                + "\n"
                + items.entrySet().stream()
                        .map(entry -> "Item: " + entry.getKey() + " -> " + entry.getValue().getName())
                        .collect(Collectors.joining("\n"))
                + "\n"
                + states.entrySet().stream()
                        .map(entry -> "State: " + entry.getKey() + " -> " + entry.getValue().getName())
                        .collect(Collectors.joining("\n"))
                + "\n"
                + actions.entrySet().stream()
                        .map(entry -> "Action: " + entry.getKey() + " -> " + entry.getValue().getName())
                        .collect(Collectors.joining("\n"));
    }

    public static class Builder {
        SymbolIndex symbolIndex;

        public Builder() {
            this.symbolIndex = new SymbolIndex();
        }

        public SymbolIndex build() {
            return symbolIndex;
        }

        public void addPart(PartDefinition def) {
            putUnique(symbolIndex.parts, def.getQualifiedName(), def, "part");
        }

        public void addEnum(EnumerationDefinition def) {
            putUnique(symbolIndex.enums, def.getQualifiedName(), def, "enum");
        }

        public void addItem(ItemDefinition def) {
            putUnique(symbolIndex.items, def.getQualifiedName(), def, "item");
        }

        public void addState(StateDefinition def) {
            putUnique(symbolIndex.states, def.getQualifiedName(), def, "state");
        }

        public void addAction(ActionDefinition def) {
            putUnique(symbolIndex.actions, def.getQualifiedName(), def, "action");
        }

        private <T extends Definition> void putUnique(Map<String, T> map, String key, T value, String kind) {
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("Missing qualified name for " + kind);
            }
            if (map.containsKey(key)) {
                throw new IllegalStateException("Duplicate " + kind + " definition: " + key);
            }
            map.put(key, value);
        }
    }
}
