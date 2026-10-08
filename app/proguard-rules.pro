# ============================================================
# KnitTrack — ProGuard/R8 правила для release-сборки.
# ============================================================
# Минификация включена: isMinifyEnabled = true.
# Файл подключён в app/build.gradle.kts (строка 22).
#
# Что защищаем:
# 1. Gson — сериализация доменных сущностей в JSON
#    (DataExporterImpl / DataImporterImpl).
# 2. Attributes — сигнатуры и runtime-аннотации (нужны Gson).
# 3. Kotlin coroutines — служебные поля.
# 4. Room, Hilt — их consumer-rules применяются автоматически.
#
# Доменные сущности Project, Session, Category защищены
# аннотацией @Keep прямо в коде.
# ============================================================

# ------------------------------------------------------------
# Attributes — метаданные классов и полей
# ------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ------------------------------------------------------------
# Gson — инфраструктура
# ------------------------------------------------------------
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ------------------------------------------------------------
# Gson — доменные модели (страховка поверх @Keep)
# ------------------------------------------------------------
-keep class com.knittrac.app.domain.entity.Project { *; }
-keep class com.knittrac.app.domain.entity.Session { *; }
-keep class com.knittrac.app.domain.entity.Category { *; }
-keep class com.knittrac.app.core.common.SyncStatus { *; }

# ------------------------------------------------------------
# Kotlin coroutines
# ------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------
# Общие dontwarn
# ------------------------------------------------------------
-dontwarn javax.annotation.**
-dontwarn kotlin.**
