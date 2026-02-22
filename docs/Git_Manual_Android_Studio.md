# 📘 Manual Definitivo -- Git + GitHub para Android Studio

------------------------------------------------------------------------

# 1️⃣ Configuración Inicial

## Configuración global (una sola vez)

``` shell
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"
git config --global init.defaultBranch main
git config --global color.ui auto
```

------------------------------------------------------------------------

# 2️⃣ Flujo Profesional para Proyectos Android

## Estructura recomendada de ramas

-   **main** → producción estable
-   **develop** (opcional) → integración
-   **feature/**\* → nuevas funcionalidades
-   **fix/**\* → correcciones
-   **hotfix/**\* → errores urgentes en producción

------------------------------------------------------------------------

# 3️⃣ Flujo Real de Trabajo con GitHub (día a día)

## 1. Actualizar main

``` shell
git checkout main
git pull
```

## 2. Crear nueva rama

``` shell
git checkout -b feature/login
```

## 3. Trabajar y commitear

``` shell
git add .
git commit -m "Añade validación de login"
```

## 4. Subir rama al remoto (primera vez)

``` shell
git push -u origin feature/login
```

A partir de aquí ya puedes usar:

``` shell
git push
```

## 5. Crear Pull Request en GitHub

-   Ir al repositorio en GitHub
-   Abrir Pull Request (PR)
-   Revisar cambios
-   Solicitar review (si aplica)

------------------------------------------------------------------------

# 4️⃣ Git desde Android Studio (lo importante)

## Qué hace Android Studio por debajo

-   **Commit** en Android Studio → ejecuta `git commit`
-   **Push** → `git push`
-   **Update Project** (según opción) → normalmente `git pull` (o
    fetch + merge)
-   **Checkout** rama → `git checkout`
-   **New Branch** → `git checkout -b`

✅ Consejo: aunque uses UI, conviene entender los comandos para resolver
conflictos y errores rápido.

------------------------------------------------------------------------

# 5️⃣ Tipos de Merge en GitHub (elige bien)

## Merge Commit

-   Mantiene historial completo
-   Añade un commit de merge extra

## Squash & Merge (recomendado para features)

-   Une todos los commits del PR en 1 commit
-   Historial más limpio

## Rebase & Merge

-   Reescribe historial
-   No crea commit de merge
-   Útil si el equipo trabaja con rebase "limpio"

------------------------------------------------------------------------

# 6️⃣ Rebase correcto antes de PR (cuando tu rama se queda atrás)

Actualizar tu rama con lo último de main:

``` shell
git checkout feature/login
git fetch
git rebase origin/main
```

Si hay conflictos:

``` shell
# arregla archivos en conflicto
git add .
git rebase --continue
```

Abortar rebase si se lía:

``` shell
git rebase --abort
```

⚠️ Si ya habías hecho push y rebaseas, normalmente necesitarás:

``` shell
git push --force-with-lease
```

(usar con cuidado y nunca en `main`)

------------------------------------------------------------------------

# 7️⃣ Resolver conflictos (Android Studio + terminal)

Cuando Git te diga algo tipo:

CONFLICT (content): Merge conflict in .../MainActivity.kt

1.  Abre el archivo y busca marcadores:
    -   `<<<<<<<`
    -   `=======`
    -   `>>>>>>>`
2.  Decide qué código se queda (o combina ambos)
3.  Luego:

``` shell
git add ruta/al/archivo.kt
git commit
```

En rebase:

``` shell
git add ruta/al/archivo.kt
git rebase --continue
```

------------------------------------------------------------------------

# 8️⃣ Buenas prácticas profesionales

-   No trabajar directamente en **main**
-   Commits pequeños y descriptivos
-   PRs por feature/cambio claro
-   Evitar `push --force` (y si toca, usar `--force-with-lease`)
-   Revisar `git status` antes de commitear
-   No mezclar refactors grandes con features (separa PRs)

------------------------------------------------------------------------

# 9️⃣ Comandos clave "de supervivencia"

## Ver historial visual

``` shell
git log --oneline --graph --decorate --all
```

## Ver cambios antes de commitear

``` shell
git diff
```

## Ver último commit

``` shell
git log -1 HEAD
```

## Revertir un commit (seguro en remoto)

``` shell
git revert <hash>
```

## Reset suave (quitar último commit pero mantener cambios)

``` shell
git reset --soft HEAD~1
```

## Reset duro (peligroso)

``` shell
git reset --hard HEAD~1
```

------------------------------------------------------------------------

# 🔟 Publicar versión (Tags / Releases)

Crear tag:

``` shell
git tag v1.0.0
```

Subir tags:

``` shell
git push origin --tags
```

Usado para releases en GitHub, builds firmadas y versiones.

------------------------------------------------------------------------

# 1️⃣1️⃣ Android Studio + Git: .gitignore recomendado (Android)

## Qué NO subir nunca

-   `.idea/` (en general)\
-   `*.iml`
-   `local.properties` (contiene rutas locales del SDK)
-   `build/` (de módulos)
-   `.gradle/`
-   `captures/`
-   `externalNativeBuild/`
-   `cmake-build-*`
-   `*.apk` / `*.aab` (salvo releases adjuntos)

## Ejemplo mínimo (útil)

``` shell
# Android Studio / IntelliJ
.idea/
*.iml

# Gradle
.gradle/
build/

# Local config
local.properties

# NDK / CMake
.externalNativeBuild/
cxx/
cmake-build-*/

# Keystores (NO subir)
*.jks
*.keystore

# Logs
*.log
```

✅ Si usas Jetpack Compose, Kotlin, etc., esto sigue siendo válido.

------------------------------------------------------------------------

# 1️⃣2️⃣ Keystore y firmas (muy importante)

-   **Nunca** subas tu keystore (`.jks` / `.keystore`) al repo.
-   Guarda passwords en un gestor o variables de entorno.
-   Para CI, usa secretos (GitHub Actions Secrets, etc.).

------------------------------------------------------------------------

# 1️⃣3️⃣ Flujo completo resumido (ideal)

``` shell
git checkout main
git pull
git checkout -b feature/nueva-funcionalidad
# trabajar
git add .
git commit -m "Implementa nueva funcionalidad"
git push -u origin feature/nueva-funcionalidad
# Pull Request
# Merge (Squash recomendado)
```

------------------------------------------------------------------------

# 🧠 Conceptos fundamentales

-   **Working Directory** → archivos reales
-   **Staging Area** → zona previa al commit
-   **Commit** → snapshot del proyecto
-   **HEAD** → posición actual
-   **Origin** → remoto principal
-   **Upstream** → rama remota asociada

------------------------------------------------------------------------

Fin del Manual
