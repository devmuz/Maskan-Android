# Firestore deserializes these via reflection (@DocumentId/@PropertyName/no-arg
# constructors) with no direct call site R8 can see — keep them intact.
-keepclassmembers class com.maskan.mobileapp.data.model.** {
    <init>(...);
    <fields>;
}
-keep class com.maskan.mobileapp.data.model.** { *; }

# Every Firebase SDK (auth/firestore/storage/functions/crashlytics/messaging/
# remote-config) registers itself via a ComponentRegistrar, discovered at
# runtime with Class.forName(...).newInstance() from a manifest meta-data
# entry. firebase-components' own consumer rule only does
# "-keep class * implements ComponentRegistrar", which under AGP 9's default
# R8 strict-full-mode keeps the class but strips its no-arg constructor,
# silently breaking the reflective newInstance() call for all Firebase
# SDKs at once (surfaces as e.g. "FirebaseCrashlytics component is not
# present"). Keep the constructor explicitly.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>(...);
}
