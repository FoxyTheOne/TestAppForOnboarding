# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ---------- Gson (DataStore) ----------
# Классы хранения в DataStore помечены @Keep и @SerializedName (см. StoredModels.kt).
# Правило ниже – страховка для любых будущих классов с @SerializedName: R8 не должен
# переименовывать или удалять такие поля, иначе уже сохранённые у пользователей данные перестанут читаться.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile