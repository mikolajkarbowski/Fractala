import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

// Vite serves this `frontend/` directory as the web root. The Scala.js plugin
// runs sbt to link the `frontend` subproject and exposes its output as the
// virtual module `scalajs:main.js` (imported from main.js).
export default defineConfig({
  plugins: [
    scalaJSPlugin({
      // Directory containing build.sbt (the repo root, one level up from here).
      cwd: "..",
      // The sbt subproject id to link.
      projectID: "frontend",
    }),
  ],
});
