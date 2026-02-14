package com.yugpatel.rockpapergame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RockPaperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun RockPaperTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF6200EE),
        secondary = Color(0xFF03DAC5),
        tertiary = Color(0xFF3700B3),
        background = Color(0xFFF8F9FA),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color(0xFF212121),
        onSurface = Color(0xFF212121),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "start") {
        composable("start") {
            StartScreen(onStartGame = {
                navController.navigate("game")
            })
        }
        composable("game") {
            GameScreen(onBack = {
                navController.popBackStack()
            })
        }
    }
}

@Composable
fun StartScreen(onStartGame: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6200EE), Color(0xFF3700B3))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .scale(scale)
                    .shadow(20.dp, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\ud83e\udea8 \ud83d\udcc4 \u2702\ufe0f",
                        fontSize = 60.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "ROCK PAPER",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF6200EE),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "SCISSORS",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF6200EE),
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "First to 5 wins the Match!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03DAC5)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Created by Yug Patel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.LightGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .height(64.dp)
                    .width(240.dp)
                    .shadow(12.dp, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
            ) {
                Text("START MATCH", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }
    }
}

@Composable
fun GameScreen(onBack: () -> Unit) {
    var playerScore by remember { mutableIntStateOf(0) }
    var computerScore by remember { mutableIntStateOf(0) }
    var currentStreak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf("Choose your weapon!") }
    var computerChoice by remember { mutableStateOf("?") }
    var playerChoice by remember { mutableStateOf("?") }
    var isAnimating by remember { mutableStateOf(false) }
    var gameHistory by remember { mutableStateOf(listOf<String>()) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var matchWinner by remember { mutableStateOf("") }

    val choices = listOf("Rock", "Paper", "Scissors")
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val WIN_LIMIT = 5

    fun resetGame() {
        playerScore = 0
        computerScore = 0
        currentStreak = 0
        result = "Choose your weapon!"
        computerChoice = "?"
        playerChoice = "?"
        gameHistory = emptyList()
        showWinnerDialog = false
    }

    suspend fun play(choice: String) {
        if (isAnimating || showWinnerDialog) return
        isAnimating = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        
        playerChoice = choice
        result = "CPU is thinking..."
        computerChoice = "?"
        
        delay(700) 
        
        val computer = choices[Random.nextInt(3)]
        computerChoice = computer

        val outcome = when {
            choice == computer -> "DRAW"
            choice == "Rock" && computer == "Scissors" -> "WIN"
            choice == "Paper" && computer == "Rock" -> "WIN"
            choice == "Scissors" && computer == "Paper" -> "WIN"
            else -> "LOSE"
        }

        result = when (outcome) {
            "WIN" -> {
                playerScore++
                currentStreak++
                if (currentStreak > bestStreak) bestStreak = currentStreak
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                "BOOM! YOU WIN! \ud83c\udf89"
            }
            "LOSE" -> {
                computerScore++
                currentStreak = 0
                "OUCH! CPU WIN! \ud83d\ude1e"
            }
            else -> {
                currentStreak = 0
                "STALEMATE! \u2694\ufe0f"
            }
        }
        
        gameHistory = (listOf(outcome) + gameHistory).take(8)
        
        if (playerScore >= WIN_LIMIT) {
            delay(500)
            matchWinner = "PLAYER"
            showWinnerDialog = true
        } else if (computerScore >= WIN_LIMIT) {
            delay(500)
            matchWinner = "COMPUTER"
            showWinnerDialog = true
        }
        
        isAnimating = false
    }

    if (showWinnerDialog) {
        WinnerDialog(
            winner = matchWinner,
            onRestart = { resetGame() },
            onExit = { 
                resetGame()
                onBack() 
            }
        )
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            CenterAlignedTopAppBar(
                title = { Text("ARENA", fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { resetGame() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("BEST STREAK: $bestStreak", fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(20.dp))
                Text("CURRENT: $currentStreak", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }

            // Score Board
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreItem("YOU", playerScore, Color(0xFF6200EE), WIN_LIMIT)
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Text("VS", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.LightGray)
                    }
                    ScoreItem("CPU", computerScore, Color(0xFFF44336), WIN_LIMIT)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Choices Display Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BattleBox("YOUR PICK", playerChoice, Color(0xFFEDE7F6))
                BattleBox("CPU PICK", computerChoice, Color(0xFFFFF3E0))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Result Message
            Box(
                modifier = Modifier.height(80.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = result,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) togetherWith
                        fadeOut(animationSpec = tween(90))
                    }, label = "resultAnim"
                ) { targetText ->
                    Text(
                        text = targetText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        color = when {
                            targetText.contains("WIN") -> Color(0xFF4CAF50)
                            targetText.contains("OUCH") -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            // History Dots
            if (gameHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(gameHistory) { outcome ->
                        val color = when(outcome) {
                            "WIN" -> Color(0xFF4CAF50)
                            "LOSE" -> Color(0xFFF44336)
                            else -> Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Control Panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "SELECT WEAPON", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 12.sp, 
                        letterSpacing = 2.sp,
                        color = Color.LightGray
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeaponButton("Rock", "\ud83e\udea8") { scope.launch { play("Rock") } }
                        WeaponButton("Paper", "\ud83d\udcc4") { scope.launch { play("Paper") } }
                        WeaponButton("Scissors", "\u2702\ufe0f") { scope.launch { play("Scissors") } }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreItem(label: String, score: Int, color: Color, limit: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Gray)
        Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Black, color = color)
        LinearProgressIndicator(
            progress = { score.toFloat() / limit },
            modifier = Modifier.width(60.dp).height(4.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun BattleBox(title: String, choice: String, bgColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.LightGray)
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(bgColor)
                .shadow(2.dp, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            val emoji = when(choice) {
                "Rock" -> "\ud83e\udea8"
                "Paper" -> "\ud83d\udcc4"
                "Scissors" -> "\u2702\ufe0f"
                else -> "?"
            }
            Text(emoji, fontSize = 48.sp)
        }
    }
}

@Composable
fun WeaponButton(label: String, emoji: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(84.dp)
                .shadow(6.dp, CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(emoji, fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.DarkGray)
    }
}

@Composable
fun WinnerDialog(winner: String, onRestart: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (winner == "PLAYER") "\ud83c\udfc6" else "\ud83e\udd16",
                    fontSize = 80.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (winner == "PLAYER") "VICTORY!" else "DEFEAT!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (winner == "PLAYER") Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (winner == "PLAYER") "You are the Rock Paper Scissors Champion!" else "The CPU was too strong this time.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                ) {
                    Text("PLAY AGAIN", fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Main Menu", color = Color.Gray)
                }
            }
        }
    }
}
