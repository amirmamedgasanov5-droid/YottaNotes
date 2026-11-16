package design.noodle.showcase.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.components.NoodleAvatar
import design.noodle.components.NoodleBadge
import design.noodle.components.NoodleGroup
import design.noodle.components.NoodleIconButton
import design.noodle.components.NoodleLargeTitleList
import design.noodle.components.NoodleMenu
import design.noodle.components.NoodleMenuItem
import design.noodle.components.NoodleRow
import design.noodle.components.NoodleScrollableTabs
import design.noodle.components.NoodleSearchBar
import design.noodle.components.NoodleSwipeToDelete
import design.noodle.components.NoodleTag
import design.noodle.components.NoodleText
import design.noodle.components.NoodleToastTone
import design.noodle.showcase.ShowcaseState
import design.noodle.theme.NoodleTheme

private class Contact(val id: Int, val name: String, val role: String, val group: Int, val online: Boolean, val unread: Int)

private val seed = listOf(
    Contact(1, "Анна Смирнова", "Дизайнер продукта", 2, true, 2),
    Contact(2, "Иван Петров", "iOS разработчик", 2, true, 0),
    Contact(3, "Мария Ким", "Руководитель студии", 2, false, 1),
    Contact(4, "Олег Громов", "Лучший друг", 1, true, 0),
    Contact(5, "Света Орлова", "Сестра", 3, false, 0),
    Contact(6, "Даниил Соколов", "Android разработчик", 2, true, 0),
    Contact(7, "Полина Белова", "Подруга", 1, false, 4),
    Contact(8, "Мама", "Семья", 3, true, 0),
)

@Composable
fun ListsScreen(state: LazyListState, ui: ShowcaseState, bottomPad: Dp) {
    val colors = NoodleTheme.colors
    val contacts = remember { mutableStateListOf<Contact>().apply { addAll(seed) } }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }

    val filtered = contacts.filter {
        (tab == 0 || it.group == tab) && (query.isBlank() || it.name.contains(query, ignoreCase = true))
    }

    NoodleLargeTitleList(
        title = "Списки",
        state = state,
        bottomPadding = bottomPad,
        trailing = {
            Box {
                NoodleIconButton(Icons.Outlined.MoreVert, { menu = true }, container = androidx.compose.ui.graphics.Color.Transparent)
                NoodleMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    items = listOf(
                        NoodleMenuItem("Новый контакт", Icons.Outlined.Add) {
                            contacts.add(0, Contact(100 + contacts.size, "Новый контакт", "Только что", 1, true, 0))
                            ui.toast.show("Контакт добавлен", Icons.Outlined.Add, NoodleToastTone.Success)
                        },
                        NoodleMenuItem("Сбросить список", Icons.Outlined.Refresh) {
                            contacts.clear(); contacts.addAll(seed)
                            ui.toast.show("Список восстановлен", Icons.Outlined.Refresh)
                        },
                        NoodleMenuItem("Очистить всё", Icons.Outlined.Delete, destructive = true) {
                            contacts.clear()
                            ui.toast.show("Список очищен", Icons.Outlined.Delete, NoodleToastTone.Danger)
                        },
                    ),
                )
            }
        },
    ) {
        item { Box(Modifier.padding(horizontal = 16.dp)) { NoodleSearchBar(query, { query = it }) } }
        item { Spacer(Modifier.height(8.dp)) }
        item { NoodleScrollableTabs(listOf("Все", "Друзья", "Работа", "Семья"), tab, { tab = it }) }
        item { Spacer(Modifier.height(16.dp)) }

        if (filtered.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    design.noodle.components.NoodleLoader()
                    NoodleText("Здесь пока пусто", Modifier.padding(top = 16.dp), style = NoodleTheme.typography.headline)
                    NoodleText(
                        "Нажмите ⋮ и добавьте контакт",
                        Modifier.padding(top = 4.dp),
                        style = NoodleTheme.typography.subhead,
                        color = colors.labelSecondary,
                    )
                }
            }
        } else {
            item {
                NoodleGroup(
                    header = "Контакты · ${filtered.size}",
                    footer = "Смахните строку влево — появится «Удалить». Длинный свайп удаляет сразу.",
                    dividerInset = 72.dp,
                ) {
                    filtered.forEach { c ->
                        row(key = c.id) {
                            NoodleSwipeToDelete(onDelete = {
                                contacts.remove(c)
                                ui.toast.show("${c.name} удалён", Icons.Outlined.Delete, NoodleToastTone.Danger)
                            }) {
                                NoodleRow(
                                    title = c.name,
                                    subtitle = c.role,
                                    leading = { NoodleAvatar(c.name, online = c.online) },
                                    trailing = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (c.unread > 0) NoodleBadge(c.unread)
                                            if (c.group == 2) NoodleTag("Work")
                                        }
                                    },
                                    onClick = { ui.toast.show(c.name, Icons.Outlined.Star) },
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
