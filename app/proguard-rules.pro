-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep public class * extends androidx.fragment.app.Fragment

-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**