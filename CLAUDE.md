# CLAUDE.md - Agent Instructions

## Build & Development Commands
```
npm run dev              # Start dev server
npm run portfolio        # Start portfolio dev server
npm run tailwind         # Watch and compile Tailwind CSS
npm run dev:portfolio    # Run portfolio + tailwind concurrently
npm run test             # Run ClojureScript tests
npm run build            # Production build
```

## Project Structure
- ClojureScript codebase with Shadow-CLJS
- Replicant for DOM rendering (similar to React)
- DataScript for immutable database/state management
- TailwindCSS for styling

## Code Style Guidelines
- Use kebab-case for ClojureScript names
- Follow functional programming principles
- Prefer immutable data structures
- Use descriptive function names
- Single responsibility principle for functions

## Key Technologies
- Replicant (2025.03.27) - DOM rendering
- DataScript (1.5.4) - Immutable database
- Shadow-CLJS - ClojureScript compiler
- TailwindCSS (3.3.5) - CSS framework