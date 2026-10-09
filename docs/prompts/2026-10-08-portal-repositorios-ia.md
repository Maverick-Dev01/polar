# Instrucción: «Arsenal», portal personal de repositorios y skills para agentes de IA

> Copia este documento completo como instrucción para el agente que construirá la app. «Arsenal» es un nombre provisional; propón 3 alternativas en la fase de diseño.

---

## 0. Qué quiero y por qué

Uso varios agentes de programación: Claude Code, Codex, Cursor y otros. Cada proyecto necesita herramientas distintas. Una app móvil de transporte público, por ejemplo, necesita mapas, tiempo real y rutas; otra puede necesitar video, diseño o seguridad.

Hoy encuentro repositorios útiles (skills, plugins, servidores MCP, CLIs, librerías) y los pierdo entre chats y pestañas. Quiero **mi propio catálogo**, desplegado en **Vercel**, que sirva para dos cosas:

1. **Para mí (humano):**
   - guardar y clasificar repositorios;
   - ver para qué sirve cada uno, qué tan seguro es y cómo se instala;
   - armar «kits» por tipo de proyecto.
2. **Para mis agentes de IA:** que un agente, en plena sesión, pueda **consultar el catálogo** y decidir qué skills le ayudan con la tarea actual. Ejemplos:
   - «voy a hacer una app de transporte con Google Maps, ¿qué me sirve?»;
   - «la IA está divagando o no resuelve, ¿qué herramienta lo corrige?».

Ya existen directorios públicos (skills.sh, claudskills.com, etc.). Este es **mío**: curado por mí, con mis notas, mis veredictos y mis kits.

---

## 1. Funciones

### 1.1 Agregar un repositorio (lo más importante: que sea rápido)
1. Pego una URL de GitHub, o `owner/repo`, o una URL de documentación.
2. El servidor consulta la **API de GitHub** y llena solo:
   - nombre, descripción, estrellas, licencia y fecha del último commit;
   - lenguaje y temas;
   - si es fork o está archivado.
3. Detecta automáticamente qué contiene:
   - **skills:** rutas `skills/*/SKILL.md`, con su nombre y descripción del frontmatter;
   - plugins: `.claude-plugin/`, `.codex-plugin/`, `.cursor-plugin/`;
   - servidores MCP: `.mcp.json`, `mcp.json`, `server.json`;
   - hooks;
   - scripts de instalación: `install.sh`, `npx …`.
4. Genera un **resumen del README** y propone:
   - **etiquetas:** categorías como diseño, video, mapas, seguridad, testing, rendimiento, contexto/tokens, documentación, móvil, web, backend, datos o productividad;
   - **casos de uso:** frases como «crear app móvil con mapas en tiempo real» o «la IA divaga».
5. Yo reviso, edito y guardo. Puedo agregar:
   - mis **notas**;
   - un **veredicto**: ✅ Recomendado / 🟡 Parcial / ⚠️ Con cuidado / ❌ No usar;
   - el **motivo** del veredicto.

### 1.2 Revisión de seguridad (obligatoria antes de marcar ✅)
Un panel por repositorio que muestre:
- **Licencia:** si es MIT, Apache u otra, si falta o no se puede determinar.
- **Actividad:** último commit y si está archivado.
- **Riesgos detectados por análisis estático** (sin ejecutar nada):
  - comandos `curl | sh` o `wget`;
  - `npx` o `npm i` dentro de un SKILL.md;
  - hooks que corren en cada acción del agente;
  - servidores MCP remotos;
  - instrucciones de pegar claves de API;
  - binarios descargables.
- **Lista de verificación manual** que yo marco: leí el SKILL.md, revisé los hooks, entiendo qué instala.

Nunca ejecutes código de los repositorios del catálogo. El análisis es solo de lectura, con la API de GitHub.

### 1.3 Instalación por agente
Para cada repositorio o skill, botones para copiar el comando según el agente. Para Claude Code:
- proyecto: `npx skills add owner/repo --agent claude-code --skill nombre --copy`;
- global: el mismo comando con `-g`;
- plugin: `claude plugin marketplace add owner/repo`.

También comandos para Codex, Cursor, Gemini CLI y OpenCode cuando apliquen, y un aviso visible si el repositorio **instala hooks o MCP**.

### 1.4 Kits por tipo de proyecto
Un «kit» es una colección con nombre, descripción y orden de instalación. Ejemplos:
- «App móvil nativa Android/Mac»;
- «Video promo con IA»;
- «App de transporte con mapas»;
- «Landing en Vercel».

Un botón **«Copiar instrucciones de instalación del kit»** genera el bloque de comandos y un mensaje listo para pegar al agente («instala y lee estas skills antes de empezar»). Los kits se pueden duplicar y editar.

### 1.5 «Mi IA tiene un problema»
Pantalla de diagnóstico por **síntoma**:
- divaga o da respuestas largas;
- se queda sin contexto;
- diseños genéricos;
- no prueba su código;
- comete errores de seguridad;
- rompe cosas al refactorizar;
- etc.

Cada síntoma enlaza a los repositorios que lo resuelven, con mi veredicto.

### 1.6 Búsqueda
- Texto libre y filtros por etiqueta, tipo (skill, plugin, MCP, CLI, librería), agente compatible, veredicto y licencia.
- Búsqueda semántica opcional (embeddings) si no complica el MVP; si no, déjala para la fase 2.

### 1.7 Acceso para agentes (diferenciador clave)
Diséñalo para que un agente lo use sin navegador:
- **`GET /llms.txt`:** índice en Markdown del catálogo, siguiendo la convención llms.txt.
- **`GET /api/catalog?query=…&tag=…&agent=…`:** JSON con repositorios, skills, veredicto, comandos de instalación y avisos de seguridad.
- **`GET /api/kits/:slug`:** el kit completo con sus comandos.
- **Servidor MCP remoto** (`/api/mcp`, transporte HTTP) con herramientas de solo lectura:
  - `search_catalog(query, tags?)`;
  - `get_repo(id)`;
  - `recommend_for_task(descripcion)`: devuelve los repositorios relevantes con el motivo;
  - `get_kit(slug)`.
- Agrega una página «Conecta tu agente» con el comando para registrar el MCP en Claude Code y Codex.
- Los endpoints públicos **solo exponen repositorios marcados como públicos**. Mis notas privadas nunca salen por la API.

### 1.8 Importar y exportar
Exportar e importar todo el catálogo en JSON, como respaldo y para moverlo entre cuentas.

---

## 2. Datos iniciales (siémbralos en la base)

Estos los evalué con Claude Code para mi proyecto Polar, una app nativa en Kotlin/Compose y SwiftUI:

| Repositorio | Tipo | Veredicto | Nota |
|---|---|---|---|
| `Leonxlnx/taste-skill` | skills de diseño | ✅ | Anti-diseño genérico para web. Usar `design-taste-frontend`, `redesign-existing-projects` y `minimalist-ui`. Solo son guías, no ejecutan nada |
| `motiondivision/ai-kit` | skill `motion` + MCP | ✅ para web | Skill oficial de Motion (antes Framer Motion). No aplica a apps nativas |
| `motiondivision/motion` | librería | 🟡 | Es el código de la librería, no una skill. Usar el ai-kit |
| `heygen-com/hyperframes` | skills de video | ✅ | Video desde HTML + GSAP; la ruta principal para promos |
| `remotion-dev/skills` | skills de video | ✅ | Video con React |
| `pbakaus/impeccable` | skill de diseño | ✅ | Auditoría, crítica y pulido de interfaces |
| `obra/superpowers` | metodología | ✅ | Idea → especificación → plan → TDD → revisión |
| `mattpocock/skills` | skills de ingeniería | 🟡 | Muy buenas, pero repiten lo que ya hace *superpowers*; evitar dos métodos a la vez. `grill-me` sirve para afinar requisitos |
| `shanraisshan/claude-code-best-practice` | documentación | 🟡 | Lectura de referencia, no se instala |
| `morluto/rea` | ingeniería inversa | ❌ para apps propias | Riesgo legal si se usa con apps de terceros |
| `ayghri/i-have-adhd` | estilo de respuesta | 🟡 | Respuestas cortas; redundante si ya usas el modo «caveman» |
| `mksglu/context-mode` | hooks + MCP de contexto | ⚠️ | Reduce tokens, pero intercepta todas las acciones y su licencia no es clara |
| `rtk-ai/rtk` | CLI proxy de tokens | ⚠️ | Apache 2.0, un solo binario. Puede ocultar líneas de error de compilaciones |

Los síntomas iniciales para la sección 1.5 salen de esas notas:
- «la IA divaga» → `i-have-adhd` o el modo caveman;
- «se queda sin contexto» → `rtk` o `context-mode`, con su aviso;
- «diseños genéricos» → `taste-skill` o `impeccable`;
- «no sigue un proceso» → `superpowers`.

---

## 3. Stack y arquitectura (propuesta; justifica cambios)

- **Next.js** (App Router) + **TypeScript** estricto, desplegado en **Vercel**.
- **Base de datos:** Postgres serverless desde el Marketplace de Vercel (Neon) con **Drizzle ORM** y migraciones versionadas.
- **Autenticación:** **Auth.js con GitHub OAuth**, permitido **solo para mi usuario de GitHub**, configurado por variable de entorno. Todo lo de escritura exige sesión. Lectura pública, configurable por repositorio.
- **GitHub API:** un token de solo lectura en una variable de entorno del servidor, nunca en el cliente. Caché de respuestas (por ejemplo, revalidación cada 24 h) y respeto del límite de peticiones.
- **Resumen del README:** genera el resumen con la API de Claude (modelo `claude-sonnet-5-5`) desde el servidor, con su clave en una variable de entorno. Si no hay clave, usa un resumen extractivo (primeros párrafos) para que la app funcione sin IA.
- **Markdown:** renderiza los README sanitizados (sin HTML ni scripts arbitrarios).
- **MCP:** usa el SDK oficial de MCP para TypeScript con transporte Streamable HTTP en una ruta de Next.js.
- **Pruebas:** Vitest para la lógica (detección de skills, análisis de riesgos, generación de comandos) y Playwright para los flujos principales (agregar repositorio, crear kit, copiar comandos).

---

## 4. Diseño (usa las mismas skills que en Polar)

Antes de diseñar, **instala y lee** estas skills en el proyecto:
```bash
npx skills add Leonxlnx/taste-skill --agent claude-code --skill design-taste-frontend --skill redesign-existing-projects --skill minimalist-ui --copy
npx skills add motiondivision/ai-kit --agent claude-code --skill motion --copy
```
Además usa **Impeccable** (`/impeccable`: shape, critique, audit, polish) y **frontend-design** si están disponibles, y el proceso de **superpowers** (brainstorming → especificación → plan → TDD → revisión).

**Principios:**
- **Estética editorial y tranquila**, no el «dashboard SaaS» genérico (lee el Problema A de la skill taste). Tipografía con carácter para titulares y una sans legible para el cuerpo.
- **Simetría estricta:**
  - las tarjetas del catálogo miden lo mismo sin importar el texto: descripción con líneas reservadas y elipsis;
  - las filas de botones tienen el mismo ancho y alto;
  - nada se encima;
  - cuadrícula de espaciado de 4/8 px y radios consistentes.
- **Modo claro y oscuro** completos, con tokens de color en CSS.
- **Animación con Motion:**
  - transiciones cortas (150–300 ms) y resortes suaves;
  - animación de layout al filtrar;
  - aparición escalonada en listas;
  - **respetar `prefers-reduced-motion`**.
- **Accesibilidad:**
  - WCAG 2.2 AA;
  - navegación completa con teclado;
  - foco visible y etiquetas en todos los controles;
  - contraste verificado.
- **Responsive:** funciona igual de bien en el teléfono, porque voy a consultar mi catálogo desde el móvil.
- **Detalles de producto:**
  - «copiar comando» con confirmación visible;
  - estados vacíos que enseñan qué hacer;
  - esqueletos de carga;
  - errores claros cuando GitHub no responde.

---

## 5. Proceso y entregables

1. **Brainstorming y especificación** → `docs/specs/…-design.md`. Incluye pantallas, modelo de datos, API, MCP y riesgos. Muéstrame un **prototipo visual** (HTML estático o capturas) en claro y oscuro, teléfono y escritorio. **Espera mi aprobación.**
2. **Plan de implementación** en tareas pequeñas con pruebas.
3. **MVP** (fase 1):
   - agregar repositorio con autollenado y detección de skills;
   - veredicto y notas;
   - catálogo con búsqueda y filtros;
   - kits con «copiar instrucciones»;
   - `/llms.txt` y `/api/catalog`;
   - semillas de la sección 2.
4. **Fase 2:**
   - servidor MCP;
   - pantalla «Mi IA tiene un problema»;
   - análisis de riesgos completo;
   - búsqueda semántica;
   - importar y exportar.
5. **Despliegue en Vercel:**
   - documenta las variables de entorno: `AUTH_GITHUB_ID`, `AUTH_GITHUB_SECRET`, `AUTH_SECRET`, `ALLOWED_GITHUB_LOGIN`, `GITHUB_TOKEN`, `DATABASE_URL`, `ANTHROPIC_API_KEY` (opcional);
   - documenta los pasos en `README.md`;
   - **yo** creo las cuentas y pego las claves. Tú nunca manejas mis contraseñas ni claves.
6. **Verificación:** lint, tipos, pruebas, Lighthouse (accesibilidad ≥ 95) y prueba manual en teléfono.

## 6. Reglas de git
- Repositorio nuevo en mi cuenta **personal** de GitHub (Maverick-Dev01), identidad **José Catalino <josecatalino.code@gmail.com>**. Verifica `git config user.email` antes del primer commit. **Nunca** uses la cuenta de la empresa.
- Commits pequeños, en español, con formato convencional.
- **Prohibido** agregar `Co-Authored-By`, «Generated with…» o cualquier mención a IA en commits o PRs.
- No hagas push ni despliegues sin que yo lo pida.
