package `in`.aboobacker.labdroid.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import `in`.aboobacker.labdroid.NavigationCategory
import `in`.aboobacker.labdroid.R
import `in`.aboobacker.labdroid.Screen
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme

@Composable
fun HomeBottomNavigation(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit = {}
) {
    val currentCategory = currentScreen.toCategory()
    NavigationBar {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = stringResource(R.string.projects)
                )
            },
            label = { Text(stringResource(R.string.projects)) },
            selected = currentCategory == NavigationCategory.PROJECTS,
            onClick = { onTabSelected(Screen.Projects) }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = stringResource(R.string.work_items)
                )
            },
            label = { Text(stringResource(R.string.work_items)) },
            selected = currentCategory == NavigationCategory.ISSUES,
            onClick = { onTabSelected(Screen.Issues) }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.CallMerge,
                    contentDescription = stringResource(R.string.mrs)
                )
            },
            label = { Text(stringResource(R.string.mrs)) },
            selected = currentCategory == NavigationCategory.MRS,
            onClick = { onTabSelected(Screen.MergeRequests) }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(R.string.profile)
                )
            },
            label = { Text(stringResource(R.string.profile)) },
            selected = currentCategory == NavigationCategory.PROFILE,
            onClick = { onTabSelected(Screen.Profile) }
        )
    }
}

@Preview
@Composable
fun PreviewHomeBottomNavigation() {
    LabdroidTheme {
        HomeBottomNavigation(
            currentScreen = TODO(),
            onTabSelected = {}
        )
    }
}