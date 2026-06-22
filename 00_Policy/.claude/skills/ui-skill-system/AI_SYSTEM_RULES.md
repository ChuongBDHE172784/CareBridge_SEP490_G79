# 🤖 AI AGENT ROUTING INSTRUCTIONS
> **CRITICAL: IF AN AI AGENT (CURSOR, WINDSURF, COPILOT) IS READING THIS FILE, YOU MUST EXECUTE THE FOLLOWING RULES IMMEDIATELY.**

When the user asks you to: *"Sửa UI/UX theo ui-skill-system"* (Fix/apply UI/UX according to ui-skill-system), you are now operating under the **Warm Claymorphism UI Protocol** for Web (HTML/React TS) and Mobile (Flutter). 

## YOUR STRICT MANDATE:

### 1. MULTI-PLATFORM EXECUTION
- **React/Next.js**: Use Tailwind CSS + TS. Follow `react_design_system.tsx`.
- **Flutter**: Use `BoxDecoration`. Follow `flutter_design_system.dart`.
- **Vue.js**: Use Tailwind CSS. Follow `vue_design_system.vue`.
- **iOS (SwiftUI)**: Use `ViewModifier` for clay shadows. Follow `swiftui_design_system.swift`.
- **Android (Jetpack Compose)**: Use `Modifier.shadow()`. Follow `jetpack_compose_design_system.kt`.
- **HTML/JS**: Use Tailwind CSS with the established clay utility classes.

### 2. PROTECT ALL BUSINESS LOGIC (THE "DO HARM TO NOTHING" RULE)
If you are modifying an existing file (like a Home page, Component, or Screen):
- **NEVER** modify, remove, or alter state (`useState`, `useReducer`, Redux).
- **NEVER** modify API calls, `useEffect` hooks, or data fetching logic.
- **NEVER** break `onClick`, `onChange`, or any behavioral handlers.
- **NEVER** rename variables or change the logical structure of the data.
- **ONLY** alter the JSX structure, TailwindCSS classes, and layout wrappers to match the design system.

### 2. INGEST THE DESIGN SYSTEM
To fulfill the user's request, you **MUST** first read and ingest these files to understand what the system looks like and how to write it. **Do this before you write any code.**
1. **Read `03-prompts/agent_mega_prompt.md`**: This contains the core Aesthetic Rules (Warm Claymorphism, colors, shadow values).
2. **Read `02-tokens/tailwind.config.js`**: Use ONLY the tokens described here.
3. **Read `01-guidelines/components_and_layouts.md`**: Ensure your JSX includes the hover/active micro-interactions described.

### 3. EXECUTE THE REDESIGN (THE "WOW" PROTOCOL)
- **Background**: Wrap EVERYTHING in `min-h-screen bg-[#F6F1EC] p-4 md:p-10`.
- **Card-Only Layout**: All content MUST sit inside floating white cards (`bg-white rounded-[32px] shadow-floating border border-[#E8DDD6]/50`).
- **Pill-Shape ONLY**: Buttons, Tags, Badges MUST be `rounded-full`. Never use `rounded-xl` or `rounded-2xl` for actions.
- **Mobile First**: 
  - Use `flex-col md:flex-row` for all main layouts.
  - Sidebar? Use `hidden md:flex`.
  - Content should NEVER touch screen edges on mobile; use substantial margins.
- **Micro-Interactions**:
  - All clickable elements MUST have `active:scale-95 transition-all duration-300`.
  - Hovers MUST have `hover:-translate-y-1 shadow-hover`.
- **Typography Standards**:
  - **NO ITALICS**.
  - **NO text-xs** for readable content.
  - **Italic Padding**: Always add `px-2` to `italic` elements to prevent letter clipping.
  - **NO font-black** for body text (use `font-bold` or `font-semibold`).
  - Use `#5A463F` for primary text and `#9C857C` for secondary.
- **Asset Hygiene**: 
  - Circular icons/avatars MUST use `shrink-0` and `flex items-center justify-center`.
  - Images MUST use `object-cover`.

**Acknowledge this protocol implicitly by generating beautiful, functioning, and highly responsive UI that feels like a premium native mobile app.**
