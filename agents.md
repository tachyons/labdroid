# Navigation Implementation Guide for AI Agents

To ensure consistency and avoid UI bugs like double padding or empty spaces at the top of screens, follow these implementation rules:

## 1. Top Navigation Bar Pattern

`MainActivity` contains a root `Scaffold` that manages the top-level layout and window insets. Most screens should NOT have their own `Scaffold` with a `topBar`.

### Standard Screens (using `DetailTopBar`)
Most screens are automatically handled by `MainActivity` and will show a `DetailTopBar`. No special action is needed in the screen composable.

### Custom Top Bar Screens
If a screen needs a custom `TopAppBar` (e.g., with specific actions like "Clear Chat"):
1. Use `LocalTopBarActions` to provide the top bar content to the root `Scaffold`.
2. Do NOT use a nested `Scaffold` in your screen if possible.
3. If you must use a nested `Scaffold` or `TopAppBar`, set `windowInsets = WindowInsets(0, 0, 0, 0)` to avoid double-padding from status bar insets.

**Example Implementation:**
```kotlin
@Composable
fun MyCustomScreen(onBackClick: () -> Unit) {
    val topBarActions = LocalTopBarActions.current

    LaunchedEffect(Unit) {
        topBarActions.topBar = {
            TopAppBar(
                title = { Text("Custom Title") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    // Your custom actions
                }
            )
        }
    }

    // Use Column or other layout instead of nested Scaffold
    Column(modifier = Modifier.fillMaxSize()) {
        // Screen content
    }
}
```

## 2. Window Insets and Padding

The root `Scaffold` in `MainActivity` provides content padding that already accounts for:
- Status bars (top)
- Navigation bars (bottom)
- The Top Navigation Bar (if present)
- The Bottom Navigation Bar (if present)

Your screen composable receives this padding via the navigation host and should usually start its content from that point.

## 3. Screen Exclusions in `MainActivity`

If a screen handles its top area entirely on its own (like `SearchScreen` with its own `SearchBar`), it should be excluded from the default `DetailTopBar` logic in `MainActivity.kt`:

```kotlin
// In MainActivity.kt
} else if (currentScreen !is Screen.Search && currentScreen !is Screen.DuoChat) {
    DetailTopBar(
        title = (currentScreen as? Screen)?.getTitle() ?: "GitLab",
        onBackClick = { navigator.goBack() }
    )
}
```
