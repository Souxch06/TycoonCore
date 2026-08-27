/*
 * Decompiled with CFR 0.152.
 */
package kotlin.jvm.internal;

import java.io.Serializable;
import kotlin.SinceKotlin;
import kotlin.jvm.internal.FunctionReference;
import kotlin.reflect.KFunction;

@SinceKotlin(version="1.7")
public class FunInterfaceConstructorReference
extends FunctionReference
implements Serializable {
    private final Class funInterface;

    public FunInterfaceConstructorReference(Class clazz) {
        super(1);
        this.funInterface = clazz;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof FunInterfaceConstructorReference)) {
            return false;
        }
        FunInterfaceConstructorReference funInterfaceConstructorReference = (FunInterfaceConstructorReference)object;
        return this.funInterface.equals(funInterfaceConstructorReference.funInterface);
    }

    @Override
    public int hashCode() {
        return this.funInterface.hashCode();
    }

    @Override
    public String toString() {
        return "fun interface " + this.funInterface.getName();
    }

    @Override
    protected KFunction getReflected() {
        throw new UnsupportedOperationException("Functional interface constructor does not support reflection");
    }
}

