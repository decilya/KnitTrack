# ============================================================
# KnitTrack — ProGuard/R8 правила для release-сборки.
# ============================================================
# Минификация включена: isMinifyEnabled = true.
# Файл подключён в app/build.gradle.kts (строка 22).
#
# Что защищаем:
# 1. Gson — сериализация DTO в JSON
#    (DataExporterImpl / DataImporterImpl через data/dto).
# 2. Attributes — сигнатуры и runtime-аннотации (нужны Gson).
# 3. Kotlin coroutines — служебные поля.
# 4. Room, Hilt — их consumer-rules применяются автоматически.
#
# DTO-классы ProjectDto, SessionDto, ExportDataDto защищены
# аннотацией @Keep прямо в коде — правила ниже дублируют это
# для независимости от androidx.annotation.
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
# Gson — DTO-модели (страховка поверх @Keep)
# ------------------------------------------------------------
# DTO — то, что реально попадает в JSON-бэкап. R8 не должен
# их переименовывать или удалять, иначе старые бэкапы перестанут
# читаться. Правило покрывает всё подпакеты data.dto на случай
# будущих сущностей (Pattern, Note).
-keep class com.knittrac.app.data.dto.** { *; }

# ------------------------------------------------------------
# Gson — доменные модели (страховка для debug-сериализации)
# ------------------------------------------------------------
# На случай отладки и отладочного Gson.toJson(project) без DTO.
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
