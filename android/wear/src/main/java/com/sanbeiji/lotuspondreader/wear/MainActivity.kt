package com.sanbeiji.lotuspondreader.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.foundation.SwipeToDismissValue
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.sanbeiji.lotuspondreader.wear.models.HistoryItem
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private var phoneNodeId: String? = null
    
    // States for Compose
    private val isLoading = mutableStateOf(true)
    private val historyList = mutableStateOf<List<HistoryItem>>(emptyList())
    private val selectedStory = mutableStateOf<HistoryItem?>(null)
    private val errorMessage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register Wearable Message Listener
        Wearable.getMessageClient(this).addListener(this)

        // Find connected phone node
        findPhoneNode {
            fetchHistory()
        }

        setContent {
            WearAppTheme {
                MainLayout()
            }
        }
    }

    private fun findPhoneNode(onReady: () -> Unit = {}) {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                val node = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
                if (node != null) {
                    phoneNodeId = node.id
                    Log.d("WearMainActivity", "Found phone node: ${node.displayName} (id: ${node.id})")
                    onReady()
                } else {
                    Log.e("WearMainActivity", "No paired phone node found.")
                    errorMessage.value = "Cannot connect to phone. Make sure Bluetooth is on."
                    isLoading.value = false
                }
            }
            .addOnFailureListener { e ->
                Log.e("WearMainActivity", "Failed to search connected nodes", e)
                errorMessage.value = "Connection check failed: ${e.localizedMessage}"
                isLoading.value = false
            }
    }

    private fun fetchHistory() {
        val node = phoneNodeId
        if (node == null) {
            findPhoneNode { fetchHistory() }
            return
        }
        isLoading.value = true
        errorMessage.value = null

        Wearable.getMessageClient(this)
            .sendMessage(node, "/fetch_history", ByteArray(0))
            .addOnFailureListener { e ->
                Log.e("WearMainActivity", "Failed to send fetch request", e)
                errorMessage.value = "Sync request failed. Try again."
                isLoading.value = false
            }
    }

    private fun generateStory() {
        val node = phoneNodeId
        if (node == null) {
            errorMessage.value = "No phone connected to generate."
            return
        }
        isLoading.value = true
        errorMessage.value = null

        Wearable.getMessageClient(this)
            .sendMessage(node, "/generate_story", ByteArray(0))
            .addOnFailureListener { e ->
                Log.e("WearMainActivity", "Failed to send generate request", e)
                errorMessage.value = "Failed to start generation."
                isLoading.value = false
            }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WearMainActivity", "Message received path: ${messageEvent.path}")
        when (messageEvent.path) {
            "/history_response" -> {
                try {
                    val jsonStr = String(messageEvent.data)
                    val list = Json.decodeFromString<List<HistoryItem>>(jsonStr)
                    historyList.value = list
                    errorMessage.value = null
                    isLoading.value = false
                    
                    // If a story is currently being read, make sure to update its reference from the fresh list
                    val currentSelected = selectedStory.value
                    if (currentSelected != null) {
                        selectedStory.value = list.firstOrNull { it.id == currentSelected.id } ?: currentSelected
                    }
                } catch (e: Exception) {
                    Log.e("WearMainActivity", "Error decoding history payload", e)
                    errorMessage.value = "Data format error from phone."
                    isLoading.value = false
                }
            }
            "/error_response" -> {
                val errorMsg = String(messageEvent.data)
                errorMessage.value = errorMsg
                isLoading.value = false
            }
        }
    }

    @Composable
    fun MainLayout() {
        val story = selectedStory.value
        val state = rememberSwipeToDismissBoxState()

        if (story != null) {
            // SwipeToDismissBox provides native WearOS swipe right to dismiss
            SwipeToDismissBox(
                state = state,
                onDismissed = { selectedStory.value = null }
            ) { isBackground ->
                if (!isBackground) {
                    ReadingScreen(story = story)
                }
            }
            
            // Sync swipe dismissal state
            LaunchedEffect(state.currentValue) {
                if (state.currentValue == SwipeToDismissValue.Dismissed) {
                    selectedStory.value = null
                    state.snapTo(SwipeToDismissValue.Default)
                }
            }
        } else {
            HomeScreen()
        }
    }

    @Composable
    fun HomeScreen() {
        val loading = isLoading.value
        val err = errorMessage.value
        val list = historyList.value

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Syncing with phone...",
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (err != null) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⚠️ Error",
                        style = MaterialTheme.typography.title2,
                        color = MaterialTheme.colors.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CompactChip(
                        onClick = { fetchHistory() },
                        label = { Text("Retry") }
                    )
                }
            } else if (list.isEmpty()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🪷 Lotus Pond",
                        style = MaterialTheme.typography.title2,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No saved stories found.",
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Chip(
                        onClick = { generateStory() },
                        label = { Text("Generate Story", textAlign = TextAlign.Center) },
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
            } else {
                StoryListScreen(list = list)
            }
        }
    }

    @Composable
    fun StoryListScreen(list: List<HistoryItem>) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "🪷 Lotus Pond",
                    style = MaterialTheme.typography.title2,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Chip(
                    onClick = { generateStory() },
                    label = { Text("➕ Generate New", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            items(list) { item ->
                Chip(
                    onClick = { selectedStory.value = item },
                    label = { Text(item.title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    fun ReadingScreen(story: HistoryItem) {
        val textContent = remember(story) {
            story.data.sentences.joinToString("") { it.mandarin }
        }
        val listState = remember(story.id) {
            ScalingLazyListState(
                initialCenterItemIndex = 0,
                initialCenterItemScrollOffset = 0
            )
        }

        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = null
        ) {
            item {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.title2,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                Text(
                    text = textContent,
                    style = MaterialTheme.typography.body1.copy(lineHeight = 26.sp),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister listener
        Wearable.getMessageClient(this).removeListener(this)
    }
}

@Composable
fun WearAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = androidx.compose.ui.graphics.Color(0xFF80CBC4), // Teal
            primaryVariant = androidx.compose.ui.graphics.Color(0xFF00796B),
            secondary = androidx.compose.ui.graphics.Color(0xFFFFB74D), // Orange
            secondaryVariant = androidx.compose.ui.graphics.Color(0xFFF57C00),
            background = androidx.compose.ui.graphics.Color.Black,
            surface = androidx.compose.ui.graphics.Color(0xFF263238),
            error = androidx.compose.ui.graphics.Color(0xFFEF5350),
            onPrimary = androidx.compose.ui.graphics.Color.Black,
            onSecondary = androidx.compose.ui.graphics.Color.Black,
            onBackground = androidx.compose.ui.graphics.Color.White,
            onSurface = androidx.compose.ui.graphics.Color.White,
            onError = androidx.compose.ui.graphics.Color.Black
        ),
        content = content
    )
}
