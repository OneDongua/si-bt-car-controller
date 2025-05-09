# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontoptimize
-dontpreverify
-verbose
-printmapping mapping.txt
-optimizations !code/simplification/arithmetic,!field/,!class/merging/
-keepattributes Annotation
-keep class com.baidu.mobstat.**{*;}

-keep class android.app.**{
public <methods>;
public <fields>;
}
-keep class android.content.**{*;}
-keep class com.android.cglib.proxy.**{
public <methods>;
public <fields>;
}
-keep interface com.android.cglib.proxy.**{
public <methods>;
public <fields>;
}

-keep class android.widget.**{
public <methods>;
public <fields>;
}

-keep class com.androlua.**{
public <methods>;
public <fields>;
}

-keep class com.nirenr.**{
public <methods>;
public <fields>;
}

-keep interface com.androlua.**{
public <methods>;
public <fields>;
}
-keep interface com.nirenr.**{
public <methods>;
public <fields>;
}
-keep class com.luajava.**{
public <methods>;
public <fields>;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep final class com.android.cglib.dx.* {
public <methods>;
public <fields>;
}
-keep enum com.android.cglib.dx.* {*;}
-keepattributes Signature
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.preference.Preference
-keep public class * extends android.preference.PreferenceActivity
-keep public class * extends android.accessibilityservice.AccessibilityService

