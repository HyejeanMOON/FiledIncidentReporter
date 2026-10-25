import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      // Next resolves this marker to an empty module on the server; do the same for tests.
      "server-only": "next/dist/compiled/server-only/empty.js",
    },
  },
  test: {
    environment: "node",
    include: ["src/**/*.test.ts"],
  },
});
