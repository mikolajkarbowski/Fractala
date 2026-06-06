// Load runtime configuration first (sets window.__FRACTALA_API__ from the Vite env).
import "./config.js";

// The `scalajs:` import is resolved by @scala-js/vite-plugin-scalajs to the linked
// Scala.js output, which runs FractalApp.main automatically (scalaJSUseMainModuleInitializer := true).
import "scalajs:main.js";
