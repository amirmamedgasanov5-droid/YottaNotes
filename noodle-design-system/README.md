# Noodle Design System

Нативная дизайн-система для Android на Jetpack Compose. Стиль: тихий монохром, много воздуха,
мелкий лёгкий шрифт, тончайшие линии, одинаково строгие светлая и тёмная темы.
Принцип «нить»: в покое линия прямая и тонкая, а когда элемент живой (двигается, выбран, фокусирован) — она волнится и пружиной успокаивается. Так работают слайдер, индикатор вкладок, подчёркивание поля, прогресс и шапка (наверху страницы нить волнистая, при прокрутке натягивается). Шрифты: Prata (заголовки) и Inter (интерфейс).
Акцент (янтарь) — только в нити и выделении.

- `noodle/` — библиотека дизайн-системы (`design.noodle`)
- `app/` — демо-приложение со всеми компонентами
- `dist/NoodleDS-3.0.0.apk` — готовая сборка (release, подписана debug-ключом)

## Компоненты
**Фундамент:** цвета (light/dark + 5 акцентов), типографика (Large Title → Caption 2), пружины
(`smooth/snappy/bouncy/gentle/press`), `SquircleShape`, тактильный отклик.

**Навигация:** `NoodleLargeTitleList` (сворачивающийся Large Title), `NoodleNavigationBar`,
`NoodleBackButton`, `NoodleFloatingTabBar` (стеклянная капсула с растягивающейся подсветкой),
`NoodleTabBar` (классический iOS), `NoodleScrollableTabs`.

**Контролы:** `NoodleButton` (Filled/Tinted/Gray/Plain/Destructive × Large/Medium/Small, loading),
`NoodleIconButton`, `NoodleSwitch`, `NoodleSegmentedControl`, `NoodleSlider`, `NoodleStepper`,
`NoodleCheckbox`, `NoodleRadio`, `NoodleChip`, `NoodleTag`, `NoodleTextField`, `NoodleSearchBar`,
`NoodleWheelPicker`.

**Списки:** `NoodleGroup` + `NoodleRow` (inset-группы), `NoodleSwipeToDelete`, `NoodleAvatar`, `NoodleBadge`.

**Оверлеи и обратная связь:** `NoodleBottomSheet`, `NoodleAlert`, `NoodleActionSheet`, `NoodleMenu`,
`NoodleToastHost` (тост-«капля»), `NoodleSpinner`, `NoodleProgressBar`, `NoodleProgressRing`,
`NoodleIndeterminateBar`, `Modifier.skeleton()`, `NoodleBarChart`, `NoodlePageIndicator`.

**Бренд:** `NoodleLogo`, `NoodleWaves`, `NoodleLoader` («нити лапши»).

## Сборка
```
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleRelease
```
