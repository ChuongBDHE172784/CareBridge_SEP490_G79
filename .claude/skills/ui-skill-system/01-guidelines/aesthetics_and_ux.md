# Design Principles: Warm Claymorphism

This system adopts a stunning, premium "Warm Claymorphism" visual language. 
All UI designs must strictly adhere to these core principles to maintain a 3D, cozy, and inviting aesthetic.

## 1. Warm & Earthy Palette
Avoid highly saturated or cold colors (no neon blues, bright reds, or pure greens).
Prioritize a warm, calming mood using beige, terracotta, and soft brown tones. The background is slightly beige, while cards and floating elements are clean white or off-white.

## 2. Abundant Roundness (Soft Geometry)
No sharp corners. Everything must be smooth and rounded.
- Buttons and chips use full-rounded pills (`rounded-full`).
- Cards, panels, and modals use large radii (`rounded-2xl` to `rounded-[32px]`).
- Even inner elements (like images or charts inside cards) should have generous border-radii (`rounded-xl`).

## 3. 3D Volume & Claymorphism
Design elements should feel like they are made of soft, matte clay floating in space.
- Use soft, layered drop shadows to create a sense of hovering (e.g., `box-shadow: 0 12px 32px rgba(90, 70, 63, 0.08)`).
- Avoid harsh borders. If borders are necessary, they should be extremely subtle (`#E8DDD6`).
- Opt for soft inner shadows or highlights on active states to create a "pressed into clay" effect.

## 4. Calm & Friendly Typography
Typography should mirror the roundness and warmth of the shapes.
- Use friendly, modern sans-serif fonts with soft terminals (e.g., Quicksand, Nunito, Roboto, or Inter with tracking adjusted).
- Text colors are deep earthy browns (`#5A463F`) rather than pure blacks (`#000000`).

## 5. Spacer & Cross-Platform Responsive Layout
- **Mobile-First & Native Feel**: The UI MUST be flawlessly responsive. Mobile UI (iOS/Android) should mimic Native Apps (e.g., sticky bottom navigation, swipeable sheets), while Web versions use standard Sidebars.
- Elements need room to cast their soft shadows. Use generous padding inside cards (`p-6`, `p-8`) on desktop, and comfortably tight (`p-4`) on mobile.
- **Touch Targets**: All interactive elements MUST be at least `48px` tall (or wide) to ensure cross-platform accessibility.

## 6. Lively Animations & Micro-Interactions (The "Soul")
Do not let the UI feel static. It needs bouncy, lively animations:
- **Floating**: Avatars, floating buttons, or decorative 3D elements should have a continuous `animate-float`.
- **Bouncy Hover (group-hover)**: When hovering over a card, any nested icon should playfully scale up or bounce (`group-hover:scale-110 transition-transform`).
- **Squish State**: Buttons must press deeply like clay (`active:scale-95`).
# Universal UI System: Architecture & Specs

The Warm Clay UI Skill System is a robust methodology that scales across B2B SaaS, mobile apps, dashboards, fintech, and AI tools.

## Foundation

### The "Clay Canvas"
The root page background should always be the warm canvas color (`bg-[#F6F1EC]`).
Content sections, cards, and modal windows sit on top of this canvas, typically in opaque white (`bg-[#FFFFFF]`) or surface color (`bg-[#F2EAE4]`), elevated by floating soft shadows.

### Core Layout Max-Widths
- **Dashboards / SaaS App**: Fluid with sidebars, max-width `1440px` for the content area.
- **Websites / Landing Pages**: Centered, max-width `1200px`.
- **Modals / Dialogs**: Small (`400px`), Medium (`600px`), Large (`800px`). Custom width constraints should use Tailwind max-w classes.

## Global Specs

### Border Radius Hierarchy
- **sm (8px)**: Only for extremely small inner items, tags, or tiny UI controls.
- **md (16px)**: Nested containers, input fields, dropdown menus.
- **lg (24px)**: Primary cards, sidebars, dashboard panels.
- **xl (32px)**: Large feature cards, hero sections, modal dialogs.
- **full (9999px)**: Buttons, pills, avatars, and badges.

### Elevations (Shadows)
Our design relies heavily on shadows to distinguish hierarchy instead of borders.
- **Soft Floating (Default Card)**: A large, diffuse shadow tinted slightly brown to match the environment. `box-shadow: 0 12px 32px rgba(90, 70, 63, 0.06);`
- **Hover/Active (Elevated)**: A deeper, larger offset shadow to mimic an element moving closer to the user.
- **Pressed (Inner Clay)**: An inner shadow for when a button is clicked or a toggle is active.

### Micro-interactions
Components should feel tactile.
- Buttons should subtly shrink (`scale-95`) or depress (inner shadow) on click.
- Hovering over a card should softly lift it up (`-translate-y-1` and shadow expansion).
- All transitions should be smooth (`transition-all duration-300 ease-out`).

## Rule of Rejection
If an AI generates an interface featuring sharp corners, harsh `#000` text, flat saturated colors, or heavy solid borders, it **violates** this Universal UI System and must self-correct to the Warm Clay aesthetic.
# UX Rules & Interaction

## 1. Joyful, Tactile & Animated Interactions
Every interaction should feel like manipulating high-quality, soft, physical objects.
- **Hover Lifting & Bouncing**: Cards should lift (`-translate-y-1` or `-translate-y-2`). Inner icons inside cards should scale playfully (`group-hover:scale-110`).
- **Active/Pressed States**: Objects should depress or sink (`scale-95` + `shadow-inner-pressed`).
- **Loading States**: Use gentle pulse animations or animated skeleton loaders in the warm surface color (`bg-[#E8DDD6]`).
- **Continuous Animations**: Key 3D or visual items should idle animate (e.g., `animate-float`).

## 2. Warm & Forgiving Forms
- Labels should be placed above inputs with clear typography (`text-sm font-medium text-[#5A463F]`).
- Input fields should be fully rounded (`rounded-2xl`) or highly rounded, using a soft background (`bg-[#FFFFFF]` or `bg-[#F2EAE4]`) and no harsh borders.
- **Focus State**: When an input is focused, surround it with a soft glowing ring using the Primary color (`ring-4 ring-[#C98C7B]/30`).
- **Validation/Errors**: Avoid angry red. Use a softer coral or warm orange for errors, accompanied by a clear, human-readable error message.

## 3. State Awareness
- Empty states should be friendly, ideally featuring a 3D claymorphic illustration and a clear call-to-action out of the empty state.
- Navigation should always highlight the active tab or active page using a soft pill background in the primary color, with white text.

## 4. Accessibility with Softness
Even though the shadows and colors are soft, contrast must be maintained.
- Ensure text on the primary button (`#C98C7B`) is `#FFFFFF`.
- Secondary text (`#9C857C`) must still be readable against the background (`#F6F1EC`).
- Touch targets on mobile must be large and forgiving (minimum `48px` height for buttons and inputs).
