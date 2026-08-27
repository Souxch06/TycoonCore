/*
 * Decompiled with CFR 0.152.
 */
package com.cryptomorin.xseries.reflection.jvm;

import com.cryptomorin.xseries.reflection.jvm.NamedMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.ClassHandle;

public abstract class FlaggedNamedMemberHandle
extends NamedMemberHandle {
    protected Class<?> returnType;
    protected boolean isStatic;

    protected FlaggedNamedMemberHandle(ClassHandle classHandle) {
        super(classHandle);
    }

    public FlaggedNamedMemberHandle asStatic() {
        this.isStatic = true;
        return this;
    }

    public FlaggedNamedMemberHandle returns(Class<?> clazz) {
        this.returnType = clazz;
        return this;
    }

    public FlaggedNamedMemberHandle returns(ClassHandle classHandle) {
        this.returnType = (Class)classHandle.unreflect();
        return this;
    }
}

