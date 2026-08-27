---
name: CareBridge Design System
colors:
  surface: '#fff8f6'
  surface-dim: '#f1d4ca'
  surface-bright: '#fff8f6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fff1ec'
  surface-container: '#ffe9e3'
  surface-container-high: '#ffe2d9'
  surface-container-highest: '#fadcd3'
  on-surface: '#271812'
  on-surface-variant: '#524440'
  inverse-surface: '#3e2c26'
  inverse-on-surface: '#ffede7'
  outline: '#84736f'
  outline-variant: '#d6c2bd'
  surface-tint: '#845143'
  primary: '#845143'
  on-primary: '#ffffff'
  primary-container: '#c98c7b'
  on-primary-container: '#51271b'
  inverse-primary: '#fab7a4'
  secondary: '#6e5a52'
  on-secondary: '#ffffff'
  secondary-container: '#f6dacf'
  on-secondary-container: '#735e56'
  tertiary: '#625d59'
  on-tertiary: '#ffffff'
  tertiary-container: '#a09a95'
  on-tertiary-container: '#36322e'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbd1'
  primary-fixed-dim: '#fab7a4'
  on-primary-fixed: '#341006'
  on-primary-fixed-variant: '#693a2d'
  secondary-fixed: '#f8ddd2'
  secondary-fixed-dim: '#dbc1b7'
  on-secondary-fixed: '#271812'
  on-secondary-fixed-variant: '#55433b'
  tertiary-fixed: '#e9e1db'
  tertiary-fixed-dim: '#ccc5c0'
  on-tertiary-fixed: '#1e1b18'
  on-tertiary-fixed-variant: '#4a4642'
  background: '#fff8f6'
  on-background: '#271812'
  surface-variant: '#fadcd3'
typography:
  display-lg:
    fontFamily: Lexend
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Lexend
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Lexend
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Lexend
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Lexend
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Lexend
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  button:
    fontFamily: Lexend
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 20px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  safe-margin: 24px
  gutter: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
  touch-target-min: 48px
---

## Brand & Style

This design system is built on the pillars of **nurturing care, professional reliability, and warmth**. Designed specifically for the Vietnamese maternal and childcare market, the visual language avoids the sterile feel of traditional medical apps, opting instead for an "Organic Minimalism" that feels like a supportive companion.

The aesthetic prioritizes soft tactile elements, generous whitespace, and a high-contrast yet warm color palette to ensure legibility and calm for busy parents. Every interaction is designed to feel soft and intentional, reducing cognitive load through clear hierarchy and friendly geometry.

## Colors

The palette is inspired by natural earth tones to evoke a sense of grounding and maternal warmth. 

- **Primary (#C98C7B):** A warm terracotta used for key actions, progress indicators, and brand moments.
- **Text & Contrast (#5A463F):** A deep cocoa brown replaces pure black to maintain softness while ensuring AA/AAA accessibility for long-form reading (e.g., breastfeeding guides).
- **Secondary Text (#9C857C):** Used for metadata, labels, and less critical information.
- **Canvas (#F6F1EC):** The base background color, providing a soft, non-glare surface that is easier on the eyes during late-night checks.
- **Surfaces:** Pure White is reserved for elevated cards to create clear separation from the warm canvas.

## Typography

The design system utilizes **Lexend**, a font family specifically designed to reduce visual stress and improve reading proficiency. Its rounded terminals and open counters align perfectly with the friendly and approachable brand personality.

All Vietnamese diacritics must be handled with standard Lexend weights. For content-heavy sections, such as "Cẩm nang chăm sóc bé," maintain a line height of at least 1.5x the font size to ensure a comfortable reading experience for sleep-deprived users.

## Layout & Spacing

The layout is optimized for a **390x844 (iPhone 13/14)** portrait base. It uses a flexible fluid grid with a strict **24px safe margin** on the left and right edges to prevent content from feeling cramped.

- **Vertical Rhythm:** Follows an 8px base grid.
- **Touch Targets:** Every interactive element (buttons, toggles, icons) must occupy a minimum area of 48x48px to accommodate parents who may be multi-tasking or holding a child.
- **Card Padding:** Standard inner padding for cards is 20px or 24px to match the outer safe margins.

## Elevation & Depth

Depth is conveyed through **Tonal Layering** and **Ambient Shadows**.

1.  **Level 0 (Canvas):** The #F6F1EC background.
2.  **Level 1 (Cards):** White surfaces with a very soft, diffused shadow: `box-shadow: 0px 4px 20px rgba(90, 70, 63, 0.06);`. The shadow uses a tint of the Deep Cocoa primary text color rather than pure black to maintain the warm aesthetic.
3.  **Level 2 (Active Elements):** Floating Action Buttons (FABs) or active modals use a slightly deeper shadow: `box-shadow: 0px 8px 24px rgba(90, 70, 63, 0.12);`.

Avoid harsh borders. Use subtle value shifts between #FFFFFF and #F2EAE4 to differentiate between parent and child containers.

## Shapes

The shape language is defined by extreme roundness, mimicking the "softness" associated with childcare.

- **Buttons & Chips:** Always use the "Pill" shape (fully rounded corners).
- **Cards:** Large containers use a radius of **24px to 32px**.
- **Input Fields:** Use a 16px radius to balance between the pill-shaped buttons and the larger cards.
- **Iconography:** Use Lucide-style icons with a 2px stroke and rounded caps/joins.

## Components

### Buttons
- **Primary:** Background #C98C7B, Text #FFFFFF. Pill-shaped. Height 52px for main CTAs.
- **Secondary:** Background #F2EAE4, Text #5A463F. Pill-shaped.

### Chips & Badges
- Used for categories like "Sức khỏe" or "Dinh dưỡng."
- Always include an icon + text for status indicators.
- Example: A "Hoàn thành" badge should have a check icon and the text "Hoàn thành" in #5A463F.

### Cards
- Standard card: White background, 24px radius, 20px internal padding.
- Used for "Lịch tiêm chủng" or "Nhật ký của bé" entries.

### Input Fields
- Background #FFFFFF, Border 1px solid #F2EAE4.
- Placeholder text in #9C857C.
- Height 56px to ensure easy tapping.

### Feedback & Status
- **Success:** Use #C98C7B (Primary) with a check icon. 
- **Warning/Alert:** Use a soft muted orange (e.g., #E8A87C) but always accompany with the Deep Cocoa text for readability. Never rely on color alone; always use descriptive Vietnamese text (e.g., "Cần chú ý").