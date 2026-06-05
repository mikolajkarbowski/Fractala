import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

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
