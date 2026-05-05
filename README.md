# Fractala

Fractala is a comprehensive platform for defining, generating, and visualizing fractals using a custom L-System Domain-Specific Language (DSL). The project features a robust core engine, a REST API, and an interactive web frontend.

## Stack
- **Language:** [Scala 3.3.7](https://www.scala-lang.org/)
- **Build Tool:** [sbt](https://www.scala-sbt.org/)
- **Core Engine:**
  - [Cats](https://typelevel.org/cats/) - Functional programming abstractions
  - [Breeze](https://github.com/scalanlp/breeze) - Numerical processing and linear algebra
  - [FastParse](https://com-lihaoyi.github.io/fastparse/) - DSL parsing
- **API Server:**
  - [Tapir](https://tapir.softwaremill.com/) - Typed API endpoints
  - [Http4s](https://http4s.org/) - HTTP server (Ember)
  - [Circe](https://circe.github.io/circe/) - JSON library
  - [PureConfig](https://pureconfig.github.io/pureconfig/) - Configuration management
- **Frontend:**
  - [Scala.js](https://www.scala-js.org/) - Scala to JavaScript compiler
  - [Scalatags](https://github.com/com-lihaoyi/scalatags) - Type-safe HTML/CSS construction
  - [scalajs-dom](https://scala-js.github.io/scala-js-dom/) - DOM API for Scala.js

## Requirements
- **Java JDK 11 or higher** (JDK 17 recommended)
- **sbt 1.x**

## Project Structure
- `core/`: The heart of the project. Contains the L-System iteration logic, the DSL parser, and the drawing instruction generation.
- `api/`: A RESTful service that exposes the core logic via HTTP. Includes Swagger UI for API exploration.
- `frontend/`: A web-based user interface that allows users to write DSL code and visualize the resulting fractals in real-time.

## Setup & Run

### 1. API Server
To start the API server:
```bash
sbt "project api" run
```
The application uses **PureConfig** for configuration management. Default settings are in `api/src/main/resources/application.conf`.

### 2. Frontend
To compile the frontend:
```bash
sbt "project frontend" fastOptJS
```
After compilation, open `frontend/src/main/resources/index.html` in your web browser to use the interface.

**TODO:** Add a development server or automated asset pipeline for the frontend.

## Environment Variables
The API server configuration can be overridden using the following environment variables:
- `SERVER_HOST`: The host address to bind to (default: `0.0.0.0`).
- `SERVER_PORT`: The port number to listen on (default: `9000`).

Example (Linux/macOS):
```bash
SERVER_PORT=8080 sbt "project api" run
```

Example (Windows PowerShell):
```powershell
$env:SERVER_PORT="8080"; sbt "project api" run
```

## Tests
The project uses [ScalaTest](https://www.scalatest.org/) for unit testing.
- To run all tests across all modules: `sbt test`
- To run tests for the core module only: `sbt "project core" test`

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

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
