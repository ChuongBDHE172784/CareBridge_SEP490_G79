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
  headline-lg:
    fontFamily: Quicksand
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Quicksand
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Quicksand
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Quicksand
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-sm:
    fontFamily: Quicksand
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-caps:
    fontFamily: Quicksand
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  unit: 8px
  container-margin-mobile: 20px
  container-margin-desktop: 40px
  gutter-mobile: 16px
  gutter-desktop: 24px
  sidebar-width: 280px
---

## Brand & Style
The design system is built on a foundation of "Nurturing Professionalism." It balances the clinical reliability of a medical platform with the visceral warmth of human care. The aesthetic merges **Minimalism** with **Tactile/Skeuomorphic** undertones—using soft, pillowy elevations and organic roundedness to reduce user anxiety for both caregivers and patients.

The target audience includes healthcare professionals and administrative experts who require high utility without the cold, sterile feeling of traditional medical software. The emotional response should be one of immediate calm, safety, and effortless competence.

## Colors
The palette is rooted in earth tones to evoke a sense of grounding and stability. 

- **Canvas & Backgrounds:** Use `#F6F1EC` as the primary background for the mobile app to create a warm, non-glare environment. For the Web Portal, transition to `#FFFFFF` as the primary canvas to maintain a professional B2B "SaaS" clarity, using `#F6F1EC` only for subtle section nesting.
- **Primary Accent:** `#C98C7B` (Terracotta) is reserved for primary actions, progress indicators, and active states. It provides high visibility while remaining soft on the eyes.
- **Typography:** `#5A463F` is the lead color for all headings and body text to ensure high legibility without the harshness of pure black. `#9C857C` is used for secondary metadata and disabled states.

## Typography
This design system utilizes **Quicksand** across all platforms to maintain a friendly, approachable character. The rounded terminals of the typeface mirror the UI's physical roundedness.

- **Headlines:** Use Bold (700) weights to establish clear hierarchy, especially in the Web Portal's dashboard headers.
- **Body:** SemiBold (500/600) is preferred over Regular for body text to ensure the thin stems of the rounded font remain legible against the warm cream backgrounds.
- **Accessibility:** Maintain a minimum size of 14px for all functional text. Use the `label-caps` style sparingly for section headers in sidebars or small metadata tags.

## Layout & Spacing
The layout relies on a **8px base grid** to ensure consistency across both native and web environments.

- **Mobile (390x844):** A single-column fluid layout. Content is housed in cards with 20px side margins. Navigation is anchored to a fixed bottom bar with clear, centered labels.
- **Web (1440x1024):** A fixed sidebar (280px) on the left, with a fluid content area. The main workspace uses a 12-column grid. Modules are separated by 24px gutters to allow the UI to "breathe."
- **Rhythm:** Vertical spacing between cards should be consistent (16px on mobile, 24px on web) to maintain the "floating surface" aesthetic.

## Elevation & Depth
The system uses **Tonal Layering** combined with **Ambient Shadows** to create a sense of soft depth.

- **Shadows:** Avoid pure black shadows. Use a warm tint: `rgba(90, 70, 63, 0.08)` for base elevation and `rgba(90, 70, 63, 0.15)` for hovered or active states. The blur should be high (12px to 20px) to keep the look soft and "pillowy."
- **Z-Index Strategy:** 
  - Level 0: Canvas (`#F6F1EC`)
  - Level 1: Cards & Panels (`#FFFFFF`)
  - Level 2: Modals, Floating Action Buttons, and Bottom Nav.
- **Web Contrast:** On the Web Portal, use subtle 1px borders in `#F2EAE4` instead of heavy shadows for a cleaner, more data-centric professional look.

## Shapes
The shape language is extremely soft and organic, categorized as **Pill-shaped**.

- **Cards & Panels:** Use a 24px radius for standard mobile cards and 32px for large dashboard containers on web.
- **Buttons & Inputs:** All primary buttons and text input fields must be fully pill-shaped (rounded-full).
- **Icons:** Use Lucide-style icons with a 2px stroke width and rounded joins/caps to match the typography.

## Components
- **Buttons:** Minimum height of 48px for touch targets. Primary buttons use Terracotta (`#C98C7B`) with white text. Secondary buttons use a transparent background with a 2px Terracotta stroke.
- **Cards:** Mobile cards should have 16px internal padding. On Web, increase padding to 32px to handle denser data tables and charts.
- **Bottom Nav (Mobile):** High-contrast active states using the Primary Terracotta color for the icon and label. Non-active items use Taupe (`#9C857C`).
- **Sidebar (Web):** The sidebar uses a subtle `#F2EAE4` background. Active states are indicated by a "pill" highlight that spans the width of the sidebar minus 16px margins.
- **Inputs:** Floating labels are preferred to save vertical space. The focus state should include a 2px border in Primary Terracotta.
- **Chips/Status:** For medical status (e.g., "Pending", "Confirmed"), use the Primary Terracotta at 10% opacity for the background and 100% opacity for the text.