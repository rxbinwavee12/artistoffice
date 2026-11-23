package com.hb.office

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.UploadFile
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hb.office.ui.theme.HBBackofficeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Notification-Kanal vorab initialisieren
        NotificationHelper.createNotificationChannel(this)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("artist_office_prefs", Context.MODE_PRIVATE) }
            var accentColor by remember { mutableStateOf(prefs.getString("accent_color", "cyan") ?: "cyan") }

            fun setAccentTheme(themeKey: String) {
                accentColor = themeKey
                prefs.edit().putString("accent_color", themeKey).apply()
            }

            HBBackofficeTheme(accentColor = accentColor) {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val audioPlayerViewModel: AudioPlayerViewModel = viewModel()

                // Skip Login if already logged in
                LaunchedEffect(Unit) {
                    authViewModel.checkUser()
                }

                val authState by authViewModel.authState.collectAsState()

                LaunchedEffect(authState) {
                    if (authState is AuthState.Success) {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = "login",
                            modifier = Modifier.fillMaxSize(),
                            enterTransition = { fadeIn(animationSpec = tween(350)) + slideInHorizontally { it } },
                            exitTransition = { fadeOut(animationSpec = tween(350)) + slideOutHorizontally { -it } },
                            popEnterTransition = { fadeIn(animationSpec = tween(350)) + slideInHorizontally { -it } },
                            popExitTransition = { fadeOut(animationSpec = tween(350)) + slideOutHorizontally { it } }
                        ) {
                            composable("login") {
                                LoginScreen(
                                    authState = authState,
                                    onLogin = { email, pass -> authViewModel.login(email, pass) },
                                    onRegister = { email, pass -> authViewModel.register(email, pass) }
                                )
                            }
                            composable("dashboard") {
                                DashboardScreen(
                                    onProjectSelected = { projectName ->
                                        navController.navigate("projectDetail/$projectName")
                                    },
                                    onNavigateToAdmin = {
                                        navController.navigate("adminPanel")
                                    },
                                    onSetTheme = { themeKey -> setAccentTheme(themeKey) },
                                    onLogout = {
                                        authViewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("adminPanel") {
                                AdminScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("setlistManager/{projectName}") { backStackEntry ->
                                val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                                SetlistScreen(projectName = projectName, onNavigateBack = { navController.popBackStack() })
                            }
                            composable("financeManager/{projectName}") { backStackEntry ->
                                val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                                FinanceScreen(projectName = projectName, onNavigateBack = { navController.popBackStack() })
                            }
                            composable("merchManager/{projectName}") { backStackEntry ->
                                val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                                MerchScreen(projectName = projectName, onNavigateBack = { navController.popBackStack() })
                            }
                            composable("calendarManager/{projectName}") { backStackEntry ->
                                val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                                CalendarScreen(projectName = projectName, onNavigateBack = { navController.popBackStack() })
                            }
                            composable("pollManager/{projectName}") { backStackEntry ->
                                val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                                PollScreen(projectName = projectName, onNavigateBack = { navController.popBackStack() })
                            }
                            composable("linkHubManager/{projectName}") { backStackEntry ->
                                val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                                LinkHubScreen(projectName = projectName, onNavigateBack = { navController.popBackStack() })
                            }
                            composable("metronome") {
                                MetronomeScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("projectDetail/{projectName}") { backStackEntry ->
                                val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                                ProjectDetailScreen(
                                    projectName = projectName,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToTool = { toolRoute -> navController.navigate("$toolRoute/$projectName") },
                                    onNavigateToMetronome = { navController.navigate("metronome") },
                                    audioPlayerViewModel = audioPlayerViewModel
                                )
                            }
                        }

                        BottomAudioPlayerBar(
                            audioPlayerViewModel = audioPlayerViewModel,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

// Helper für den Dateinamen
fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result ?: "unknown_file"
}

// Tageszeit-abhangiger Begrüßungs-Header
fun getGreetingHeader(userName: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Guten Morgen"
        in 12..17 -> "Guten Tag"
        else -> "Guten Abend"
    }
    val formattedName = if (userName.isNotBlank()) {
        userName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    } else {
        "Artist"
    }
    return "$greeting, $formattedName"
}

data class FolderPathNode(val id: String, val name: String)

@Composable
fun BottomAudioPlayerBar(
    audioPlayerViewModel: AudioPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerState by audioPlayerViewModel.playerState.collectAsState()

    if (playerState.currentUrl.isNotBlank()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .zIndex(100f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AudioFile,
                            contentDescription = null,
                            tint = if (playerState.hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = playerState.currentTitle,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (playerState.hasError) {
                                Text(
                                    text = "Wiedergabe-Fehler – Tippe auf ↗️ für Browser",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                val currentSec = playerState.currentPosition / 1000
                                val durSec = playerState.duration / 1000
                                Text(
                                    text = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, durSec / 60, durSec % 60),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playerState.currentUrl))
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Im Browser öffnen"
                            )
                        }
                        IconButton(onClick = { audioPlayerViewModel.togglePlayPause() }) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = { audioPlayerViewModel.closePlayer() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Schließen"
                            )
                        }
                    }
                }

                if (playerState.duration > 0 && !playerState.hasError) {
                    Slider(
                        value = playerState.currentPosition.toFloat(),
                        onValueChange = { pos -> audioPlayerViewModel.seekTo(pos.toLong()) },
                        valueRange = 0f..playerState.duration.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectDetailScreen(
    projectName: String,
    onNavigateBack: () -> Unit,
    onNavigateToTool: (String) -> Unit,
    onNavigateToMetronome: () -> Unit,
    audioPlayerViewModel: AudioPlayerViewModel,
    modifier: Modifier = Modifier,
    storageViewModel: StorageViewModel = viewModel()
) {
    val context = LocalContext.current
    val items by storageViewModel.items.collectAsState()
    val comments by storageViewModel.comments.collectAsState()

    val folderPath = remember { mutableStateListOf<FolderPathNode>() }
    val currentFolderId = folderPath.lastOrNull()?.id ?: ""

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showAddDriveLinkDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<StorageItem?>(null) }
    var itemForComments by remember { mutableStateOf<StorageItem?>(null) }
    var itemForLyrics by remember { mutableStateOf<StorageItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("Alle") }

    LaunchedEffect(projectName, currentFolderId) {
        storageViewModel.loadItems(projectName, currentFolderId)
    }

    // Google Drive Link Hinzufügen Dialog
    if (showAddDriveLinkDialog) {
        var driveTitle by remember { mutableStateOf("") }
        var driveUrlInput by remember { mutableStateOf("") }
        var driveCategory by remember { mutableStateOf("Full Mix") }
        val categories = listOf("Full Mix", "Drums", "Vocals", "Bass", "Gitarre", "Sonstiges")

        AlertDialog(
            onDismissRequest = { showAddDriveLinkDialog = false },
            title = { Text("Google Drive Link hinzufügen") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = driveTitle,
                        onValueChange = { driveTitle = it },
                        label = { Text("Titel (z.B. Demo Track 1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = driveUrlInput,
                        onValueChange = { driveUrlInput = it },
                        label = { Text("Google Drive Link reinkopieren") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Kategorie:", style = MaterialTheme.typography.titleSmall)

                    categories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { driveCategory = cat }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = (driveCategory == cat),
                                onClick = { driveCategory = cat }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = cat)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (driveTitle.isNotBlank() && driveUrlInput.isNotBlank()) {
                            storageViewModel.addDriveLink(projectName, driveTitle, driveUrlInput, currentFolderId, driveCategory)
                            showAddDriveLinkDialog = false
                        }
                    }
                ) {
                    Text("Hinzufügen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDriveLinkDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // File Upload Dialog mit Kategorie-Auswahl
    var selectedUploadUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUploadUri = it }
    }

    if (selectedUploadUri != null) {
        val uri = selectedUploadUri!!
        val fileName = getFileName(context, uri)
        var uploadCategory by remember { mutableStateOf("Full Mix") }
        val categories = listOf("Full Mix", "Drums", "Vocals", "Bass", "Gitarre", "Sonstiges")

        AlertDialog(
            onDismissRequest = { selectedUploadUri = null },
            title = { Text("Datei hochladen") },
            text = {
                Column {
                    Text(text = "Datei: $fileName", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Kategorie wählen:", style = MaterialTheme.typography.titleSmall)

                    categories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { uploadCategory = cat }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (uploadCategory == cat),
                                onClick = { uploadCategory = cat }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = cat)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        storageViewModel.uploadLocalFileToCloud(context, projectName, currentFolderId, uri, fileName, uploadCategory)
                        selectedUploadUri = null
                    }
                ) {
                    Text("Hochladen")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUploadUri = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Ordner Erstellen Dialog mit Status-Auswahl
    if (showCreateFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        var folderStatus by remember { mutableStateOf("Demo") }
        val statuses = listOf("Demo", "Recording", "Mixing", "Mastered", "Released")

        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Neuen Ordner erstellen") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Ordnername (z.B. Track 1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Status zuweisen:", style = MaterialTheme.typography.titleSmall)

                    statuses.forEach { st ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { folderStatus = st }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = (folderStatus == st),
                                onClick = { folderStatus = st }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = st)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            storageViewModel.createFolder(projectName, newFolderName, currentFolderId, folderStatus)
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Erstellen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Kommentare & Feedback Dialog
    if (itemForComments != null) {
        val targetItem = itemForComments!!
        var newCommentText by remember { mutableStateOf("") }

        LaunchedEffect(targetItem.id) {
            storageViewModel.loadComments(projectName, targetItem.id)
        }

        AlertDialog(
            onDismissRequest = {
                storageViewModel.stopCommentsListener()
                itemForComments = null
            },
            title = { Text("Feedback: ${targetItem.name}") },
            text = {
                Column(modifier = Modifier.height(300.dp)) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (comments.isEmpty()) {
                            item {
                                Text("Noch kein Feedback vorhanden.", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            items(comments) { c ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "${c.author}:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = c.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Kommentar schreiben...") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val currentEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
                                val authorName = currentEmail?.substringBefore("@") ?: "Artist"
                                storageViewModel.addComment(projectName, targetItem.id, authorName, newCommentText)
                                newCommentText = ""
                            }
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Senden")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storageViewModel.stopCommentsListener()
                        itemForComments = null
                    }
                ) {
                    Text("Schließen")
                }
            }
        )
    }

    // Songtext & Akkorde Dialog
    if (itemForLyrics != null) {
        val targetItem = itemForLyrics!!
        var lyricsInput by remember { mutableStateOf(targetItem.lyrics) }

        AlertDialog(
            onDismissRequest = { itemForLyrics = null },
            title = { Text("Songtext & Akkorde: ${targetItem.name}") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = lyricsInput,
                        onValueChange = { lyricsInput = it },
                        label = { Text("Text & Akkorde eintragen") },
                        placeholder = { Text("[Verse 1]\nAm C G D\nSongtext hier...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        storageViewModel.updateLyrics(projectName, targetItem.id, lyricsInput)
                        itemForLyrics = null
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForLyrics = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Löschen Bestätigen Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Löschen bestätigen") },
            text = { Text("Möchtest du '${itemToDelete?.name}' wirklich löschen?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        itemToDelete?.let { storageViewModel.deleteItem(projectName, it) }
                        itemToDelete = null
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Pfad Anzeige (Breadcrumb & Zurück-Button)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconButton(onClick = {
                if (folderPath.isNotEmpty()) {
                    folderPath.removeAt(folderPath.lastIndex)
                } else {
                    onNavigateBack()
                }
            }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück")
            }

            val currentPathText = if (folderPath.isEmpty()) {
                projectName
            } else {
                "$projectName > " + folderPath.joinToString(" > ") { it.name }
            }

            Text(
                text = currentPathText,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        // Projekt-Tools Schnellzugriff (GROSSZÜGIGE HORIZONTALE LISTE)
        Text(
            text = "Projekt-Tools",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        val projectTools = listOf(
            Triple("Setlisten", Icons.Default.Mic) { onNavigateToTool("setlistManager") },
            Triple("Bandkasse", Icons.Default.AttachMoney) { onNavigateToTool("financeManager") },
            Triple("Merch", Icons.Default.ShoppingCart) { onNavigateToTool("merchManager") },
            Triple("Kalender", Icons.Default.DateRange) { onNavigateToTool("calendarManager") },
            Triple("Umfragen", Icons.Default.Poll) { onNavigateToTool("pollManager") },
            Triple("Socials", Icons.Default.Share) { onNavigateToTool("linkHubManager") },
            Triple("Metronom", Icons.Default.AudioFile) { onNavigateToMetronome() }
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            items(projectTools) { (label, icon, onClick) ->
                Card(
                    onClick = onClick,
                    modifier = Modifier.width(105.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Action Buttons: + Ordner & Hochladen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showCreateFolderDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Ordner")
            }

            Button(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hochladen")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Kategorie-Filter Chips
        val filterCategories = listOf("Alle", "Full Mix", "Drums", "Vocals", "Bass", "Gitarre", "Sonstiges")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            items(filterCategories) { cat ->
                FilterChip(
                    selected = (selectedCategoryFilter == cat),
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(cat) }
                )
            }
        }

        // Inhalts-Liste (Gefiltert)
        val filteredItems = items.filter { item ->
            if (item.isFolder) true
            else selectedCategoryFilter == "Alle" || item.category == selectedCategoryFilter
        }

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Dieser Ordner ist leer.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn {
                items(filteredItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            if (item.isFolder) {
                                folderPath.add(FolderPathNode(item.id, item.name))
                            } else if (item.downloadUrl.isNotBlank()) {
                                // In-App Audio Player starten!
                                audioPlayerViewModel.playTrack(context, item.name, item.downloadUrl)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.isFolder) Icons.Default.Folder else Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = if (item.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (item.isFolder && item.status.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(item.status, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                                if (!item.isFolder) {
                                    Text(
                                        text = "Kategorie: ${item.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!item.isFolder) {
                                IconButton(onClick = { itemForLyrics = item }) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Songtext & Akkorde"
                                    )
                                }
                                IconButton(onClick = { itemForComments = item }) {
                                    Icon(
                                        imageVector = Icons.Default.Comment,
                                        contentDescription = "Kommentare"
                                    )
                                }
                            }

                            IconButton(onClick = { itemToDelete = item }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Löschen",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    val isLoading = authState is AuthState.Loading

    AnimatedContent(
        targetState = isRegisterMode,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "LoginRegisterAnimation",
        modifier = modifier.fillMaxSize()
    ) { registerMode ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (registerMode) "Artist Office Registrierung" else "Artist Office Login",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-Mail") },
                enabled = !isLoading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Passwort") },
                enabled = !isLoading,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth()
            )

            if (authState is AuthState.Error) {
                Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (registerMode) {
                            onRegister(email, password)
                        } else {
                            onLogin(email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (registerMode) "Registrieren" else "Einloggen")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                    Text(if (registerMode) "Bereits einen Account? Hier einloggen." else "Noch kein Account? Hier registrieren.")
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    onProjectSelected: (String) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSetTheme: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    projectViewModel: ProjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val projects by projectViewModel.projects.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()
    val userRole by projectViewModel.userRole.collectAsState()
    val userName by projectViewModel.userName.collectAsState()
    val showApprovalEvent by projectViewModel.showApprovalEvent.collectAsState()
    val isKickedOut by projectViewModel.isKickedOut.collectAsState()

    LaunchedEffect(isKickedOut) {
        if (isKickedOut) {
            onLogout()
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    // Benachrichtigungs-Berechtigung auf Android 13+ (API 33+) anfordern
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }
        LaunchedEffect(Unit) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Wenn in Echtzeit die Freischaltung erfolgt, Push-Benachrichtigung feuern!
    LaunchedEffect(showApprovalEvent) {
        if (showApprovalEvent) {
            NotificationHelper.showApprovalNotification(context)
            projectViewModel.resetApprovalEvent()
        }
    }

    if (showCreateDialog) {
        var newProjectName by remember { mutableStateOf("") }
        var newProjectDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Neues Projekt erstellen") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Projektname (z.B. Neuer Track)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newProjectDesc,
                        onValueChange = { newProjectDesc = it },
                        label = { Text("Beschreibung (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            projectViewModel.createProject(newProjectName, newProjectDesc)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Erstellen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Personalisierter Tageszeit-Header mit Logout-Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getGreetingHeader(userName),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Willkommen im Artist Office",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Ausloggen",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Design-Akzent:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    val themes = listOf(
                        "cyan" to androidx.compose.ui.graphics.Color(0xFF38BDF8),
                        "violet" to androidx.compose.ui.graphics.Color(0xFFA855F7),
                        "gold" to androidx.compose.ui.graphics.Color(0xFFF59E0B),
                        "red" to androidx.compose.ui.graphics.Color(0xFFEF4444)
                    )
                    themes.forEach { (key, color) ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(color, shape = CircleShape)
                                .clickable { onSetTheme(key) }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Deine Projekte",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (userRole == "admin") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mitglieder")
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Neues Projekt")
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Aktuell siehst du keine Projekte. Warte bitte bis du freigeschaltet wirst. Dies kann bis zu 48 Stunden dauern.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            projects.forEach { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    onClick = { onProjectSelected(project.name) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = project.name, style = MaterialTheme.typography.titleLarge)
                        if (project.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = project.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    adminViewModel: AdminViewModel = viewModel(),
    projectViewModel: ProjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val users by adminViewModel.users.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val allProjects by projectViewModel.projects.collectAsState()

    var userToEdit by remember { mutableStateOf<UserProfile?>(null) }
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }

    if (userToDelete != null) {
        val targetUser = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Mitglied löschen") },
            text = { Text("Möchtest du den Account von '${targetUser.email}' wirklich unwiderruflich aus dem Backoffice löschen?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        adminViewModel.deleteUser(context, targetUser.uid, targetUser.email)
                        userToDelete = null
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (userToEdit != null) {
        val user = userToEdit!!
        var usernameInput by remember { mutableStateOf(user.username) }
        var selectedRole by remember { mutableStateOf(user.role) }
        val selectedProjects = remember { mutableStateListOf<String>().apply { addAll(user.allowedProjects) } }

        AlertDialog(
            onDismissRequest = { userToEdit = null },
            title = { Text("Mitglied bearbeiten") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = "E-Mail: ${user.email}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Künstlername / Username") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Rolle zuweisen:", style = MaterialTheme.typography.titleSmall)

                    val roles = listOf("user" to "Standard User", "hb" to "Hindenburg", "sr" to "SchuppenRecords", "admin" to "Admin (Vollzugriff)")
                    roles.forEach { (roleKey, roleLabel) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = roleKey }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedRole == roleKey),
                                onClick = { selectedRole = roleKey }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = roleLabel)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Einzelne Projekte freischalten:", style = MaterialTheme.typography.titleSmall)

                    allProjects.forEach { proj ->
                        val isChecked = selectedProjects.contains(proj.name)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedProjects.remove(proj.name)
                                    else selectedProjects.add(proj.name)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedProjects.add(proj.name)
                                    else selectedProjects.remove(proj.name)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = proj.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.updateUser(
                            uid = user.uid,
                            username = usernameInput,
                            role = selectedRole,
                            allowedProjects = selectedProjects.toList()
                        )
                        userToEdit = null
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück")
            }
            Text(
                text = "Mitglieder-Verwaltung",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(users) { u ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (u.username.isNotBlank()) u.username else u.email,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "E-Mail: ${u.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Rolle: ${u.role.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Button(onClick = { userToEdit = u }) {
                                Text("Bearbeiten")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { userToDelete = u }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Mitglied löschen",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetlistScreen(
    projectName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    setlistViewModel: SetlistViewModel = viewModel()
) {
    val setlists by setlistViewModel.setlists.collectAsState()
    val isLoading by setlistViewModel.isLoading.collectAsState()

    LaunchedEffect(projectName) {
        setlistViewModel.loadSetlists(projectName)
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var activeStageSetlist by remember { mutableStateOf<SetlistItem?>(null) }

    if (activeStageSetlist != null) {
        val currentSet = activeStageSetlist!!
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "STAGE MODE: ${currentSet.name}", style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { activeStageSetlist = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                currentSet.songs.forEachIndexed { idx, song ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${idx + 1}.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = song, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }

                if (currentSet.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Bühnen-Notizen: ${currentSet.notes}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        return
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var songsRaw by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Neue Setlist erstellen ($projectName)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Setlist Name (z.B. Festival Set 45 Min)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = songsRaw,
                        onValueChange = { songsRaw = it },
                        label = { Text("Songs (ein Song pro Zeile)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Bühnen-Notizen (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val songList = songsRaw.split("\n")
                            setlistViewModel.createSetlist(projectName, name, songList, notes)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Erstellen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück")
            }
            Text(text = "Setlisten: $projectName", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = { showCreateDialog = true }) {
                Text("+ Setlist")
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn {
                items(setlists) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        onClick = { activeStageSetlist = item }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = "${item.songs.size} Songs", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { activeStageSetlist = item }) {
                                Text("Stage Mode 🎤")
                            }
                            IconButton(onClick = { setlistViewModel.deleteSetlist(projectName, item.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceScreen(
    projectName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    financeViewModel: FinanceViewModel = viewModel()
) {
    val finances by financeViewModel.finances.collectAsState()
    val totalBalance by financeViewModel.totalBalance.collectAsState()
    val isLoading by financeViewModel.isLoading.collectAsState()

    LaunchedEffect(projectName) {
        financeViewModel.loadFinances(projectName)
    }

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var amountRaw by remember { mutableStateOf("") }
        var isIncome by remember { mutableStateOf(true) }
        var category by remember { mutableStateOf("Gage") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Neue Buchung eintragen ($projectName)") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Beschreibung (z.B. Gig Gage)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = amountRaw, onValueChange = { amountRaw = it }, label = { Text("Betrag (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isIncome, onClick = { isIncome = true })
                        Text("Einnahme (+)")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = !isIncome, onClick = { isIncome = false })
                        Text("Ausgabe (-)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountRaw.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amount > 0) {
                            financeViewModel.addTransaction(projectName, title, amount, isIncome, category)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Buchen")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Abbrechen") } }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onNavigateBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück") }
            Text(text = "Bandkasse: $projectName", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = { showAddDialog = true }) { Text("+ Buchung") }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Kassenstand", style = MaterialTheme.typography.titleMedium)
                Text(text = String.format("%.2f €", totalBalance), style = MaterialTheme.typography.headlineLarge, color = if (totalBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn {
                items(finances) { f ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = f.title, style = MaterialTheme.typography.titleMedium)
                                Text(text = f.category, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = (if (f.isIncome) "+ " else "- ") + String.format("%.2f €", f.amount),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (f.isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            IconButton(onClick = { financeViewModel.deleteTransaction(projectName, f.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MerchScreen(
    projectName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    merchViewModel: MerchViewModel = viewModel()
) {
    val merchList by merchViewModel.merchList.collectAsState()
    val isLoading by merchViewModel.isLoading.collectAsState()

    LaunchedEffect(projectName) {
        merchViewModel.loadMerch(projectName)
    }

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var priceRaw by remember { mutableStateOf("") }
        var s by remember { mutableStateOf("10") }
        var m by remember { mutableStateOf("10") }
        var l by remember { mutableStateOf("10") }
        var xl by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Neuen Merch Artikel anlegen ($projectName)") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Artikelname (z.B. Band T-Shirt)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = priceRaw, onValueChange = { priceRaw = it }, label = { Text("Verkaufspreis (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = s, onValueChange = { s = it }, label = { Text("S") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = m, onValueChange = { m = it }, label = { Text("M") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = l, onValueChange = { l = it }, label = { Text("L") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = xl, onValueChange = { xl = it }, label = { Text("XL") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = priceRaw.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            merchViewModel.addMerchItem(projectName, name, p, s.toIntOrNull() ?: 0, m.toIntOrNull() ?: 0, l.toIntOrNull() ?: 0, xl.toIntOrNull() ?: 0)
                            showAddDialog = false
                        }
                    }
                ) { Text("Anlegen") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Abbrechen") } }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onNavigateBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück") }
            Text(text = "Merch: $projectName", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = { showAddDialog = true }) { Text("+ Artikel") }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn {
                items(merchList) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = String.format("%.2f €", item.price), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("S: ${item.sizeS}")
                                Text("M: ${item.sizeM}")
                                Text("L: ${item.sizeL}")
                                Text("XL: ${item.sizeXL}")
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { merchViewModel.deleteMerch(projectName, item.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(
    projectName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    calendarViewModel: CalendarViewModel = viewModel()
) {
    val context = LocalContext.current
    val events by calendarViewModel.events.collectAsState()
    val isLoading by calendarViewModel.isLoading.collectAsState()

    LaunchedEffect(projectName) {
        calendarViewModel.loadEvents(projectName)
    }

    var showAddDialog by remember { mutableStateOf(false) }

    fun pickDateTime(onDateTimeSelected: (String) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val formatted = String.format("%02d.%02d.%d %02d:%02d", dayOfMonth, month + 1, year, hourOfDay, minute)
                        onDateTimeSelected(formatted)
                    },
                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Probe") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Neuen Termin eintragen ($projectName)") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titel (z.B. Probe / Gig Berlin)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { pickDateTime { selected -> date = selected } },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (date.isNotBlank()) "Datum: $date" else "Datum & Uhrzeit wählen 📅")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notizen (optional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            calendarViewModel.addEvent(projectName, title, date, type, notes)
                            showAddDialog = false
                        }
                    }
                ) { Text("Eintragen") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Abbrechen") } }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onNavigateBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück") }
            Text(text = "Kalender: $projectName", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = { showAddDialog = true }) { Text("+ Termin") }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn {
                items(events) { ev ->
                    val userEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@") ?: "User"
                    val myRsvp = ev.rsvps[userEmail] ?: "Offen"

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = ev.title, style = MaterialTheme.typography.titleMedium)
                                Text(text = ev.date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            if (ev.notes.isNotBlank()) {
                                Text(text = ev.notes, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Dein Status: ", style = MaterialTheme.typography.labelMedium)
                                FilterChip(selected = (myRsvp == "Dabei"), onClick = { calendarViewModel.setRsvp(projectName, ev.id, userEmail, "Dabei") }, label = { Text("Dabei") })
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(selected = (myRsvp == "Absage"), onClick = { calendarViewModel.setRsvp(projectName, ev.id, userEmail, "Absage") }, label = { Text("Absage") })
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { calendarViewModel.deleteEvent(projectName, ev.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PollScreen(
    projectName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    pollViewModel: PollViewModel = viewModel()
) {
    val polls by pollViewModel.polls.collectAsState()
    val isLoading by pollViewModel.isLoading.collectAsState()
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@") ?: "User"

    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        var question by remember { mutableStateOf("") }
        var opt1 by remember { mutableStateOf("") }
        var opt2 by remember { mutableStateOf("") }
        var opt3 by remember { mutableStateOf("") }
        var opt4 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Neue Umfrage erstellen ($projectName)") },
            text = {
                Column {
                    OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Frage (z.B. Welches Shirt?)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option 1") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option 2") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text("Option 3 (optional)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = opt4, onValueChange = { opt4 = it }, label = { Text("Option 4 (optional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (question.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank()) {
                            pollViewModel.createPoll(projectName, question, listOf(opt1, opt2, opt3, opt4))
                            showCreateDialog = false
                        }
                    }
                ) { Text("Erstellen") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Abbrechen") } }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onNavigateBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück") }
            Text(text = "Band-Umfragen: $projectName", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = { showCreateDialog = true }) { Text("+ Umfrage") }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(polls) { poll ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Text(text = poll.question, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { pollViewModel.deletePoll(projectName, poll.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            val totalVotes = poll.votes.size
                            val myVote = poll.votes[currentUser]

                            poll.options.forEachIndexed { idx, opt ->
                                val votesForOpt = poll.votes.count { it.value == idx }
                                val percentage = if (totalVotes > 0) (votesForOpt.toFloat() / totalVotes) else 0f

                                Column(modifier = Modifier.fillMaxWidth().clickable { pollViewModel.castVote(projectName, poll.id, currentUser, idx) }.padding(vertical = 4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = opt, style = MaterialTheme.typography.bodyMedium, color = if (myVote == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                        Text(text = "$votesForOpt Stimme(n)", style = MaterialTheme.typography.labelSmall)
                                    }
                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp),
                                        color = if (myVote == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinkHubScreen(
    projectName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    linkHubViewModel: LinkHubViewModel = viewModel()
) {
    val context = LocalContext.current
    val links by linkHubViewModel.links.collectAsState()
    val isLoading by linkHubViewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var platform by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Neuen Link hinzufügen ($projectName)") },
            text = {
                Column {
                    OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("Plattform (z.B. Spotify / Instagram)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Link-URL (https://...)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (platform.isNotBlank() && url.isNotBlank()) {
                            linkHubViewModel.addLink(projectName, platform, url)
                            showAddDialog = false
                        }
                    }
                ) { Text("Hinzufügen") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Abbrechen") } }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onNavigateBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück") }
            Text(text = "Socials & Link-Hub: $projectName", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = { showAddDialog = true }) { Text("+ Link") }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(links) { link ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = link.platform, style = MaterialTheme.typography.titleMedium)
                                Text(text = link.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Copied Link", link.url)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Link in die Zwischenablage kopiert!", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Kopieren", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { linkHubViewModel.deleteLink(projectName, link.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetronomeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    metronomeViewModel: MetronomeViewModel = viewModel()
) {
    val bpm by metronomeViewModel.bpm.collectAsState()
    val isPlaying by metronomeViewModel.isPlaying.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Zurück")
            }
            Text(text = "Proberaum Metronom", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "$bpm", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text(text = "BPM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = { metronomeViewModel.setBpm(bpm - 5) }) { Text("-5") }
            FilledTonalButton(onClick = { metronomeViewModel.setBpm(bpm - 1) }) { Text("-1") }
            FilledTonalButton(onClick = { metronomeViewModel.setBpm(bpm + 1) }) { Text("+1") }
            FilledTonalButton(onClick = { metronomeViewModel.setBpm(bpm + 5) }) { Text("+5") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = bpm.toFloat(),
            onValueChange = { metronomeViewModel.setBpm(it.toInt()) },
            valueRange = 30f..260f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { metronomeViewModel.togglePlay() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isPlaying) "Stoppen" else "Starten", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { metronomeViewModel.tapTempo() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Tap Tempo ⏱️", style = MaterialTheme.typography.titleMedium)
        }
    }
}
