# Search Domains & Stacks Reference

## Available Domains

| Domain | Use For | Example Keywords |
|--------|---------|------------------|
| `product` | Product type recommendations | SaaS, e-commerce, portfolio, healthcare, beauty, service |
| `style` | UI styles, colors, effects | glassmorphism, minimalism, dark mode, brutalism |
| `typography` | Font pairings, Google Fonts | elegant, playful, professional, modern |
| `color` | Color palettes by product type | saas, ecommerce, healthcare, beauty, fintech, service |
| `landing` | Page structure, CTA strategies | hero, hero-centric, testimonial, pricing, social-proof |
| `chart` | Chart types, library recommendations | trend, comparison, timeline, funnel, pie |
| `ux` | Best practices, anti-patterns | animation, accessibility, z-index, loading |
| `react` | React/Next.js performance | waterfall, bundle, suspense, memo, rerender, cache |
| `web` | Web interface guidelines | aria, focus, keyboard, semantic, virtualize |
| `prompt` | AI prompts, CSS keywords | (style name) |

## Available Stacks

| Stack | Focus |
|-------|-------|
| `html-tailwind` | Tailwind utilities, responsive, a11y (DEFAULT) |
| `react` | State, hooks, performance, patterns |
| `nextjs` | SSR, routing, images, API routes |
| `vue` | Composition API, Pinia, Vue Router |
| `svelte` | Runes, stores, SvelteKit |
| `swiftui` | Views, State, Navigation, Animation |
| `react-native` | Components, Navigation, Lists |
| `flutter` | Widgets, State, Layout, Theming |
| `shadcn` | shadcn/ui components, theming, forms, patterns |
| `jetpack-compose` | Composables, Modifiers, State Hoisting, Recomposition |

## Domain Search Examples

```bash
# Style search
python3 skills/ui-ux-pro-max/scripts/search.py "glassmorphism dark" --domain style

# Chart recommendations
python3 skills/ui-ux-pro-max/scripts/search.py "real-time dashboard" --domain chart

# UX best practices
python3 skills/ui-ux-pro-max/scripts/search.py "animation accessibility" --domain ux

# Alternative fonts
python3 skills/ui-ux-pro-max/scripts/search.py "elegant luxury" --domain typography

# Landing structure
python3 skills/ui-ux-pro-max/scripts/search.py "hero social-proof" --domain landing
```

## Stack Search Examples

```bash
# Default stack (html-tailwind)
python3 skills/ui-ux-pro-max/scripts/search.py "layout responsive form" --stack html-tailwind

# React patterns
python3 skills/ui-ux-pro-max/scripts/search.py "state management hooks" --stack react

# Next.js SSR
python3 skills/ui-ux-pro-max/scripts/search.py "routing images api" --stack nextjs
```
