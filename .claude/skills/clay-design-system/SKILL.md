---
name: clay-design-system
description: >
  Apply the Warm Claymorphism UI/UX design language (earthy terracotta/beige colors, rounded elements, floating layouts, soft 3D claymorphism shadows) to web (HTML/React/Vue) or mobile (Flutter/SwiftUI/Jetpack Compose) applications. Trigger this skill whenever the user asks to design, style, redesign, improve, or modify the UI/UX according to 'ui-skill-system', 'claymorphism', 'warm design', 'warm claymorphism', or requests a beautiful, soft, modern, or premium user interface.
---

# Warm Claymorphism UI Skill System

This skill enforces a consistent, premium, 3D, and warm "Claymorphism" UI/UX design language across all generating AI agents. It ensures interfaces are soft, tactile, inviting, and highly polished, rather than generic, sharp, or cold.

## Critical Mandate: Protect All Business Logic (Do No Harm)

When modifying existing files, views, components, or pages:
1. **NEVER** modify, remove, or alter state management (`useState`, `useReducer`, Redux, etc.).
2. **NEVER** modify API calls, `useEffect` hooks, or data fetching logic.
3. **NEVER** break `onClick`, `onChange`, or any behavioral handlers.
4. **NEVER** rename variables or change the logical structure of the data.
5. **ONLY** alter the markup structure, CSS classes, and layout wrappers to match the design system.

---

## 🎨 Design System Tokens

### Color Palette
- **Background**: `#F6F1EC` (Use `bg-[#F6F1EC]` or platform equivalent)
- **Surface (Cards/Containers)**: `#FFFFFF` (white) or `#F2EAE4` (for nested surfaces)
- **Primary Accent**: `#C98C7B` (Use `bg-[#C98C7B] text-white`). Hover: `#B67868`.
- **Text**: Primary `#5A463F`, Secondary `#9C857C`.
- **Avoid**: `#000000` (pure black) entirely. Use deep brown `#5A463F` instead.

### Borders & Rounding
- **Main Cards/Containers**: `rounded-2xl` or `rounded-[32px]`.
- **Buttons, Tags, Badges, Chips**: `rounded-full` (Pill shape ONLY). Never use `rounded-2xl` or `rounded-xl` for buttons.
- **Inputs**: `rounded-xl` minimum.

### Shadows & Depth
- **Floating Surface**: `shadow-[0_12px_32px_rgba(90,70,63,0.06)]` (soft warm shadow).
- **Interactive Hover**: Use `hover:-translate-y-1 hover:shadow-hover duration-500` (long transition duration for premium feel).
- **Interactive Click**: Use `active:scale-95 transition-all duration-300`.
- **Inner Depth (for inputs/active states)**: `shadow-[inset_0_4px_8px_rgba(90,70,63,0.05)]`.

---

## 🧱 Component Styling Guidelines

### Typography & Legibility
- **Font**: Use a friendly, rounded sans-serif like **Quicksand** or **Nunito** (`font-sans`).
- **Body Text**: Use `text-base` (16px) for standard body text. Avoid `text-sm` for important readouts.
- **Secondary Text**: Use `text-sm` ONLY for metadata or non-critical info. **Never use `text-xs` for readable sentences.**
- **No Italics**: Avoid the use of `italic` styles to maintain clean layout rendering.
- **Weight**: Use `font-black` (900) for headers/call-to-actions. Use `font-semibold` (600) or `font-bold` (700) for body lists/sentences to prevent "blobby" rendering.

### Icons
- **No emojis** in main headers/titles.
- Place a Lucide icon inside a soft rounded wrapper next to the title instead:
  ```html
  <div class="w-12 h-12 bg-[#C98C7B]/15 text-[#C98C7B] rounded-full flex items-center justify-center shrink-0">
    <i data-lucide="icon-name"></i>
  </div>
  ```

### Inputs & Forms
- **Height**: Minimum `h-12` or `py-3` for generous touch targets.
- **Form Labels**: Use `text-[10px] md:text-xs font-black uppercase tracking-widest px-2 mb-2 text-[#9C857C]`.
- **Styling**: `bg-[#F6F1EC] rounded-full p-4 font-bold border-2 border-transparent focus:border-[#C98C7B]/20 focus:ring-4 focus:ring-[#C98C7B]/5`.

### Horizontal Scrolling (The "Story" Pattern)
- **Clipping Prevention**: Never let horizontal scroll containers clip shadows. Use `px-8` and `py-10` on the container to give shadows breathing room.
- **Ghost Spacing**: Always add a final empty `<div class="w-8 shrink-0"></div>` after the last item to prevent it from sticking to the right edge.
- **Snap & Feel**: Use `snap-x` on the container and `snap-start` or `snap-center` on items.
- **Scrollbar**: Hide browser scrollbars using the `.hide-scrollbar` utility.
- **Layout**: Use `flex overflow-x-auto gap-6`. Never use `grid` for horizontal scrolling.

### Layout & Flexbox Hygiene (Anti-Bug Rules)
- **Explicit Flex Declaration**: Always declare the display class `flex` or `inline-flex` explicitly on any container that uses flex layout directives (such as `flex-col`, `flex-row`, `gap-x`, `gap-y`, `items-center`, `justify-between`). Declaring layout direction/gap without declaring `flex` prevents the browser from formatting layout items and gaps correctly.
- **Toggled Hidden Elements**: When toggling visibility using the `hidden` class on a flex container (e.g. dynamic menus, drawers, speed-dials), define the default display style explicitly alongside it: `class="hidden flex flex-col gap-2..."`. This ensures that when the `hidden` class is removed, the container correctly defaults back to a flex layout.

### 🚫 Common AI Layout Pitfalls & How to Avoid Them
- **Shadow Clipping via `overflow-hidden`**:
  * *Pitfall*: Placing `overflow-hidden` and `shadow-[...]` on the same card wrapper. This clips the card's outer shadow completely.
  * *Fix*: Keep the card container shadow-friendly (no `overflow-hidden`). If you need to crop image/content corners, wrap the content inside an inner child container that has `overflow-hidden` and matches the parent's corner radius.
- **Squashed Icons in Flex Container**:
  * *Pitfall*: Placing icons or visual indicators inside horizontal flex containers without setting fixed sizes. When text content expands, the icons squash.
  * *Fix*: Always apply `shrink-0` (or `flex-shrink-0`) to all icon wrappers, badges, or visual avatars inside flex layouts.
- **Flex Parent Text Overflow**:
  * *Pitfall*: Long titles/emails inside list items breaking flex rows or pushing elements off-screen.
  * *Fix*: Always add `min-w-0` to the text container wrapper inside the flex layout to allow the child texts to safely `truncate` or `line-clamp`.
- **Layout Shifts on Dynamic Borders**:
  * *Pitfall*: Adding borders on hover or focus (e.g., changing from no border to `border-2 border-primary`), causing elements to shift by 1-2px and trigger layout jitter.
  * *Fix*: Start with a transparent border by default (`border-2 border-transparent`) and only change the color on state updates (`hover:border-primary`).
- **Parent Z-Index Isolation**:
  * *Pitfall*: Giving an absolute dropdown/popover `z-50` but its parent container is set to `relative z-0`. The dropdown will be rendered *behind* neighboring cards that have standard z-index or stack hierarchy.
  * *Fix*: Ensure the parent container of any absolute popover/dropdown is given an appropriate high-level z-index or resides outside layout isolation layers.
- **Visual Focus Rings (A11y)**:
  * *Pitfall*: Removing default focus outlines (`focus:outline-none`) for clean aesthetics without providing a keyboard focus alternative.
  * *Fix*: Always pair outline removal with a soft aesthetic focus ring: `focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20`.

---

## 🧩 Comprehensive Component Specifications

Ensure EVERY user interface element is rendered according to these strict styling guidelines:

### 1. Nhóm hiển thị từ cạnh màn hình (Edge-anchored Panels)
* **Bottom Sheet / Action Sheet**:
  - *Layout & Corners*: Slides up from the bottom. Top corners must be heavily rounded (`rounded-t-[32px]`).
  - *Details*: Must have a center handle pill: `<div class="w-12 h-1.5 bg-[#E8DDD6] rounded-full mx-auto my-3 shrink-0"></div>`.
  - *Background*: Soft white surface (`bg-white shadow-[0_-12px_32px_rgba(90,70,63,0.08)]`).
* **Drawer / Sidebar / Off-canvas Menu**:
  - *Layout & Corners*: Trims side interface. Corners facing viewport must be rounded (`rounded-r-[32px]` or `rounded-l-[32px]`).
  - *Typography*: Active nav items must be rounded-full pills (`bg-[#C98C7B]/10 text-[#C98C7B] font-bold px-4 py-3`). Inactive items remain `#5A463F` on transparent backgrounds with smooth hover states.

### 2. Nhóm Phản hồi & Cảnh báo (Feedback & Notification)
* **Toast Notification & Snackbar**:
  - *Design*: Pill-shaped floating overlay (`rounded-full shadow-[0_16px_36px_rgba(90,70,63,0.15)]`).
  - *Color Palette*: Use a dark earthy background (`bg-[#5A463F] text-white`) or terracotta (`bg-[#C98C7B] text-white`).
  - *Action Buttons (Snackbar)*: Must be a pill inside the toast with high contrast text (e.g., `bg-white/20 hover:bg-white/30 text-white rounded-full px-3 py-1 text-sm font-black active:scale-95 transition-all`).
* **Tooltip**:
  - *Design*: Small floating capsule (`rounded-full bg-[#5A463F] text-[#F6F1EC] text-xs px-3 py-1 shadow-md font-medium`).
* **Modal / Dialog / Alert / Popup**:
  - *Backdrop*: Semi-transparent warm dark screen dimming (`bg-[#5A463F]/40 backdrop-blur-sm`).
  - *Container*: Center floating cards (`bg-white rounded-[32px] p-8 max-w-md w-full shadow-[0_24px_48px_rgba(90,70,63,0.12)] border border-[#E8DDD6]/50`).
* **Popover / Context Menu**:
  - *Container*: Small hovering cards (`bg-white rounded-2xl border border-[#E8DDD6]/50 p-2 shadow-lg z-50`).
  - *Items*: Actions inside popover use rounded-lg hover states (`rounded-xl hover:bg-[#F6F1EC] text-[#5A463F] transition-all px-4 py-2 font-semibold`).
* **Lightbox**:
  - *Design*: Media wrapped in a thick frame (`bg-white p-4 rounded-[32px] shadow-2xl`).
  - *Close Button*: Pill/Circle floating at top-right (`rounded-full bg-white text-[#5A463F] shadow-md hover:scale-105 active:scale-95`).
* **Banner / Inline Message / Flash Notice**:
  - *Container*: Rounded box (`bg-[#F2EAE4] border-l-4 border-[#C98C7B] p-4 rounded-2xl flex items-center gap-3`).
* **Badge**:
  - *Design*: Perfectly circular (`w-5 h-5 rounded-full flex items-center justify-center bg-[#C98C7B] text-white text-[10px] font-black border-2 border-white`).
* **Skeleton Loader (Shimmer Effect)**:
  - *Design*: Rounded block structures (`rounded-2xl bg-[#E8DDD6] animate-pulse`). For high fidelity, use a linear gradient animation sweep (`bg-gradient-to-r from-[#E8DDD6] via-[#F2EAE4] to-[#E8DDD6] bg-[length:200%_100%] animate-shimmer`).

### 3. Nhóm Hướng dẫn & Tương tác tại chỗ (Context & Guidance)
* **Coachmark / Onboarding Tour**:
  - *Focus Area*: Use a cutout hole effect using box-shadow (`ring-[9999px] ring-[#5A463F]/60`).
  - *Hint Card*: Floating white box (`bg-white rounded-[24px] p-6 shadow-2xl relative border-2 border-[#C98C7B]/30`).
* **Dropdown / Select**:
  - *Design*: Interactive input container (`h-12 bg-white border border-[#E8DDD6] rounded-xl px-4 flex items-center justify-between cursor-pointer`). Dropdown options overlay matches the Popover spec.
* **Accordion / Collapsible Panel**:
  - *Trigger Header*: Rounded pill or card block (`bg-[#F2EAE4] hover:bg-[#E8DDD6] rounded-2xl p-4 flex justify-between items-center cursor-pointer transition-all duration-300`).
  - *Body*: Expandable section using height transitions (`transition-[max-height] duration-500 overflow-hidden`).
* **Speed Dial (Expandable FAB)**:
  - *Design*: Main floating button opens a stack of smaller sub-FABs (`w-10 h-10 bg-[#5A463F] text-white rounded-full flex items-center justify-center hover:-translate-y-0.5 active:scale-95 transition-all shadow-md mb-2`).

### 4. Nhóm Điều hướng & Trải nghiệm Cuộn (Scroll & Navigation)
* **Sliver (SliverAppBar, SliverList, SliverGrid / Collapsing Header)**:
  - *Visual Transition*: On scroll down, header container background shifts smoothly from transparent into a sticky panel (`bg-[#F6F1EC]/85 backdrop-blur-md border-b border-[#E8DDD6]/50`).
* **RefreshIndicator (Pull-to-refresh)**:
  - *Indicator Icon*: Spinning arrow indicator inside a floating circular disc (`w-10 h-10 bg-white text-[#C98C7B] rounded-full shadow-[0_6px_20px_rgba(90,70,63,0.1)] flex items-center justify-center`).
* **TabBar & TabBarView**:
  - *Track Bar*: Pill container (`bg-[#F2EAE4] p-1.5 rounded-full flex gap-1`).
  - *Tab Item*: Activated state is a white pill (`bg-white text-[#5A463F] rounded-full shadow-sm px-5 py-2 font-bold transition-all`). Inactive state is quiet (`text-[#9C857C] hover:text-[#5A463F] px-5 py-2`).
* **PageView & PageIndicator**:
  - *Indicator Dots*: Active dot animates width into a soft pill (`bg-[#C98C7B] w-6 h-2 rounded-full transition-all duration-300`). Inactive dots are small circles (`bg-[#E8DDD6] w-2 h-2 rounded-full`).

### 5. Nhóm Tương tác Cử chỉ (Gestures & Micro-interactions)
* **Dismissible (Swipe-to-action)**:
  - *Behind Panel*: Swiping cards reveals functional backing triggers. Rounded corners MUST align with swiped item (`rounded-[32px]`).
  - *Actions*: Delete is a soft crimson (`bg-[#D16E61] text-white`), archive is warm yellow-brown (`bg-[#D9A05B] text-white`).
* **Hero Animation**:
  - Maintain absolute continuity. Card layouts transitions must animate boundaries smoothly between component screens.
* **DraggableScrollableSheet**:
  - Extends Bottom Sheet design with continuous vertical translation coordinates mapping.

### 6. Nhóm Lọc & Nhập liệu (Input & Selection)
* **Chips (ActionChip, FilterChip, ChoiceChip)**:
  - *Design*: Pill shape (`rounded-full px-4 py-1.5 text-sm font-semibold border-2 transition-all duration-300 active:scale-95`).
  - *Selected*: `bg-[#C98C7B] text-white border-transparent shadow-sm`.
  - *Unselected*: `bg-[#F2EAE4] text-[#5A463F] border-[#E8DDD6]/50 hover:bg-[#E8DDD6]`.
* **Segmented Control / CupertinoToggle**:
  - *Track*: Enclosing capsule (`bg-[#F2EAE4] p-1 rounded-full flex`).
  - *Items*: Pill buttons (`rounded-full text-center text-sm font-black text-[#9C857C] py-2 px-4 flex-1 transition-all active:scale-95`). Active option uses `bg-white text-[#5A463F] shadow-sm`.
* **Stepper**:
  - *Steps*: Circles (`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm shrink-0`).
  - *Active*: `bg-[#C98C7B] text-white ring-4 ring-[#C98C7B]/20`. Completed: `bg-[#5A463F] text-white`. Inactive: `bg-[#E8DDD6] text-[#9C857C]`.
  - *Lines*: Connectors must be thick bars (`h-1 bg-[#E8DDD6] rounded-full mx-2`).

### 7. Nhóm Hành động & Trạng thái (Actions & States)
* **Backdrop**:
  - Rear pane houses settings or filters (`bg-[#F2EAE4] p-6`). Front layer houses main content, maintaining `rounded-t-[32px]` and slides downward on menu actions.
* **Floating Action Button (FAB)**:
  - *Design*: Prominent circular trigger floating over view (`w-14 h-14 bg-[#C98C7B] hover:bg-[#B67868] active:scale-90 text-white rounded-full flex items-center justify-center shadow-[0_8px_24px_rgba(201,140,123,0.3)] fixed bottom-6 right-6 z-40 transition-all duration-300`).

---

## 💻 Multi-Platform Implementation Rules

### 1. React TypeScript (Tailwind CSS)
- **Main Container**:
  ```tsx
  <div className="min-h-screen w-full bg-[#F6F1EC] text-[#5A463F] font-sans overflow-hidden md:py-8 p-4 md:p-10">
  ```
- **Cards**:
  ```tsx
  <div className="bg-white rounded-[32px] shadow-[0_12px_32px_rgba(90,70,63,0.06)] border border-[#E8DDD6]/50 p-6 md:p-8">
  ```
- **Buttons**:
  ```tsx
  <button className="h-12 rounded-full bg-[#C98C7B] text-white font-semibold py-3 px-6 shadow-[0_8px_24px_rgba(90,70,63,0.08)] transition-all duration-300 hover:bg-[#B67868] hover:-translate-y-0.5 active:scale-95">
  ```

### 2. Flutter
- Use custom colors: `ClayColors.accent = Color(0xFFC98C7B); ClayColors.bg = Color(0xFFF6F1EC);`
- Apply `BoxDecoration` with multiple offset shadow layers to mimic 3D depth.
- Use `GoogleFonts.quicksand()` with heavy weight structures.

### 3. iOS (SwiftUI)
- Use custom modifiers like `.clayCard()` and `.clayShadow()` to keep shadow offsets and blurs consistent with terracotta aesthetics.
