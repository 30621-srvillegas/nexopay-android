-keepattributes Signature, *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.uc1.nexopay.data.remote.dto.** { *; }

-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
