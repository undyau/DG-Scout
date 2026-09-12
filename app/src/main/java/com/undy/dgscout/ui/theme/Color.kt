package com.undy.dgscout.ui.theme

import androidx.compose.ui.graphics.Color

// High-contrast "fairway" palette, tuned for reading a phone screen outdoors in
// direct sunlight rather than for indoor screen-brightness aesthetics. Every
// Material3 color-scheme slot is set explicitly below: Material3 components
// such as Card and Dialog read from the newer `surfaceContainer*` and
// `surfaceTint` roles, and any left unset silently fall back to the library's
// baseline purple tokens - which is exactly the low-contrast pastel look this
// palette replaces.

// ---- Light theme ----
val LightPrimary = Color(0xFF1B5E20)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFA8E6A8)
val LightOnPrimaryContainer = Color(0xFF002905)
val LightInversePrimary = Color(0xFF8FDB90)

val LightSecondary = Color(0xFFB5480A)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFDBC7)
val LightOnSecondaryContainer = Color(0xFF3A1200)

val LightBackground = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF101410)
val LightSurface = Color(0xFFEDF1ED)
val LightOnSurface = Color(0xFF101410)
val LightSurfaceVariant = Color(0xFFDCE4DC)
val LightOnSurfaceVariant = Color(0xFF333A33)
val LightSurfaceTint = LightPrimary
val LightInverseSurface = Color(0xFF2B322B)
val LightInverseOnSurface = Color(0xFFECF2EC)

val LightOutline = Color(0xFF5B645B)
val LightOutlineVariant = Color(0xFFC0CAC0)
val LightScrim = Color(0xFF000000)

val LightSurfaceBright = Color(0xFFF7FBF6)
val LightSurfaceDim = Color(0xFFD6DCD5)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF2F6F1)
val LightSurfaceContainer = Color(0xFFEDF1ED)
val LightSurfaceContainerHigh = Color(0xFFE7EBE6)
val LightSurfaceContainerHighest = Color(0xFFE1E5E0)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

// ---- Dark theme ----
val DarkPrimary = Color(0xFF6FDB74)
val DarkOnPrimary = Color(0xFF00390A)
val DarkPrimaryContainer = Color(0xFF2E7D32)
val DarkOnPrimaryContainer = Color(0xFFC9F5CA)
val DarkInversePrimary = Color(0xFF1B5E20)

val DarkSecondary = Color(0xFFFF8A50)
val DarkOnSecondary = Color(0xFF4A1500)
val DarkSecondaryContainer = Color(0xFF5C2100)
val DarkOnSecondaryContainer = Color(0xFFFFDBC7)

val DarkBackground = Color(0xFF0D110D)
val DarkOnBackground = Color(0xFFF3F6F3)
val DarkSurface = Color(0xFF1B211B)
val DarkOnSurface = Color(0xFFF3F6F3)
val DarkSurfaceVariant = Color(0xFF2B332B)
val DarkOnSurfaceVariant = Color(0xFFD3DBD3)
val DarkSurfaceTint = DarkPrimary
val DarkInverseSurface = Color(0xFFE1E5E0)
val DarkInverseOnSurface = Color(0xFF1B211B)

val DarkOutline = Color(0xFF8C968C)
val DarkOutlineVariant = Color(0xFF404940)
val DarkScrim = Color(0xFF000000)

val DarkSurfaceBright = Color(0xFF313931)
val DarkSurfaceDim = Color(0xFF0D110D)
val DarkSurfaceContainerLowest = Color(0xFF080B08)
val DarkSurfaceContainerLow = Color(0xFF151A15)
val DarkSurfaceContainer = Color(0xFF1B211B)
val DarkSurfaceContainerHigh = Color(0xFF252C25)
val DarkSurfaceContainerHighest = Color(0xFF303830)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
