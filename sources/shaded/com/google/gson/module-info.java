/*
 * Descripteur de module de la librairie gson embarquée (miroir de
 * META-INF/versions/9/module-info.class du JAR). Il est placé dans l'arbre de la librairie et non à la
 * racine de sources/ : javac cherche un module-info.java à la RACINE du sourcepath, et sa présence y ferait
 * basculer la compilation ciblée du pom.xml en mode module ("module not found: com.google.gson").
 * Ce fichier n'est jamais compilé par le build (liste <includes> du maven-compiler-plugin).
 */
/* synthetic */ module com.google.gson {
    /* static phase */ requires java.sql;
    /* static phase */ requires jdk.unsupported;

    exports com.google.gson;
    exports com.google.gson.annotations;
    exports com.google.gson.reflect;
    exports com.google.gson.stream;

}

