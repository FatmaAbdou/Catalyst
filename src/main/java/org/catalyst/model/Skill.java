package org.catalyst.model;

import java.util.Objects;
import java.util.Set;

/** A catalog skill with a volunteer's proficiency level, when assigned to a profile. */
public final class Skill {
    public static final String ANY_LEVEL = "ANY";
    public static final Set<String> LEVELS =
            Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT");

    private final int id;
    private final String name;
    private final String level;

    public Skill(int id, String name, String level) {
        if (id < 1) {
            throw new IllegalArgumentException("A skill must have a saved ID.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A skill must have a name.");
        }
        if (!ANY_LEVEL.equals(level) && !LEVELS.contains(level)) {
            throw new IllegalArgumentException("Choose a valid skill level.");
        }
        this.id = id;
        this.name = name;
        this.level = level;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String level() {
        return level;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Skill skill)) {
            return false;
        }
        return id == skill.id
                && name.equals(skill.name)
                && level.equals(skill.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, level);
    }

    @Override
    public String toString() {
        return ANY_LEVEL.equals(level) ? name : name + " (" + level + ")";
    }
}
