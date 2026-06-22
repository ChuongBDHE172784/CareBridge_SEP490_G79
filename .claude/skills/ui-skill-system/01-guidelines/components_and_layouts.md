# Component Construction Rules

All UI components must be built following these exact specifications to ensure the Warm Claymorphism aesthetic.

## Primary Button
- **Shape**: Fully rounded (`rounded-full` or `radius: 9999px`).
- **Size**: Minimum height `48px` to support Multi-platform fat-finger touch rules (`h-12`).
- **Color**: Primary (`#C98C7B`).
- **Text**: White (`#FFFFFF`), `font-medium` or `font-semibold`.
- **Shadow**: Use shadow-soft or shadow-hover to simulate volume.
- **Padding**: Vertical `12px` (`py-3`), Horizontal `24px` (`px-6`).
- **States**: 
  - Hover: Slightly lift, brighten by using Primary Hover (`#B67868`).
  - Active: Scale down slightly (`scale-95`), apply `inner-pressed` shadow.

## Secondary / Ghost Button
- **Color**: Transparent background or Surface (`#F2EAE4`).
- **Text**: Text Primary (`#5A463F`).
- **Hover**: Background shifts slightly darker (e.g. `bg-[#E8DDD6]`).

## Cards and Modals
- **Shape**: Generously rounded (`rounded-2xl` or `rounded-[32px]`).
- **Background**: Pure white (`bg-[#FFFFFF]`) or Surface (`bg-[#F2EAE4]`) depending on the context container.
- **Padding**: Large internal padding (`p-6` or `p-8`). Let the content breathe.
- **Shadow**: `shadow-soft` for standard cards, `shadow-floating` for modals or important focus elements.
- **Border**: Avoid using borders. Let the shadow distinguish the edge. If absolutely necessary, use `border-border` (`#E8DDD6`).

## Input Fields (Text, Select, Textarea)
- **Shape**: Rounded (`rounded-2xl` or `rounded-xl`).
- **Background**: Soft contrast with the container (`bg-[#F6F1EC]` or `bg-[#F2EAE4]`).
- **Border**: None, or very soft (`border border-[#E8DDD6]`).
- **Focus Ring**: `focus:ring-2 focus:ring-primary/30`. Include smooth transition.
- **Text Color**: Text Primary. Placeholder should be Text Secondary (`#9C857C`).
- **Height**: Minimum `44px` or `48px` to ensure touch friendliness.

## Badges and Tags
- **Shape**: Pill-shaped (`rounded-full`).
- **Background**: Very faint tint of the Primary color (e.g. `bg-primary/10`).
- **Text**: Primary color (`text-primary`), small font (`text-xs` or `text-sm`), uppercase or capitalized.
40: ## Diagonal Ribbon (Corner Badge)
41: - **Concept**: A rotated bar at the top-right or top-left corner of a card to highlight "New", "Pro", or "Popular".
42: - **Positioning**: Due to Large border-radii (`rounded-[32px]`), ribbons must be offset.
43: - **Strict Classes**: `absolute top-6 -right-10 bg-primary text-white text-[10px] px-10 py-1 font-bold uppercase rotate-45 shadow-sm`.
44: - **Safety Rule**: The parent container **must** have `overflow-hidden`.
45: 
46: ## Media Hygiene (Images & Videos)
47: - **Rounding**: ALL media must be rounded. `rounded-2xl` (24px) or `rounded-[32px]` (32px) depending on card size.
48: - **Shadows**: Large media should have a `shadow-soft`.
49: - **Containers**: If media is inside a card, ensure the card has `overflow-hidden` or the media itself matches the card's radius.
50: 
51: ## UI Interaction Components
52: - **Dropdowns**: Floating white cards (`shadow-floating`), `rounded-[32px]`. Menu items must have `rounded-xl` hover states (`bg-bg`).
53: - **Toast Notifications**: Pill-shaped (`rounded-full`), `bg-[#5A463F]`, `text-white`, floating `24px` from bottom/right.
54: - **Calendars**: 7-column grid. Days are `w-10 h-10` or larger, `rounded-xl`. Selected day is `bg-primary`, `text-white`, `rounded-full`.
55: 
56: # Interaction Patterns
57: 
The Warm Clay UI must feel alive, physical, and highly responsive. Think of UI elements as soft, physical clay objects.

## 1. Hover Lifting
Cards, lists items, and important buttons should visibly respond when a mouse hovers over them.
- Translate up by `2px` to `4px` (`-translate-y-1`).
- Increase the drop shadow intensity (`shadow-hover`).
- Include a transition: `transition-all duration-300 ease-out`.

## 2. Pressed / Active States (The "Squish" Effect)
When a user clicks on an interactive element, it should feel like pressing into soft clay.
- Scale down slightly: `scale-[0.98]` for cards, `scale-95` for buttons.
- Reduce drop shadow to 0, and add an **inner shadow** (`inner-pressed`).

## 3. Smooth Reveals
When a modal opens, or a section expands:
- Do not make it instantly appear.
- Use a fade-in and slide-up combination (e.g. `opacity-0 translate-y-4` resolving to `opacity-100 translate-y-0`).
- Use `duration-300` or `duration-500` with an `ease-out` timing function.

## 4. Toast Notifications
- Should float in from the bottom with a `shadow-floating`.
- Pill-shaped (`rounded-full`) or highly rounded (`rounded-2xl`).
- High contrast against the background (e.g. `bg-[#5A463F] text-white`).# Layout Patterns & Cross-Platform

Standardize how the canvas is organized based on application type. The `bg-bg` (`#F6F1EC`) is the base upon which floating white (`#FFFFFF`) containers are placed.

## 1. Cross-Platform Responsive Master Rules
- **Desktop (Web, SaaS)**: Uses `flex-row` with persistent floating sidebars and Top Navbars.
- **Mobile (iOS, Android App style)**: Sidebars are hidden (`hidden md:flex`). Instead, use Native-style **Bottom Navigation Bars** (absolute/fixed rounded pill at the bottom edge) to give it a native app feel.
- **Mobile Flex Flow Prevention**: To prevent text squishing or overlapping icons on mobile, use `flex-col md:flex-row`.
- **Icon / Text Overlap Fix**: Always use `shrink-0` on action icons (like Bell or Search) and `min-w-0` + `truncate` on adjacent text/inputs so text gracefully cuts off with ellipsis instead of squeezing the layout.
- **Mobile Buttons**: Never force `w-full` on mobile buttons if they only contain a few words. Use `w-auto self-start` so they remain elegant pills.

## 2. Modern Dashboard (B2B SaaS)
- **Background**: The entire screen is `#F6F1EC`.
- **Sidebar**: A floating white rounded card (`rounded-3xl`), separated by `24px` from the screen edges. Not touching the edge!
- **Main Area**: Also consists of one or multiple floating white cards.

## 3. Mobile App (Consumer iOS/Android feel)
- **Background**: `#F6F1EC`.
- **Header**: Soft typography on the background.
- **Cards**: Full width minus `16px` padding on each side, extremely rounded (`rounded-[32px]`).
- **Bottom Navigation**: Floating pill (`rounded-full` or `rounded-[3xl]`) hovered `16px` above the bottom edge, fixed position `z-50`. Uses icons with active primary color states.

## 3. Web Tool / Form Flow
- **Background**: `#F6F1EC`.
- **Card container**: A single centered white card (`rounded-[32px]`, max-width `600px`).
- **Shadow**: `shadow-floating`.
- **Inside**: Generously spaced inputs, stacked vertically.

## General Spacing Rule
Avoid dense layouts. Use `gap-6` or `gap-8` between major sections, and `gap-4` for minor relative items.