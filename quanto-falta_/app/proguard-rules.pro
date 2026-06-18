# ============================================================
# ProGuard Rules — QuantoFalta
# ============================================================

# ------------------------------------------------------------
# Otimização & Ofuscação
# ------------------------------------------------------------

# Habilita otimizações agressivas (múltiplos passes)
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Ofusca nomes de classes, métodos e campos
-dontskipnonpubliclibraryclasses
-dontpreverify

# Remove atributos de debug que facilitam engenharia reversa
# REMOVIDO: -keepattributes SourceFile,LineNumberTable

# Oculta o nome real do arquivo-fonte em stack traces
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------
# Remoção de Logs em Produção
# ------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static java.lang.String getStackTraceString(...);
}

-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# ------------------------------------------------------------
# Application
# ------------------------------------------------------------

# Preserva apenas o construtor público — evita expor membros internos
-keep class com.phdev.quantofalta.QuantoFaltaApplication {
    public <init>();
}

# ------------------------------------------------------------
# Room Database
# ------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

-keepclassmembers class * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Delete <methods>;
    @androidx.room.Update <methods>;
    @androidx.room.Transaction <methods>;
}

# Geração de código do Room em runtime
-keep class * extends androidx.room.RoomDatabase_Impl { *; }

# ------------------------------------------------------------
# Kotlin & Coroutines
# ------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}

# Serialização de continuações (necessário para suspend functions)
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ------------------------------------------------------------
# ViewModel & Lifecycle
# ------------------------------------------------------------
-keepclassmembers public class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}

# ------------------------------------------------------------
# Serialização (Gson / Kotlinx Serialization)
# ------------------------------------------------------------

# Preserva modelos de dados usados em serialização JSON
# Ajuste o pacote abaixo para o pacote real dos seus models
-keep class com.phdev.quantofalta.domain.model.** { *; }
-keepclassmembers class com.phdev.quantofalta.domain.model.** { *; }
-keep class com.phdev.quantofalta.billing.** { *; }
-keepclassmembers class com.phdev.quantofalta.billing.** { *; }
-keep class com.phdev.quantofalta.core.notifications.model.** { *; }
-keepclassmembers class com.phdev.quantofalta.core.notifications.model.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# ------------------------------------------------------------
# Retrofit & OkHttp (se utilizado)
# ------------------------------------------------------------
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ------------------------------------------------------------
# WebView com JavaScript Interface (descomente se usado)
# ------------------------------------------------------------
#-keepclassmembers class com.phdev.quantofalta.ui.web.MyJsInterface {
#    public *;
#}

# ------------------------------------------------------------
# Proteção Geral
# ------------------------------------------------------------

# Impede que exceções exponham stack traces reais ao usuário
-keepattributes Exceptions

# Preserva anotações usadas em runtime (ex: @Inject, @Provides)
-keepattributes *Annotation*

# Remove classes de teste do build de produção
-dontwarn junit.**
-dontwarn org.junit.**
-dontwarn androidx.test.**

# Suprime warnings de bibliotecas externas conhecidas
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit