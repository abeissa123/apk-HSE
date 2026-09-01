package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.example.service.FcmPushManager
import com.example.ui.components.HseBottomNavBar
import com.example.ui.screens.admin.AdminAttestationsScreen
import com.example.ui.screens.admin.AdminContentScreen
import com.example.ui.screens.admin.AdminUsersScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.content.ContentReaderDialog
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.emergency.EmergencyScreen
import com.example.ui.screens.ia.HseIaScreen
import com.example.ui.screens.policies.PoliciesScreen
import com.example.ui.screens.profile.NotificationsCenterScreen

import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.SuccessStoriesScreen
import com.example.ui.screens.profile.SurveysScreen
import com.example.ui.screens.program.ProgramScreen
import com.example.ui.theme.HSEDarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HseViewModel
import com.example.ui.viewmodel.MainTab

class MainActivity : ComponentActivity() {
  private val viewModel: HseViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    FcmPushManager.initialize(applicationContext)

    // Check if opened from notification
    intent?.let { intent ->
      val targetTab = intent.getStringExtra("target_tab")
      if (targetTab == "URGENCE") {
        viewModel.setTab(MainTab.URGENCE)
      } else if (targetTab == "NOTIFICATIONS" || intent.hasExtra("notification_type")) {
        viewModel.isNotificationsScreenOpen.value = true
      }
    }

    setContent {
      MyApplicationTheme {
        MainAppContent(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun MainAppContent(
  viewModel: HseViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val authState by viewModel.authState.collectAsState()
  val currentTab by viewModel.currentTab.collectAsState()

  val userState by viewModel.user.collectAsState()
  val activeUser = userState?.takeIf { it.email.isNotBlank() } ?: authState.currentUser
  val isUserAdmin = activeUser.isAdmin

  // Request Notification permission on Android 13+ (API 33+)
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      FcmPushManager.initialize(context, activeUser.email, activeUser.site)
    }
  }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  // Subscribe user to FCM topics when logged in
  LaunchedEffect(activeUser.email, activeUser.site) {
    if (activeUser.email.isNotBlank()) {
      FcmPushManager.subscribeUser(
        context = context,
        email = activeUser.email,
        site = activeUser.site,
        role = activeUser.role
      )
    }
  }

  val isNotificationsOpen by viewModel.isNotificationsScreenOpen.collectAsState()
  val isSurveysOpen by viewModel.isSurveysScreenOpen.collectAsState()
  val isStoriesOpen by viewModel.isSuccessStoriesScreenOpen.collectAsState()
  val isHseIaOpen by viewModel.isHseIaScreenOpen.collectAsState()
  val isAdminContentOpen by viewModel.isAdminContentScreenOpen.collectAsState()
  val isAdminUsersOpen by viewModel.isAdminUsersScreenOpen.collectAsState()
  val isAdminAttestationsOpen by viewModel.isAdminAttestationsScreenOpen.collectAsState()
  val selectedViewingContent by viewModel.selectedViewingContent.collectAsState()

  if (selectedViewingContent != null) {
    ContentReaderDialog(
      content = selectedViewingContent!!,
      viewModel = viewModel,
      onDismiss = { viewModel.closeContentReader() }
    )
  }

  if (!authState.isLoggedIn) {
    LoginScreen(
      viewModel = viewModel,
      modifier = modifier
    )
  } else if (isHseIaOpen) {
    HseIaScreen(
      viewModel = viewModel,
      onBack = { viewModel.closeHseIa() }
    )
  } else if (isAdminContentOpen && isUserAdmin) {
    AdminContentScreen(
      viewModel = viewModel,
      onBack = { viewModel.isAdminContentScreenOpen.value = false }
    )
  } else if (isAdminUsersOpen && isUserAdmin) {
    AdminUsersScreen(
      viewModel = viewModel,
      onBack = { viewModel.isAdminUsersScreenOpen.value = false }
    )
  } else if (isAdminAttestationsOpen && isUserAdmin) {
    AdminAttestationsScreen(
      viewModel = viewModel,
      onBack = { viewModel.isAdminAttestationsScreenOpen.value = false }
    )
  } else if (isNotificationsOpen) {
    NotificationsCenterScreen(
      viewModel = viewModel,
      onBack = { viewModel.isNotificationsScreenOpen.value = false }
    )
  } else if (isSurveysOpen) {
    SurveysScreen(
      viewModel = viewModel,
      onBack = { viewModel.isSurveysScreenOpen.value = false }
    )
  } else if (isStoriesOpen) {
    SuccessStoriesScreen(
      viewModel = viewModel,
      onBack = { viewModel.isSuccessStoriesScreenOpen.value = false }
    )
  } else {
    Scaffold(
      modifier = modifier
        .fillMaxSize()
        .background(HSEDarkBg)
        .testTag("main_scaffold"),
      bottomBar = {
        HseBottomNavBar(
          currentTab = currentTab,
          isAdmin = isUserAdmin,
          onTabSelected = { viewModel.setTab(it) }
        )
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (currentTab) {
          MainTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
          MainTab.CONTENUS -> {
            if (isUserAdmin) {
              AdminContentScreen(viewModel = viewModel, onBack = { viewModel.setTab(MainTab.DASHBOARD) })
            } else {
              DashboardScreen(viewModel = viewModel)
            }
          }
          MainTab.UTILISATEURS -> {
            if (isUserAdmin) {
              AdminUsersScreen(viewModel = viewModel, onBack = { viewModel.setTab(MainTab.DASHBOARD) })
            } else {
              DashboardScreen(viewModel = viewModel)
            }
          }
          MainTab.PROGRAMME -> ProgramScreen(viewModel = viewModel)
          MainTab.URGENCE -> EmergencyScreen(viewModel = viewModel)
          MainTab.POLITIQUES -> PoliciesScreen(viewModel = viewModel)
          MainTab.PROFIL -> ProfileScreen(viewModel = viewModel)
          MainTab.IA -> HseIaScreen(viewModel = viewModel, onBack = { viewModel.setTab(MainTab.DASHBOARD) })
        }
      }
    }
  }

}


