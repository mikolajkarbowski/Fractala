# Fractala

---

## Writing Fractals (The DSL)

Our custom DSL is designed to be:
- human-readable
- order-independent
- type-safe

A fractal definition consists of up to four blocks:
- `Config`
- `Colors`
- `Axiom`
- `Rules`

---

### Example 1: The Classic Colorful Plant

This example demonstrates a standard deterministic L-System.
Notice how colors are defined once in the `Colors` block and reused safely within the `Rules`.

```plaintext
// My Beautiful Fractal
Config {
  lineLength: 10.0
  lineWidth: 2.0
  turningAngle: 25.0
  startingColor: stem
  maxIterations: 5
}

Colors {
  stem: 0.54, 0.27, 0.07  // Brown
  leaf: 0.0, 1.0, 0.0     // Green
}

Axiom: X

Rules {
  // X acts as a structural placeholder generating branches
  X -> <stem> F [ + X ] [ - X ] + F
  
  // F draws the actual lines and grows over time
  F -> F F <leaf> [ + F ]
}
```
---

### Example 2: The Stochastic Magic Tree

The parser is fully order-independent.
You can put the Axiom at the top and Config at the bottom.

This example also highlights stochastic rules — defining probabilities
for how a symbol evolves, resulting in organic, randomized growth.

```plaintext
Axiom: F

// Notice the weights in parentheses (e.g., 0.33 means 33% chance)
Rules {
  F (0.33) -> F [ + <bloom> F ] F
  F (0.33) -> F [ - <bloom> F ] F
  F (0.34) -> F <wood> F
}

Colors {
  wood: 0.6, 0.4, 0.2
  bloom: 1.0, 0.4, 0.7
}

// Config fields are case-insensitive and optional (defaults apply if omitted)
CONFIG {
  turningAngle: 22.5
  maxIterations: 4
}
```

## Running the API server

To run the server, use:

```ps1
sbt "project api" run
```

The application uses **PureConfig** for configuration management. You can find the default settings in: `api/src/main/resources/application.conf`.

To override settings without changing the file, you can use environment variables:

**Windows (PowerShell):**
```powershell
$env:SERVER_PORT="9000"; sbt "project api" run
```

## Running the Frontend

The frontend is a [Scala.js](https://www.scala-js.org/) application bundled with
[Vite](https://vitejs.dev/) via the [`@scala-js/vite-plugin-scalajs`](https://github.com/scala-js/vite-plugin-scalajs)
plugin. Vite drives sbt for you, so you do not need to link the Scala.js output manually.

### Prerequisites

- **JDK 17 or newer** (e.g. [Temurin](https://adoptium.net/)).
- **sbt 1.x** — see `project/build.properties` for the pinned version.
- **Node.js 18 or newer** (ships with `npm`) — required only for the frontend.

### First-time setup

Install the Node dependencies (run once, from the `frontend/` directory):

```powershell
cd frontend
npm install
```

### Development

The frontend talks to the API at `http://localhost:9000`, so start the API first
(see above), then in a second terminal:

```powershell
cd frontend
npm run dev
```

Vite prints a local URL (default `http://localhost:5173`). It compiles the Scala.js
sources through sbt and hot-reloads the browser when you edit the Scala code.

### Production build

```powershell
cd frontend
npm run build      # outputs static files to frontend/dist
npm run preview    # serves the built output locally
```
