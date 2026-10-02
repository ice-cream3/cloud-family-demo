# Mobile Frontend UI Design

This directory stores the UI design references used by `mobile-frontend`.

## Asset Inventory

| ID | File | Source | Page Scope | Implemented In | Notes |
| --- | --- | --- | --- | --- | --- |
| UI-001 | [assets/mobile-app-screens.png](assets/mobile-app-screens.png) | Chat attachment, 2026-10-02 | Home, tools, image compression, activity, messages, profile, history, settings, more tools sheet | `src/components/HomePage.tsx`, `src/styles/app.css` | Main mobile tool app reference. |
| UI-002 | [assets/mobile-auth-screens.png](assets/mobile-auth-screens.png) | Chat attachment, 2026-10-02 | Splash/onboarding, login, phone register, email register, quick login, forgot password, verification, success states | `src/components/LoginPage.tsx`, `src/styles/app.css` | Current implementation covers login/register visual style and core auth flow. |

## Current Implementation Mapping

The current React implementation is code-driven rather than image-sliced. The design images are used as visual references for layout, spacing, color, and component hierarchy.

| Design Area | Current Route/State | Component |
| --- | --- | --- |
| Login page | No session | `LoginPage` |
| Phone/email register | `LoginPage` register mode | `LoginPage` |
| Home tab | `screen === "home"` | `HomePage` |
| Tool center | `screen === "tools"` | `HomePage` |
| Image compression | `screen === "compress"` | `HomePage` |
| Activity center | `screen === "activity"` | `HomePage` |
| Messages | `screen === "message"` | `HomePage` |
| Profile | `screen === "profile"` | `HomePage` |
| History | `screen === "history"` | `HomePage` |
| Settings | `screen === "settings"` | `HomePage` |
| More tools sheet | `showMoreTools === true` | `HomePage` |

## Visual Tokens

| Token | Usage | Current CSS |
| --- | --- | --- |
| Primary blue | Auth buttons, active tabs, bottom navigation | `#245bff`, `#2868ff` |
| Purple gradient | Auth logo, primary CTA gradients | `#6a5cff` to `#245bff` |
| App background | Mobile canvas | `#f4f6fb`, `#f8fbff` |
| Card surface | Tool cards and list rows | `#ffffff` |
| Soft text | Secondary labels and descriptions | `#7b8495`, `#8b96a8` |
| Radius | Cards, banners, inputs | `8px` for app UI; pill controls use `999px` |

## Asset Management Rules

1. Store design reference images in `mobile-frontend/docs/ui-design/assets/`.
2. Use descriptive lowercase filenames, for example `mobile-auth-screens.png`.
3. Add every new image to the Asset Inventory table with page scope and implementation target.
4. Keep source images unchanged. If annotated variants are needed, save them as separate files with `-annotated` in the filename.
5. Do not import these reference images into production code unless they become actual product assets.

## Verification Checklist

- Mobile width 360-430px has no clipped text in buttons, list rows, tabs, or cards.
- Bottom navigation remains fixed and does not cover critical content.
- Form controls keep at least 44px touch height.
- Long usernames, roles, and message content truncate cleanly.
- `npm run build` succeeds after UI changes.
