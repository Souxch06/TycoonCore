package com.cryptomorin.xseries.abstractions;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
interface Material {
    public String name();

    public boolean isSupported();

    default public Material or(Material other) {
        return this.isSupported() ? this : other;
    }

    public boolean equals(Object var1);

    public int hashCode();
}

