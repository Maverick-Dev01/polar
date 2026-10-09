# Fase 3 · Subproyecto 4: Guía de uso dentro de la app — plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.

**Goal:** Que cualquier persona aprenda a usar Polar sin ayuda externa, con tres piezas iguales en Android y Mac:
1. un recorrido animado la primera vez;
2. un modo «?» que explica cada control;
3. un centro de ayuda con búsqueda y botones «Llévame ahí».

**Spec:** `docs/superpowers/specs/2026-10-08-fase-3-solidez-design.md`, sección «Subproyecto 4».

## Global Constraints
Las de los subproyectos anteriores. Además:
- **Un solo contenido** en `shared-fixtures/help.json`, copiado al compilar:
  - Android: a `assets/help.json`;
  - Mac: a `Resources/help.json`; agrégalo en `build.sh`.
- **Esquema por artículo:** `id`, `categoria` (empezar | fotos | texto | diseno | imprimir), `titulo`, `resumen`, `pasos[]`, `destino` (id de pantalla o panel, por ejemplo `editor.filtros`), `animacion` (id de una animación predefinida).
- **Animaciones:**
  - nativas: Compose `animate*` / `Transition` y SwiftUI `withAnimation` / `PhaseAnimator`;
  - sin dependencias nuevas;
  - respetan reducir movimiento: cuadro final estático.
- **Accesibilidad:** el recorrido y el modo «?» funcionan con TalkBack y VoiceOver; anuncian cada paso y son navegables por foco.

## Review Focus
1. El recorrido no debe reaparecer después de completarlo o saltarlo, salvo que se pida desde Ayuda.
2. Si un control del recorrido no está visible (por ejemplo, el panel cerrado o una ventana estrecha), el paso debe abrir lo necesario o saltarse sin quedar atascado.
3. En modo «?», tocar un control nunca debe ejecutar su acción.
4. Cada `destino` de `help.json` debe existir en las dos apps; lo verifica una prueba.
5. Con letra al 130 %, las burbujas no deben salirse de la pantalla.

---

### Task 1: Contenido
- `shared-fixtures/help.json` con unos 20 artículos en español, claros y breves.
- **Temas:**
  - crear un diseño;
  - agregar fotos y carpetas;
  - cambiar una foto;
  - encuadrar;
  - quitar fondo;
  - filtros;
  - texto para todas o una;
  - frases;
  - fecha de la foto;
  - QR de canción;
  - diseño por hoja;
  - colores y distribución;
  - importar molde y «Mis moldes»;
  - papel y márgenes;
  - guías de corte;
  - calidad de exportación;
  - imprimir al 100 %;
  - compartir y guardar `.polar`;
  - deshacer;
  - abrir en la otra plataforma.
- Prueba de esquema en ambas plataformas.

### Task 2: Android — recorrido inicial (coach marks)
- Capa sobre el editor con un recorte de resaltado (`Modifier.onGloballyPositioned` registra los objetivos), una burbuja con texto y una animación por paso.
- 5 pasos: Fotos → hoja → Texto → Filtros → Imprimir.
- Saltar y Siguiente; se guarda «visto» en `SettingsRepository`.
- Pruebas de Compose: avanza, salta y no reaparece.

### Task 3: Android — modo «?» y centro de ayuda
- Botón «?» en el menú del editor.
- Pantalla Ayuda desde Ajustes y desde el menú, con búsqueda (normaliza acentos), categorías, artículo con animación y «Llévame ahí» (navega al destino).

### Task 4: Mac — recorrido, modo «?» y centro de ayuda
- Equivalentes con `.overlay` y `anchorPreference` para los objetivos.
- Ayuda en el menú Ayuda (reemplaza el `NSAlert` «Cómo usar Polar») y en Ajustes.
- Pruebas en una suite nueva, `HelpChecks`.

### Task 5: Verificación
- Recorridos completos en las dos plataformas, también con reducir movimiento y con letra grande (Android).
- Prueba de que todos los destinos existen.
