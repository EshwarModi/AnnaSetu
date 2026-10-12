# AnnaSetu - Frontend Architecture & Design System Guide

## 1. Architectural Philosophy: The Pure Vanilla CSS3 Approach

### Why No Bootstrap or Tailwind?
In enterprise and interview discussions, relying heavily on third-party CSS utility frameworks or component libraries often introduces:
1. **Unnecessary Bundle Bloat & Dependency Churn**: External dependencies frequently introduce breaking changes across major versions and increase asset payload.
2. **Generic Aesthetics**: Bootstrap-based applications tend to look identical, whereas AnnaSetu requires a culturally resonant, warm, trustworthy Indian charitable aesthetic (Saffron Terracotta `#d9531e` and Nourishing Green `#1b7a43`).
3. **Deep Mastery of CSS Fundamentals**: Demonstrates strong grasp of the CSS Cascade, Box Model, Flexbox, CSS Grid, Custom Properties, and Mobile-First Responsive Breakpoints.

---

## 2. Design Tokens & CSS Custom Properties (`main.css`)

Our design system is built entirely on centralized **CSS Custom Properties (`:root`)**, providing a single source of truth across all views:

### A. Color Palette
- **Brand Primary (`--color-primary: #d9531e`)**: Represents warmth, nourishing meals, and celebratory giving.
- **Brand Secondary (`--color-secondary: #1b7a43`)**: Represents life, growth, and elder care / child shelter trust.
- **Neutral Scale (`--color-gray-50` to `--color-gray-900`)**: Warm off-white background (`#faf9f6`) paired with deep slate text (`#1f2937`) to maximize readability without the harshness of pure black `#000000`.
- **Semantic Statuses**:
  - Success: `#15803d` / `#dcfce7` (Confirmed bookings, available slots)
  - Warning: `#b45309` / `#fef3c7` (Pending approvals, partial slots)
  - Danger: `#b91c1c` / `#fee2e2` (Fully booked slots, validation errors)
  - Info: `#0369a1` / `#e0f2fe` (Informational alerts, instructions)

### B. Typography Scale
- **Modular Scale**:
  - `xs (0.75rem / 12px)`: Badges, table headers, metadata captions.
  - `sm (0.875rem / 14px)`: Form labels, helper text, button labels.
  - `base (1rem / 16px)`: Paragraphs, input values.
  - `lg (1.125rem / 18px)`: Subheadings, card titles.
  - `xl` to `5xl (20px to 48px)`: Hero headings, section titles, impact statistics.
- **System Font Stack**: Uses native platform typography (`system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto`) to achieve zero network latency and native look-and-feel on Android, iOS, Windows, and macOS.

### C. 4px / 8px Spacing Scale
All margins, paddings, and grid gaps use a consistent mathematical progression (`--space-1: 0.25rem` to `--space-16: 4rem`). This eliminates arbitrary numbers and produces balanced visual rhythm.

---

## 3. Mobile-First Responsive Architecture

AnnaSetu is built strictly **Mobile-First**:
- Default styles without media queries target mobile screens starting from **360px** width (e.g. compact Android and iPhone devices).
- Single-column stacked layouts, full-width inputs, and touch targets of at least 42px to 48px.

### Progressive Enhancement Breakpoints:
1. **Mobile (`< 768px`)**:
   - Navigation links wrap cleanly or collapse.
   - Search form stacks into a vertical sequence.
   - Organization cards render in a 1-column scrollable feed.
   - Form fields render in single-column stacks.
2. **Tablet (`@media (min-width: 768px)`)**:
   - Hero font escalates to 2.75rem.
   - Search form aligns horizontally into a 4-column bar (`grid-template-columns: repeat(3, 1fr) auto`).
   - "How It Works" transforms into a 3-column progression.
   - Organization cards switch to a 2-column grid.
   - Form inputs split into clean 2-column and 3-column rows (`.form-row`, `.form-row-3`).
3. **Desktop (`@media (min-width: 1024px)`)**:
   - Organization showcase expands to a 3-column grid (`grid-template-columns: repeat(3, 1fr)`).
   - Footer expands to a 3-column layout (Brand & Mission, Platform Links, Coverage Cities).
4. **Wide Desktop (`@media (min-width: 1280px)`)**:
   - Page container centers with a max width of `1240px` (`--container-max-width`) preventing ultra-wide eye strain.

---

## 4. Reusable Thymeleaf UI Fragments

Rather than duplicating HTML structures across templates, Phase 3 establishes reusable fragments:

| Fragment Signature | File | Purpose |
|---|---|---|
| `navbar` | `fragments/layout.html` | Role-aware top navigation (`sec:authorize`) with user pill and CSRF logout |
| `footer` | `fragments/layout.html` | 3-column responsive footer with platform links, cities, and copyright |
| `alert(type, message)` | `fragments/components.html` | Semantic alert banners (success, danger, warning, info) with contextual icon |
| `slot-badge(meal, capacity, booked)` | `fragments/components.html` | Renders meal window tag and dynamic status badge (`Available`, `1 Left`, `Booked`) |
| `organization-card(org)` | `fragments/components.html` | Card displaying partner name, type badge, diet badge, location, capacity, and action |
| `pagination(page, totalPages, url)` | `fragments/components.html` | Accessible pagination controls with active/disabled states |

---

## 5. Web Accessibility & Performance Best Practices

1. **Semantic HTML5**: Utilizes `<header>`, `<nav>`, `<main>`, `<section>`, `<article>`, and `<footer>` rather than generic `<div>` soup.
2. **Color Contrast Compliance**: All text colors on background surfaces exceed WCAG 2.1 AA 4.5:1 contrast ratios.
3. **Accessible Form Feedback**: Input focus states use both border color transitions and glowing focus rings (`box-shadow: 0 0 0 3px var(--color-primary-focus)`), ensuring visibility for keyboard navigators without disabling browser focus outlines.
4. **Touch Friendliness**: Button padding and form inputs maintain minimum 42px height for mobile fingertips.
5. **Zero CSS Framework Overhead**: Clean ~15KB unminified CSS file with zero render-blocking JavaScript dependencies.
