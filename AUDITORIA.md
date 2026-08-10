# Auditoría técnica — AhorraGas

> Revisión de cara a: (1) publicación en Google Play, (2) migración completa a MVVM, (3) eliminación de código deprecado, (4) eficiencia y (5) testing.
> Fecha: 2026-08-09 · Rama: `main` · 84 archivos Java · `minSdk 24` / `target 36` / Java 17.

## Resumen ejecutivo

El proyecto está **notablemente bien** para un TFG: capa de datos separada en repositorios + data sources, Room como fuente de verdad, WorkManager para sync, catálogo de versiones (`libs.versions.toml`), soporte multi-idioma (ca/eu/gl) y hasta un test unitario real de `GasolineraSorter`. El diez está justificado.

Ahora bien, para llevarlo a producción hay **3 bloqueantes reales de Play Store**, bastante código deprecado puntual, un `MainActivity` de 1257 líneas que concentra casi toda la lógica (0 % MVVM en la parte principal), y cobertura de test casi nula. Nada es grave a nivel de rediseño: es trabajo de saneamiento y refactor incremental.

**Prioridad recomendada:** primero los bloqueantes de Play (rápidos), luego deprecados + eficiencia (medio), y MVVM + tests como esfuerzo mayor y continuado.

---

## 🔴 Bloqueantes para Google Play

| # | Problema | Detalle | Fix |
|---|----------|---------|-----|
| B1 | **`applicationId = "com.ahorragas.app"`** | Play **rechaza** cualquier paquete que empiece por `com.example`. Es el fallo más importante. | Renombrar a un dominio propio, p. ej. `com.ahorragas.app` o `es.ahorragas`. Cambia `applicationId`, `namespace` y el paquete de las clases (refactor de IDE). |
| B2 | **Release sin ofuscar ni firmar** | `isMinifyEnabled = false`, sin `shrinkResources`, sin `signingConfig`. Se sube un APK debug-like, pesado y sin R8. | Activar R8 (`isMinifyEnabled = true`, `isShrinkResources = true`) y crear un `signingConfig` de release con keystore fuera del repo. |
| B3 | **`versionCode = 1` / `versionName = "1.0"`** | Correcto para la primera subida, pero hay que tener el flujo de incremento claro. No bloquea la primera vez. | Definir esquema de versionado antes de la primera release. |

Relacionado (no bloquea pero afecta a compilar en limpio):
- **`google-services.json` está en `.gitignore`** (bien por seguridad), pero como el plugin `google-services` es obligatorio, **un compañero que clone el repo no puede compilar** sin ese archivo. Documenta en el README cómo obtenerlo, o valora si Firebase (Analytics + Crashlytics) es realmente necesario en v1.

---

## 🟠 APIs deprecadas / correctitud

Buenas noticias primero: **no** hay `AsyncTask`, `onBackPressed()`, `startActivityForResult`, ni `ProgressDialog`. Ya se usa `ActivityResultLauncher` y `WindowInsetsCompat`. Lo pendiente:

1. **`getParcelableExtra(String)` deprecado (API 33+)** — `StationDetailActivity.java:48`.
   ```java
   // Reemplazar por la variante con tipo:
   IntentCompat.getParcelableExtra(getIntent(), EXTRA_GASOLINERA, Gasolinera.class);
   ```
2. **`androidx.lifecycle.Transformations` deprecado** — `PromotionsViewModel.java:5`. Migrar a `Transformations.map { }` de la extensión / o `MediatorLiveData` según versión.
3. **`new Thread(...)` a pelo** — `MainActivity.java:708`, `FavoritesActivity.java:315`, `ElectrolineraRepository.java:43`. Hilos sin control de ciclo de vida; el de `ElectrolineraRepository` persiste en background sin gestión. Unificar en un `ExecutorService` (idealmente uno compartido en la capa de datos).
4. **`Scanner(is).useDelimiter("\\A")` + parseo de JSON a mano por `indexOf`** — `MainActivity.searchLocalidad()` (líneas ~940-970). Frágil (rompe si Nominatim cambia formato) y usa `HttpURLConnection` crudo cuando ya tienes Retrofit/OkHttp en el proyecto. Migrar a un `NominatimApiService` con Gson.
5. **`AppCompatDelegate.MODE_NIGHT_NO` forzado** — `BaseActivity.java:25`. Fuerza modo claro, **pero existe `res/values-night/themes.xml`**: ese recurso está muerto. O soportas modo oscuro de verdad (quitar la línea) o eliminas `values-night`.

---

## 🟡 Arquitectura / MVVM

Estado actual: **híbrido**. La capa de datos (repos + data sources + Room + mappers) está bien separada, pero la capa de presentación **no es MVVM salvo un caso** (`PromotionsViewModel`). Las Activities/Fragments hacen todo: I/O en background, lógica de negocio, formateo y pintado.

- **`MainActivity` = God Activity (1257 líneas).** Mezcla: permisos, geolocalización + filtro/smoothing de ubicación, geocoding HTTP, threading manual (`ExecutorService` + `new Thread` + `Handler`), construcción de bitmaps de markers, filtros por marca/combustible/descuentos, diálogos creados por código (el diálogo de vehículo son ~170 líneas de `LinearLayout` a mano). Todo ese estado (`allGasolineras`, `searchLocation`, `selectedFuel`…) debería vivir en un `MapViewModel`.
- **Estado mutable compartido y frágil.** El patrón `onResume()` compara "versiones" (`lastDiscountsVersion`, `lastFavoritesVersion`, `lastRadiusKm`) para decidir si recargar. Es un sustituto manual de lo que `LiveData`/`StateFlow` haría solo. Muy propenso a bugs sutiles.
- **Repos singleton con `getInstance`.** Funciona, pero al migrar a `ViewModel` conviene inyectar el repo (aunque sea con un `ViewModelProvider.Factory` manual; no hace falta Hilt para aprobar la store).
- **`PromotionsViewModel` mezcla responsabilidades.** Además del estado expone `BRAND_ALIASES` y `filterByBrand()`, que es lógica de dominio; debería estar en un `UseCase`/helper o en el repo.

**Plan MVVM sugerido (incremental, sin reescribir todo de golpe):**
1. Empezar por las pantallas de lista (`PriceListActivity`, `DistanceListActivity`, `FavoritesActivity`): son más pequeñas y ya tienen `ExecutorService` propio → extraer un `ViewModel` con `LiveData<List<Gasolinera>>` es directo.
2. Migrar `MainActivity` por bloques: sacar primero geolocalización a un `LocationViewModel`, luego el estado del mapa a `MapViewModel`.
3. Extraer los diálogos por código a XML + `DialogFragment`.
4. Considerar **View Binding** (`buildFeatures { viewBinding = true }`): hay 16 archivos con `findViewById`; ViewBinding elimina el boilerplate y los cast inseguros.

---

## 🟢 Eficiencia / rendimiento

- **`showStationsOnMap()` recrea todos los markers y limpia caché en cada cambio** (`MarkerBitmapFactory.clearCache()` se llama muy a menudo, incluso en `onResume`). Los bitmaps se regeneran constantemente. Revisar la política de caché: cachear por (marca, precio, nivel) y no invalidar todo por un simple resume.
- **`GasolineraAdapter` usa `DiffUtil` en un método pero `notifyDataSetChanged()` en otro** (`adapter/GasolineraAdapter.java:61`). Unificar hacia `ListAdapter` + `DiffUtil` para animaciones y menos redibujado.
- **Doble copia de la base de datos en git (~16 MB).** Hay `ahorragas.db` en la raíz (7.6 MB, **no se usa**, el `createFromAsset` carga `app/src/main/assets/ahorragas.db`) y la de assets (8.8 MB). La de la raíz es basura que infla el repo y el historial → borrarla. La de assets infla el APK; valora comprimirla o descargar el dataset en primer arranque.
- **`fallbackToDestructiveMigration()`** (`AppDatabase.java:37`): en producción, cualquier cambio de esquema **borra los datos del usuario** (favoritos, alertas si estuvieran en Room). Definir migraciones reales antes de publicar.
- **Sin `Application` class.** La config de osmdroid se hace en `MainActivity.onCreate`. Mover init (osmdroid, Firebase si se queda) a una `Application` es más limpio y evita repetir.
- 77 colores hardcodeados (`0xFF…` / `Color.parseColor`) en Java → mover a `colors.xml` para theming y modo oscuro coherente.

---

## 🧪 Testing

- **Unit tests:** existe **uno real y bueno** (`ExampleUnitTest` cubre `GasolineraSorter`: orden por distancia y niveles de precio). No es cierto que no haya nada, pero la cobertura es ~1 clase.
- **Instrumented:** `ExampleInstrumentedTest` es el stub por defecto (solo verifica el package name).
- **Objetivo realista para store + nota:** testear la lógica pura, que es donde más valor hay y es fácil en JVM (sin emulador):
  - `GasolineraSorter` (ampliar), `GeoUtils`/`GeoValidation` (distancias, validación de coords), `NumberUtils`, `RadiusUtils`, `EstacionMapper`, `GasolineraJsonParser`, y `PromotionsViewModel.filterByBrand()` (los alias de marca son muy testeables).
  - Renombra los `Example*Test` a nombres con sentido.
  - Para ViewModels: `InstantTaskExecutorRule` + tests de `LiveData`.

---

## 🧹 Higiene / mantenibilidad

- **Paquete `legacy/` = código muerto.** `legacy.Station`, `StationsRepository`, `legacy.network.*` (6 clases) **no se referencian desde ningún sitio** de `main`. Borrar.
- **Permiso duplicado** en `AndroidManifest.xml`: `POST_NOTIFICATIONS` aparece dos veces.
- **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`**: Play penaliza/pide justificación por este permiso. Si no es imprescindible para las alertas de precio, quítalo.
- **`WRITE_EXTERNAL_STORAGE` (maxSdk 28)**: correcto que esté acotado, pero verifica que osmdroid no lo necesita en runtime en versiones nuevas (usa cache interna).
- **Logs (`Log.*`) en ~9 archivos**: envolver en `if (BuildConfig.DEBUG)` o usar un wrapper, para no filtrar info en release.
- **README** es un scratchpad de notas de la API del ministerio, no un README de proyecto. Para publicar/enseñar conviene uno real (qué es, stack, cómo compilar, de dónde salen los datos, licencia de OSM/Nominatim).
- **Atribución OSM/Nominatim**: usar tiles de OSM y geocoding de Nominatim exige atribución visible y respetar sus *usage policies* (Nominatim: máx. 1 req/s, User-Agent identificable — ya pones `getPackageName()`, bien). Requisito legal para la store.

---

## Roadmap sugerido

**Fase 1 — Bloqueantes Play (rápido, alto impacto)**
1. Renombrar `applicationId`/`namespace`/paquete fuera de `com.example` (B1).
2. Activar R8 + `shrinkResources` + `signingConfig` de release (B2).
3. Borrar `ahorragas.db` de la raíz y el paquete `legacy/`; quitar permiso duplicado.
4. README real + atribución OSM/Nominatim.

**Fase 2 — Deprecados y eficiencia (medio)**
5. `IntentCompat.getParcelableExtra`, unificar hilos en `ExecutorService`, Nominatim vía Retrofit.
6. Colores a `colors.xml`; decidir modo oscuro (o borrar `values-night`).
7. `ListAdapter`+`DiffUtil`; revisar caché de markers; migraciones de Room.

**Fase 3 — MVVM + tests (esfuerzo continuado)**
8. ViewBinding + extraer ViewModels empezando por las listas.
9. Descomponer `MainActivity` en `MapViewModel`/`LocationViewModel`.
10. Ampliar tests de la capa de dominio y de ViewModels.

---

*Ningún punto exige rediseñar la app: es saneamiento + refactor incremental. Los bloqueantes de Play son el 20 % del trabajo con el 80 % del valor para "subirlo".*
