package pl.grzybtrop.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.grzybtrop.ui.home.HomeRoute
import pl.grzybtrop.ui.map.MapScreen
import pl.grzybtrop.ui.theme.Bg
import pl.grzybtrop.ui.theme.Border
import pl.grzybtrop.ui.theme.Lime
import pl.grzybtrop.ui.theme.Muted
import pl.grzybtrop.ui.theme.Surface as SurfaceColor

@Composable
fun AppRoot() {
    // Strona 0 = mapa, strona 1 = ekran główny. Startujemy na 1.
    val pagerState = rememberPagerState(initialPage = 1) { 2 }
    val scope = rememberCoroutineScope()

    // Gest/przycisk „wstecz” na mapie wraca na ekran główny.
    BackHandler(enabled = pagerState.currentPage == 0) {
        scope.launch { pagerState.scrollToPage(1) }
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            BottomBar(
                currentPage = pagerState.currentPage,
                onSelect = { page -> scope.launch { pagerState.scrollToPage(page) } },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,      // brak przesuwania palcem
            beyondViewportPageCount = 1,    // mapa zostaje w pamięci i nie traci pozycji
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { page ->
            when (page) {
                0 -> MapScreen()
                else -> HomeRoute()
            }
        }
    }
}

@Composable
private fun BottomBar(currentPage: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = SurfaceColor) {
        // nazwa, ikona, numer strony pagera (null = ekran jeszcze nie istnieje)
        val items = listOf<Triple<String, ImageVector, Int?>>(
            Triple("Mapa", Icons.Outlined.Map, 0),
            Triple("Spacer", Icons.Outlined.Route, 1),
            Triple("Znaleziska", Icons.Outlined.Eco, null),
        )
        items.forEach { (label, icon, page) ->
            NavigationBarItem(
                selected = page == currentPage,
                onClick = { if (page != null) onSelect(page) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Lime,
                    selectedTextColor = Lime,
                    indicatorColor = Border,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                ),
            )
        }
    }
}