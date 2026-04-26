# OrbitX

OrbitX is a playful Android gravity sandbox where you can bend a miniature solar system and watch the consequences unfold.

## Playable v0.1.0

- Spawn planets, moons, dwarf planets, asteroids, comets, probes, and custom bodies.
- Move bodies directly or drag their velocity handles to reshape orbits.
- Pause, resume, speed up time, reset the system, and toggle trails or prediction paths.
- Watch impacts merge worlds, fragment smaller objects, or eject bodies from the system.

## Build

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Notes

The simulation is intentionally scaled for fun and readability rather than scientific ephemeris precision. Positions use AU-like units, masses are solar-mass relative, and radii are enlarged for touch play.
