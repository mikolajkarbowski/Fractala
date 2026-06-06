// Frontend runtime configuration.
//
// The API base URL can be set per environment via the Vite env var VITE_API_BASE_URL
// (e.g. in a .env file or via the shell). It falls back to the local dev server.
window.__FRACTALA_API__ = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:9000";
