# Basic ProGuard rules for Android
# https://developer.android.com/studio/build/shrink-code

# Keep all activities, services, and receivers
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Keep all views and custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep all Parcelable classes
-keep public class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep R (resources) classes
-keep class **.R$* {
    public static final int *;
}

# Keep all classes that implement Serializable
-keepclassmembers class * implements java.io.Serializable {
    private <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}

# Keep all RxJava and RxAndroid classes
-keep class io.reactivex.** { *; }
-keep class rx.** { *; }
-keep interface io.reactivex.** { *; }
-keep interface rx.** { *; }

# Keep RxAndroidBle classes
-keep class com.polidea.rxandroidble3.** { *; }

# Keep EasyPermissions classes
-keep class pub.devrel.easypermissions.** { *; }

# Keep all native method names and the names of their classes
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep all classes and methods with @Keep annotation
-keep @com.android.annotations.Keep class * { *; }

# Keep all classes and methods with @KeepMember annotation
-keepclassmembers class * {
    @com.android.annotations.Keep <methods>;
}

# Keep all classes and methods with @KeepName annotation
-keepnames class * {
    @com.android.annotations.KeepName <methods>;
}

# Keep all WebView related classes
-keep class * extends android.webkit.WebViewClient
-keep class * extends android.webkit.WebView

# Keep all classes that might be accessed via reflection
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keep class com.example.hellodog.** { *; }

# Keep all classes in the Android framework
-keep class android.** { *; }

# Keep all classes in java and javax
-keep class java.** { *; }
-keep class javax.** { *; }

# Keep all enum classes
-keepclassmembers enum * {
    public static **[] values();
}

# Keep all annotations
-keepattributes *Annotation*

# Keep all inner classes
-keepclassmembers class * {
    public <init>(*);
    public static ** newInstance(*);
}

# Keep all synthetic fields
-keepclassmembers class * {
    synthetic <fields>;
}
