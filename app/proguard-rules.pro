# Reglas ProGuard/R8 para el build de release.
# Documentación: http://developer.android.com/guide/developing/tools/proguard.html

# Mantener números de línea para poder desofuscar los stack traces (Crashlytics)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── WorkManager ─────────────────────────────────────────────────────────────
# WorkManager instancia los Workers por reflexión a partir del nombre de clase.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.ahorragas.PriceAlertWorker
-keep class com.example.ahorragas.SyncWorker

# ── Modelos ─────────────────────────────────────────────────────────────────
# Seguro barato: aunque el parseo es manual (no Gson), mantenemos los modelos
# intactos para no arriesgar el paso por Intents/Parcelable ni futuros cambios.
-keep class com.example.ahorragas.model.** { *; }

# Parcelable (además de lo que ya cubre proguard-android-optimize)
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ── Room ────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# ── osmdroid / MPAndroidChart (usan recursos y algo de reflexión) ────────────
-dontwarn org.osmdroid.**
-dontwarn com.github.mikephil.charting.**
