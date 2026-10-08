# Reglas ProGuard para Drinix GCS
-keep class com.drinix.gcs.data.model.** { *; }
-keepattributes Signature, *Annotation*
-dontwarn javax.annotation.**
