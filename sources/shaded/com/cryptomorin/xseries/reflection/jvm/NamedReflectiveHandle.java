package com.cryptomorin.xseries.reflection.jvm;

import java.util.Set;
import javax.annotation.Nonnull;

public interface NamedReflectiveHandle {
    @Nonnull
    public Set<String> getPossibleNames();
}

