# Ledger release keep rules.
#
# Most libraries (Compose, Hilt/Dagger, Room, OkHttp, DataStore, Haze) ship their
# own consumer ProGuard rules, so the app-specific risk is kotlinx.serialization
# (used for the AI provider request/response DTOs) and enums that are looked up or
# persisted by name. Rules below are the canonical kotlinx.serialization set plus a
# defensive enum keep. Verified by a signed release build + on-device smoke test.

# ---- kotlinx.serialization (canonical rules from the library README) ----
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Serializers for classes with named companion objects are retrieved via reflection.
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepnames class kotlinx.serialization.SealedClassSerializer

# Keep the generated serializers for this app's serializable models.
-keep,includedescriptorclasses class com.sherif.ledger.**$$serializer { *; }
-keepclassmembers class com.sherif.ledger.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Transitive compile-only annotations ----
# Tink (via androidx.security:security-crypto) references errorprone annotations that
# aren't on the runtime classpath. They're compile-only; safe to ignore.
-dontwarn com.google.errorprone.annotations.**

# ---- Enums ----
# Some enums (e.g. LedgerThemeType, TransactionType) are persisted or looked up by
# name via valueOf(); keep their synthetic values()/valueOf() members.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
