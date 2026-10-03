
# kotlinx-serialization: modelo .polar y rutas de navegación
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.polar.app.**$$serializer { *; }
-keepclassmembers class com.polar.app.** { *** Companion; }
-keepclasseswithmembers class com.polar.app.** { kotlinx.serialization.KSerializer serializer(...); }
# kotlinx-serialization: conserva INSTANCE de los objetos @Serializable (regla oficial)
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    public static ** INSTANCE;
}
