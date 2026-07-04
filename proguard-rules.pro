# ProGuard configuration for Android

# Keep the main application class
-keep public class com.example.hellodog.MainActivity { public <init>(); }

# Keep all classes in the com.example.hellodog package
-keep class com.example.hellodog.** { *; }

# Keep all classes that implement Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Don't warn about missing classes in the default package
-dontwarn **
