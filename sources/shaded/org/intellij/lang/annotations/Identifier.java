package org.intellij.lang.annotations;

import org.intellij.lang.annotations.Pattern;

@Pattern(value="\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*")
public @interface Identifier {
}

