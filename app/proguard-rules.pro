# Suhood Phase 1 - minimal ProGuard rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
