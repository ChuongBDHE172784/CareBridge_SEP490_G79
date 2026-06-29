---
name: Serene Operational Portal
colors:
  surface: '#fef8f4'
  surface-dim: '#dfd9d5'
  surface-bright: '#fef8f4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f9f2ee'
  surface-container: '#f3ede8'
  surface-container-high: '#ede7e3'
  surface-container-highest: '#e7e1dd'
  on-surface: '#1d1b19'
  on-surface-variant: '#524440'
  inverse-surface: '#32302d'
  inverse-on-surface: '#f6f0eb'
  outline: '#84736f'
  outline-variant: '#d6c2bd'
  surface-tint: '#845143'
  primary: '#845143'
  on-primary: '#ffffff'
  primary-container: '#c98c7b'
  on-primary-container: '#51271b'
  inverse-primary: '#fab7a4'
  secondary: '#625d59'
  on-secondary: '#ffffff'
  secondary-container: '#e9e1db'
  on-secondary-container: '#68635f'
  tertiary: '#605e5a'
  on-tertiary: '#ffffff'
  tertiary-container: '#9e9a96'
  on-tertiary-container: '#34322f'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbd1'
  primary-fixed-dim: '#fab7a4'
  on-primary-fixed: '#341006'
  on-primary-fixed-variant: '#693a2d'
  secondary-fixed: '#e9e1db'
  secondary-fixed-dim: '#ccc5c0'
  on-secondary-fixed: '#1e1b18'
  on-secondary-fixed-variant: '#4a4642'
  tertiary-fixed: '#e6e2dd'
  tertiary-fixed-dim: '#cac6c1'
  on-tertiary-fixed: '#1d1b19'
  on-tertiary-fixed-variant: '#484643'
  background: '#fef8f4'
  on-background: '#1d1b19'
  surface-variant: '#e7e1dd'
  surface-main: '#FFFFFF'
  surface-accent-light: '#F6F1EC'
  surface-accent-muted: '#F2EAE4'
  status-active: '#C98C7B'
  text-heading: '#2D2A28'
  text-body: '#524F4C'
typography:
  display-lg:
    fontFamily: Quicksand
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Quicksand
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Quicksand
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Quicksand
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Quicksand
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-md:
    fontFamily: Quicksand
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  nav-item:
    fontFamily: Quicksand
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 24px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  sidebar-width: 280px
  container-padding: 32px
  grid-gutter: 24px
  panel-gap: 24px
  stack-sm: 8px
  stack-md: 16px
---

## Brand & Style

This design system is engineered for a high-fidelity B2B administrative environment that prioritizes emotional clarity and operational efficiency. The brand personality is grounded, empathetic, and professional, moving away from cold corporate blues toward a warm, human-centric "Care-Tech" aesthetic. 

The design style is **Modern Minimalist with Tactile Softness**. It utilizes expansive white space and a "Floating Panel" architecture to organize complex data. By leveraging soft-touch geometry and a restrained warm palette, the interface reduces cognitive load and "alert fatigue" common in administrative portals, ensuring the user feels calm yet fully in control.

## Colors

The palette is anchored by a stark white background to maintain a "clean room" feeling for data entry and monitoring. 

- **Primary**: Used exclusively for high-priority actions (CTAs), active navigation states, and critical status indicators.
- **Accents**: The warm neutrals (`#F6F1EC` and `#F2EAE4`) serve as subtle background shifts to differentiate the sidebar or secondary dashboard widgets without introducing heavy borders.
- **Neutral**: Text and icons utilize a soft charcoal rather than pure black to maintain the warmth of the interface.

## Typography

This design system uses **Quicksand** for all levels to leverage its rounded terminals, which echo the softness of the UI's geometry. 

- **Headlines**: Use a heavier weight (`600-700`) to provide clear visual hierarchy against the light backgrounds.
- **Body Text**: Set to `500` weight as the default to ensure legibility on high-density data tables.
- **Navigation**: Sidebar items use a specific `15px` size to balance density and touch-target comfort.
- **Labels**: Small caps and increased letter spacing are used for table headers and category labels to distinguish them from interactive data.

## Layout & Spacing

The portal follows a **Fixed-Fluid Hybrid** model. The sidebar remains fixed at `280px`, while the main content area utilizes a fluid 12-column grid that expands to fill the viewport.

- **Floating Panels**: Content is organized into discrete white cards with large internal padding (`32px`). These panels are separated by a consistent `24px` gap, allowing the warm background accent to "peek through" and define the edges.
- **Sidebar Layout**: A vertical stack with items (Tổng quan, Người dùng, etc.) padded at `12px` vertically to create an open, airy navigation feel.
- **Mobile Adaptivity**: At the `768px` breakpoint, the sidebar collapses into a bottom navigation bar or a hamburger drawer, and panel margins reduce to `16px`.

## Elevation & Depth

Hierarchy is established through **Ambient Warm Shadows** and **Tonal Layering** rather than harsh lines.

- **Level 0 (Background)**: The base layer uses `#F6F1EC`.
- **Level 1 (Panels)**: Main content cards use `#FFFFFF` with a very soft, diffused shadow: `0 4px 20px rgba(201, 140, 123, 0.08)`. The slight tint of the primary color in the shadow ensures it feels integrated into the warm theme.
- **Level 2 (Interactive)**: Buttons and hovered states use a more pronounced shadow to indicate "lift."
- **Sidebar**: Uses no shadow, relying on the contrast between `#F2EAE4` and the white main content area to define its boundary.

## Shapes

The shape language is remarkably soft to counteract the "clinical" nature of an admin portal.

- **Large Panels**: Feature a `24px` to `32px` corner radius.
- **Interactive Elements**: Buttons, input fields, and chips use a "Pill" style (fully rounded) or a minimum of `12px` radius.
- **Selection Indicators**: The active state in the sidebar navigation uses a "capsule" shape that creates a pill-like highlight behind the text.

## Components

- **Sidebar Navigation**: Items are displayed with a leading icon. The active state uses a `#C98C7B` background with white text, or a subtle `#F2EAE4` background with primary-colored text for a softer selection.
- **Primary Buttons**: Fully rounded (pill), background `#C98C7B`, text white. Use a subtle scale-down effect (0.98) on click for tactile feedback.
- **Input Fields**: Thick `2px` borders in `#F2EAE4` that transition to `#C98C7B` on focus. Backgrounds should remain white.
- **Status Chips**: Use high-transparency versions of status colors (e.g., `#C98C7B` at 10% opacity) with opaque text for a modern "glass" look within tables.
- **Data Tables**: Remove vertical borders. Use horizontal dividers in `#F2EAE4` only. Row hovering should trigger a subtle shift to `#F6F1EC`.
- **Cards**: All cards must have the standard `24px` radius and the ambient warm shadow defined in Elevation.