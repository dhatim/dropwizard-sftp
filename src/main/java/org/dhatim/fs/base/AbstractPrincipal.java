package org.dhatim.fs.base;

import java.security.Principal;
import java.util.Objects;

public abstract class AbstractPrincipal implements Principal {

    private final String name;

    protected AbstractPrincipal(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        Principal that = (Principal) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }

    @Override
    public String toString() {
        return getName();
    }
    
}
