@file:Suppress("MissingPermission")

package br.com.mobilibus.tracker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.com.mobilibus.tracker.AppGraph
import br.com.mobilibus.tracker.domain.model.FavoriteVehicle
import br.com.mobilibus.tracker.domain.model.Vehicle
import kotlinx.serialization.Serializable
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Serializable data object VehiclesTab
@Serializable data object MapTab
@Serializable data object FavoritesTab
@Serializable data object AccountTab
@Serializable data class VehicleDetail(val vehicleId: String)

@Composable
fun MobilibusApp(graph: AppGraph) {
    val userId by graph.session.userId.collectAsStateWithLifecycle(initialValue = null)
    if (userId == null) {
        val auth: AuthViewModel = viewModel(factory = factory { AuthViewModel(graph.authRepository) })
        AuthScreen(auth)
    } else {
        val tracker: TrackerViewModel = viewModel(
            key = "tracker-$userId",
            factory = factory { TrackerViewModel(userId!!, graph.favoriteRepository, graph.authRepository, graph.vehicleRepository) }
        )
        TrackerScreen(userId!!, tracker, onLogout = tracker::logout)
    }
}

@Composable
private fun AuthScreen(viewModel: AuthViewModel) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.DirectionsBus, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Mobilibus Ao Vivo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(if (register) "Crie sua conta para salvar veículos." else "Entre para acompanhar sua frota.")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("E-mail") }, singleLine = true)
            OutlinedTextField(
                password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { viewModel.submit(email, password, register) },
                modifier = Modifier.fillMaxWidth(), enabled = !submitting
            ) { Text(if (submitting) "Aguarde..." else if (register) "Criar conta" else "Entrar") }
            TextButton(onClick = { register = !register }) {
                Text(if (register) "Já tenho uma conta" else "Ainda não tenho conta")
            }
            if (register) Text("Use um e-mail válido e senha de no mínimo 6 caracteres.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TrackerScreen(userId: Long, viewModel: TrackerViewModel, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteVehicles.collectAsStateWithLifecycle()
    val favoriteIds = favorites.map { it.vehicleId }.toSet()

    Scaffold(
        bottomBar = { BottomTabs(nav) }
    ) { padding ->
        NavHost(nav, startDestination = VehiclesTab, modifier = Modifier.padding(padding)) {
            composable<VehiclesTab> {
                VehicleListScreen(feed, favoriteIds, viewModel::refresh, { vehicle, favorite ->
                    viewModel.toggleFavorite(vehicle, favorite)
                }) { nav.navigate(VehicleDetail(it.id)) }
            }
            composable<MapTab> {
                MapScreen(feed = feed, favoriteIds = favoriteIds, refresh = viewModel::refresh)
            }
            composable<FavoritesTab> {
                FavoritesScreen(favorites, favoriteIds) { nav.navigate(VehicleDetail(it)) }
            }
            composable<AccountTab> {
                AccountScreen(userId, onLogout)
            }
            composable<VehicleDetail> { entry ->
                val vehicleId = entry.toRoute<VehicleDetail>().vehicleId
                val live = (feed as? FeedUiState.Success)?.vehicles.orEmpty()
                val vehicle = live.firstOrNull { it.id == vehicleId }
                    ?: favorites.firstOrNull { it.vehicleId == vehicleId }?.asVehicle()
                DetailScreen(
                    vehicle = vehicle,
                    isFavorite = vehicleId in favoriteIds,
                    onBack = { nav.popBackStack() },
                    onToggle = { vehicle?.let { viewModel.toggleFavorite(it, vehicleId in favoriteIds) } }
                )
            }
        }
    }
}

@Composable
private fun BottomTabs(nav: NavHostController) {
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route.orEmpty()
    NavigationBar {
        NavigationBarItem(route.contains("VehiclesTab"), { nav.navigate(VehiclesTab) { launchSingleTop = true } }, { Icon(Icons.Default.DirectionsBus, "Veículos") }, label = { Text("Veículos") })
        NavigationBarItem(route.contains("MapTab"), { nav.navigate(MapTab) { launchSingleTop = true } }, { Icon(Icons.Default.LocationOn, "Mapa") }, label = { Text("Mapa") })
        NavigationBarItem(route.contains("FavoritesTab"), { nav.navigate(FavoritesTab) { launchSingleTop = true } }, { Icon(Icons.Default.Favorite, "Salvos") }, label = { Text("Salvos") })
        NavigationBarItem(route.contains("AccountTab"), { nav.navigate(AccountTab) { launchSingleTop = true } }, { Icon(Icons.Default.Person, "Conta") }, label = { Text("Conta") })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapScreen(feed: FeedUiState, favoriteIds: Set<String>, refresh: () -> Unit) {
    val context = LocalContext.current
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val defaultCenter = remember { LatLng(-19.9167, -43.9345) }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 12f)
    }
    var cachedFeed by remember { mutableStateOf<FeedUiState.Success?>(null) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var showLocationRationale by remember { mutableStateOf(false) }
    var initialCameraSet by remember { mutableStateOf(false) }
    var activeLocationRequest by remember { mutableStateOf<CancellationTokenSource?>(null) }

    DisposableEffect(Unit) {
        onDispose { activeLocationRequest?.cancel() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            activeLocationRequest = requestDeviceLocation(locationClient, onLocation = { location ->
                userLocation = location.toLatLng()
                camera.move(CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f))
                locationMessage = null
            }, onUnavailable = { locationMessage = it })
        } else {
            locationMessage = "A localização foi negada. Você ainda pode acompanhar os ônibus no mapa."
        }
    }

    LaunchedEffect(feed, favoriteIds) {
        if (feed is FeedUiState.Success) {
            cachedFeed = feed
            if (!initialCameraSet) {
                feed.vehicles.firstOrNull {
                    it.id in favoriteIds && it.latitude != null && it.longitude != null
                }?.let { vehicle ->
                    camera.move(CameraUpdateFactory.newLatLngZoom(vehicle.toLatLng(), 12f))
                    initialCameraSet = true
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            refresh()
        }
    }

    val visibleFeed = cachedFeed
    val hasLocationPermission = context.hasLocationPermission()
    val vehicles = visibleFeed?.vehicles.orEmpty().filter {
        it.id in favoriteIds && it.latitude != null && it.longitude != null
    }

    if (showLocationRationale) {
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            title = { Text("Mostrar sua localização") },
            text = { Text("Usaremos sua localização apenas enquanto esta tela estiver aberta para centralizar o mapa. Ela não é armazenada nem usada em segundo plano.") },
            confirmButton = {
                TextButton(onClick = {
                    showLocationRationale = false
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                }) { Text("Permitir") }
            },
            dismissButton = { TextButton(onClick = { showLocationRationale = false }) { Text("Agora não") } }
        )
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Mapa ao vivo") },
            actions = { IconButton(onClick = refresh) { Icon(Icons.Default.Refresh, "Atualizar mapa") } }
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission && userLocation != null),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false)
            ) {
                vehicles.forEach { vehicle ->
                    Marker(
                        state = MarkerState(position = vehicle.toLatLng()),
                        title = "Linha ${vehicle.line}",
                        snippet = vehicle.label,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (vehicle.isStale()) BitmapDescriptorFactory.HUE_ORANGE else BitmapDescriptorFactory.HUE_AZURE
                        ),
                        onClick = { _ -> selectedVehicle = vehicle; true }
                    )
                }
            }

            IconButton(
                onClick = {
                    if (hasLocationPermission) {
                        activeLocationRequest = requestDeviceLocation(locationClient, onLocation = { location ->
                            userLocation = location.toLatLng()
                            camera.move(CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f))
                            locationMessage = null
                        }, onUnavailable = { locationMessage = it })
                    } else showLocationRationale = true
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            ) { Icon(Icons.Default.MyLocation, "Centralizar na minha localização") }

            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = .92f), RoundedCornerShape(12.dp)).padding(12.dp)
            ) {
                when {
                    favoriteIds.isEmpty() -> Text("Salve um ônibus para vê-lo no mapa")
                    visibleFeed == null && feed is FeedUiState.Loading -> Text("Carregando veículos...")
                    visibleFeed == null && feed is FeedUiState.Error -> Text("Não foi possível carregar o mapa. Toque em atualizar.", color = MaterialTheme.colorScheme.error)
                    vehicles.isEmpty() -> Text("Nenhum ônibus salvo com posição disponível.")
                    else -> Text("${vehicles.size} ônibus no mapa")
                }
                Text("Azul: atualizado • Laranja: desatualizado", style = MaterialTheme.typography.labelSmall)
                locationMessage?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            }

            selectedVehicle?.takeIf { selected -> vehicles.any { it.id == selected.id } }?.let { vehicle ->
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Linha ${vehicle.line}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(vehicle.label)
                        Text(vehicle.destination, style = MaterialTheme.typography.bodySmall)
                        Text(vehicle.lastUpdatedText(), style = MaterialTheme.typography.bodySmall, color = if (vehicle.isStale()) MaterialTheme.colorScheme.error else Color.Unspecified)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleListScreen(
    state: FeedUiState,
    favoriteIds: Set<String>,
    refresh: () -> Unit,
    toggle: (Vehicle, Boolean) -> Unit,
    open: (Vehicle) -> Unit
) {
    var query by remember { mutableStateOf("") }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Veículos ao vivo") },
            actions = { IconButton(onClick = refresh) { Icon(Icons.Default.Refresh, "Atualizar") } }
        )
    }) { padding ->
        when (state) {
            FeedUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            is FeedUiState.Error -> ErrorPane(state.message, refresh, Modifier.padding(padding))
            is FeedUiState.Success -> {
                val filtered = state.vehicles.filter {
                    "$it".contains(query, ignoreCase = true) || it.destination.contains(query, true) || it.line.contains(query, true)
                }
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Atualizado ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.fetchedAt))} - ${state.vehicles.size} veículos", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Buscar linha, veículo ou destino") }, singleLine = true)
                    }
                    items(filtered, key = { it.id }) { vehicle ->
                        VehicleCard(vehicle, vehicle.id in favoriteIds, { toggle(vehicle, vehicle.id in favoriteIds) }) { open(vehicle) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesScreen(favorites: List<FavoriteVehicle>, favoriteIds: Set<String>, open: (String) -> Unit) {
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Veículos salvos") }) }) { padding ->
        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text("Nenhum veículo salvo ainda.") }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favorites, key = { it.vehicleId }) { favorite ->
                    VehicleCard(favorite.asVehicle(), favorite.vehicleId in favoriteIds, {}, onClick = { open(favorite.vehicleId) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountScreen(userId: Long, onLogout: () -> Unit) {
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Minha conta") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Person, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Sessão ativa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Usuário #$userId. Seus veículos salvos são privados desta conta.")
            HorizontalDivider()
            OutlinedButton(onClick = onLogout, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sair")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(vehicle: Vehicle?, isFavorite: Boolean, onBack: () -> Unit, onToggle: () -> Unit) {
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Detalhe do veículo") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
        )
    }) { padding ->
        if (vehicle == null) Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text("Veículo não encontrado. Atualize a lista.") }
        else Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Default.DirectionsBus, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Text(vehicle.label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            DetailRow("Linha", vehicle.line)
            DetailRow("Destino", vehicle.destination)
            DetailRow("Localização", vehicle.coordinatesText())
            vehicle.speedKmh?.let { DetailRow("Velocidade", "$it km/h") }
            FilledTonalButton(onClick = onToggle, Modifier.fillMaxWidth()) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                Spacer(Modifier.width(8.dp)); Text(if (isFavorite) "Remover dos salvos" else "Salvar veículo")
            }
        }
    }
}

@Composable private fun DetailRow(label: String, value: String) = Row {
    Text("$label: ", fontWeight = FontWeight.Bold); Text(value)
}

@Composable
private fun VehicleCard(vehicle: Vehicle, favorite: Boolean, toggle: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DirectionsBus, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Linha ${vehicle.line}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(vehicle.destination, fontWeight = FontWeight.SemiBold)
                Text(vehicle.label, style = MaterialTheme.typography.bodySmall)
                Text(vehicle.coordinatesText(), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = toggle) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Salvar") }
        }
    }
}

@Composable
private fun ErrorPane(message: String, retry: () -> Unit, modifier: Modifier = Modifier) = Box(Modifier.fillMaxSize().then(modifier), Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text("Não foi possível atualizar", style = MaterialTheme.typography.titleLarge)
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp)); Button(onClick = retry) { Text("Tentar novamente") }
    }
}

private fun FavoriteVehicle.asVehicle() = Vehicle(vehicleId, label, line, destination, latitude, longitude, null, null)
private fun Vehicle.coordinatesText() = if (latitude != null && longitude != null) "${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}" else "Localização não informada"

private fun Vehicle.toLatLng() = LatLng(requireNotNull(latitude), requireNotNull(longitude))

private fun Vehicle.isStale(now: Long = System.currentTimeMillis()): Boolean {
    val vehicleTimestamp = timestamp ?: return true
    val millis = if (vehicleTimestamp < 100_000_000_000L) vehicleTimestamp * 1_000 else vehicleTimestamp
    return now - millis > 5 * 60 * 1_000
}

private fun Vehicle.lastUpdatedText(): String {
    val vehicleTimestamp = timestamp ?: return "Atualização não informada"
    val millis = if (vehicleTimestamp < 100_000_000_000L) vehicleTimestamp * 1_000 else vehicleTimestamp
    return "Atualizado ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))}" +
        if (isStale()) " • posição desatualizada" else ""
}

private fun Context.hasLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Location.toLatLng() = LatLng(latitude, longitude)

private fun requestDeviceLocation(
    client: com.google.android.gms.location.FusedLocationProviderClient,
    onLocation: (Location) -> Unit,
    onUnavailable: (String) -> Unit
): CancellationTokenSource {
    val token = CancellationTokenSource()
    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
        .addOnSuccessListener { location ->
            if (location == null) onUnavailable("Não foi possível obter sua localização. Ative o GPS e tente novamente.")
            else onLocation(location)
        }
        .addOnFailureListener { onUnavailable("Não foi possível obter sua localização. Verifique o GPS e tente novamente.") }
    return token
}
