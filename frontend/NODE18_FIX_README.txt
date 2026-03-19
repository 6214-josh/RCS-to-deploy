This package was patched to run on Node 18.

Main changes:
1. Removed @tailwindcss/vite (Tailwind v4 native binding path)
2. Downgraded Tailwind to v3 and @vitejs/plugin-react to v4
3. Added tailwind.config.cjs and postcss.config.cjs
4. Rewrote client/src/index.css to Tailwind v3 compatible syntax

Recommended commands on Windows:
npm run clean-install
npm run dev
