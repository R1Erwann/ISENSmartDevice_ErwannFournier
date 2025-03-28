package fr.isen.fournier.androidsmartdevice

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.app.AlertDialog
import android.app.Activity
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import fr.isen.fournier.androidsmartdevice.composable.ScanScreen

class ScanActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter?.bluetoothLeScanner
    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                toggleScan()
            } else {
                // Cas où une ou plusieurs permissions sont refusées
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Permissions non accordées")
                builder.setMessage("Toutes les permissions nécéssaires à l'application ne sont pas accéptées")
                builder.setPositiveButton("OK") { _, _ ->
                    this.finishAffinity()
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
    //Liste des devices trouvé
    private val devices = mutableStateListOf<ScanResult>()
    //Scan est en cours ou non
    private var connectionStatus by mutableStateOf(true)

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            addDeviceIfNotExists(result)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanActivity", "Scan échoué avec le code d'erreur : $errorCode")
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Scan Bluetooth",
                                fontSize = 20.sp,
                                color = Color.Black
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { goBack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = Color.Black
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.LightGray,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                }) { innerPadding ->
                ScanScreen(
                    innerPadding = innerPadding,
                    bleList = devices,
                    toggleScan = {
                        connectionStatus = !connectionStatus
                        toggleScan()
                    },
                    scanning = connectionStatus,
                    connection = { selectedDevice -> goToConnection(selectedDevice) }
                )
            }
        }
        initScanBLE()
    }

    //Lance les différentes vérifications du Bluetooth
    private fun initScanBLE() {
        if (checkBluetoothAvailable(this)) {
            if (!allPermissionGranted()) {
                requestPermissionLauncher.launch(getAllPermissionsForBLE())
            }
            checkBluetoothActivated()
        }
    }


    //Vérifie si le Bluetooth est présent sur l'appareil ou non
    private fun checkBluetoothAvailable(activity: Activity): Boolean {
        if (bluetoothAdapter != null) {
            return true
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Bluetooth indisponible")
            builder.setMessage("Votre appareil ne prend pas en charge le bluetooth")
            builder.setPositiveButton("OK") { _, _ ->
                activity.finishAffinity()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
            return false
        }
    }

    //Vérifie si le Bluetooth est activé ou non
    private fun checkBluetoothActivated() {
        if (bluetoothAdapter!!.isEnabled) {
            if (isLocationEnabled()) {
                toggleScan()
            } else {
                requestLocationActivation()
            }
        } else {
            requestBluetoothActivation()
        }

    }
    //Demande à l'utilisateur d'activé son Bluetooth
    private fun requestBluetoothActivation() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        val bluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    Log.d("ScanActivity", "Bluetooth activé")
                }
            }
        bluetoothLauncher.launch(enableBtIntent)
    }

    private fun isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Pour Android 9 (Pie) et supérieur
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager.isLocationEnabled
        } else {
            // Pour les versions antérieures
            val mode = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            mode != Settings.Secure.LOCATION_MODE_OFF
        }
    }

    private fun requestLocationActivation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Localisation requise")
        builder.setMessage("Pour scanner les appareils Bluetooth, les services de localisation doivent être activés")
        builder.setPositiveButton("Activer") { _, _ ->
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        builder.setNegativeButton("Annuler") { _, _ ->
            finish()
        }
        builder.show()
    }

    //Vérifie si toutes les permissions sont autorisés
    private fun allPermissionGranted(): Boolean {
        val allPermission = getAllPermissionsForBLE()
        return allPermission.all { permission ->
            ActivityCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    //Vérifie les permissions pour le Bluetooth et la localisation
    private fun getAllPermissionsForBLE(): Array<String> {
        var allPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allPermissions = allPermissions.plus(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            allPermissions = allPermissions.plus(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
        return allPermissions
    }

    //Permet de savoir s'il doit lancer ou pas le scan
    private fun toggleScan() {
        if (connectionStatus) {
            stopScan()
        } else {
            startScan()
        }
    }

    //Méthode pour démarrer le scan
    private fun startScan() {
        if (!isLocationEnabled()) {
            requestLocationActivation()
            return
        }
        try {
            devices.clear()
            bluetoothLeScanner?.startScan(leScanCallback)
        } catch (e: SecurityException) {
            Log.e("ScanActivity", "Permissions manquantes : ${e.message}")
        }
    }
    //Arrête le scan
    private fun stopScan() {
        try {
            bluetoothLeScanner?.stopScan(leScanCallback)
        } catch (e: SecurityException) {
            Log.e("ScanActivity", "Permissions manquantes pour arrêter le scan : ${e.message}")
        }
    }

    //Ajoute le périphérique dans la liste de ceux trouvés si les conditions sont respectées
    private fun addDeviceIfNotExists(result: ScanResult) {
        //Vérifier si les permissions Bluetooth sont accordées
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            //Vérifier si l'adresse MAC du périphérique n'est pas déjà dans la liste
            if (devices.none { it.device.address == result.device.address }) {
                //Ajoute seulement les périphériques qui ont un nom
                if (result.device.name != null) {
                    devices.add(result)
                }
            }
        }
    }

    //Fonction pour revenir en arrière
    private fun goBack(){
        if (connectionStatus){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        else{
            toggleScan()
            connectionStatus = true
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    //Fonction pour aller à la page d'interraction
    @SuppressLint("MissingPermission")
    private fun goToConnection(selectedDevice: ScanResult){
        if (connectionStatus){
            val intent = Intent(this, ConnectivityActivity::class.java)
            intent.putExtra("device_name", selectedDevice.device.name ?: "Unknown Device")
            intent.putExtra("device_address", selectedDevice.device.address)
            startActivity(intent)
        }
        else{
            connectionStatus = true
            toggleScan()
            val intent = Intent(this, ConnectivityActivity::class.java)
            intent.putExtra("device_name", selectedDevice.device.name ?: "Unknown Device")
            intent.putExtra("device_address", selectedDevice.device.address)
            startActivity(intent)
        }
    }
}