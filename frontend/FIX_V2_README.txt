Fix v2:
1. Removed raw %VITE_ANALYTICS_ENDPOINT% placeholder script from client/index.html
2. Replaced it with safe runtime optional analytics loader
3. Disabled Vite HMR overlay in vite.config.ts
Cause: Vite tried to decode a URL containing %VITE_ANALYTICS_ENDPOINT%, leading to decodeURI failure.
