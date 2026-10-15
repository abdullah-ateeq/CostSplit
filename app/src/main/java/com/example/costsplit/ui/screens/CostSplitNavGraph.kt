package com.example.costsplit.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.costsplit.ui.viewmodel.CostSplitViewModel

sealed class Screen(val route: String) {
    data object Input : Screen("input")
    data object Result : Screen("result")
}

@Composable
fun CostSplitNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: CostSplitViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Input.route,
        modifier = modifier
    ) {
        composable(Screen.Input.route) {
            InputScreen(
                viewModel = viewModel,
                onNavigateToResult = {
                    navController.navigate(Screen.Result.route)
                }
            )
        }

        composable(Screen.Result.route) {
            ResultScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
