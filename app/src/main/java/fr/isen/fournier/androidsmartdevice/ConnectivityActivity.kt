package fr.isen.fournier.androidsmartdevice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.sp
import fr.isen.fournier.androidsmartdevice.composable.CheckboxListener1
import fr.isen.fournier.androidsmartdevice.composable.CheckboxListener2
import fr.isen.fournier.androidsmartdevice.composable.DeviceDetails
import java.util.UUID

enum class ImageId {
    FIRST_IMAGE,
    SECOND_IMAGE,
    THIRD_IMAGE
}

interface ImageClickListener {
    fun onImageClicked(imageId: ImageId)
}

class ConnectivityActivity : ComponentActivity(), ImageClickListener, CheckboxListener1, CheckboxListener2 {

    private var notificationCounter by mutableStateOf(0)

    private fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    private fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
        properties and property != 0

    private var gatt: BluetoothGatt? = null
    private var isConnecting by mutableStateOf(true)

    private var services: List<BluetoothGattService>? = null
    private var service: BluetoothGattService? = null

    private var notificationCharacteristic1: BluetoothGattCharacteristic? = null // Service 3, Char 2
    private var notificationCharacteristic2: BluetoothGattCharacteristic? = null // Service 4, Char 1

    private var isFirstImageClicked: Boolean = false
    private var isSecondImageClicked: Boolean = false
    private var isThirdImageClicked: Boolean = false

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var lastValue: ByteArray? = null
    private var lastCounterUpdateTime = 0L

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceAddress = intent.getStringExtra("device_address") ?: return
        val deviceName = intent.getStringExtra("device_name") ?: "Unknown Device"
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)

        setContent {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Contrôle des LEDs",
                                fontSize = 20.sp,
                                color = Color.Black
                            ) },
                        navigationIcon = {
                            IconButton(onClick = { goBackToScan() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = Color.Black
                                )
                            } },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.LightGray,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                }
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = Color.White
                ) {
                    Column {
                        DeviceDetailsScreen()
                        DeviceDetails(
                            deviceName = deviceName,
                            clickListener = this@ConnectivityActivity,
                            listenercheckbox1 = this@ConnectivityActivity,
                            listenercheckbox2 = this@ConnectivityActivity,
                            counter = notificationCounter // Passe le compteur au composable
                        )
                        if (device != null) {
                            connectToDevice(device)
                        }
                    }
                }
            }
        }
    }

    override fun onImageClicked(imageId: ImageId) {
        when (imageId) {
            ImageId.FIRST_IMAGE -> {
                if (!isFirstImageClicked) {
                    val valueToWrite1 = byteArrayOf(0x01)
                    writeValueToCharacteristic(valueToWrite1)
                    isFirstImageClicked = true
                } else {
                    val valueToWrite4 = byteArrayOf(0x00)
                    writeValueToCharacteristic(valueToWrite4)
                    isFirstImageClicked = false
                }
            }
            ImageId.SECOND_IMAGE -> {
                if (!isSecondImageClicked) {
                    val valueToWrite2 = byteArrayOf(0x02)
                    writeValueToCharacteristic(valueToWrite2)
                    isSecondImageClicked = true
                } else {
                    val valueToWrite4 = byteArrayOf(0x00)
                    writeValueToCharacteristic(valueToWrite4)
                    isSecondImageClicked = false
                }
            }
            ImageId.THIRD_IMAGE -> {
                if (!isThirdImageClicked) {
                    val valueToWrite3 = byteArrayOf(0x03)
                    writeValueToCharacteristic(valueToWrite3)
                    isThirdImageClicked = true
                } else {
                    val valueToWrite4 = byteArrayOf(0x00)
                    writeValueToCharacteristic(valueToWrite4)
                    isThirdImageClicked = false
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        gatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnecting = false
                gatt?.discoverServices()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                services = gatt?.services
                if (services != null && services!!.size >= 4) { // Vérifie qu'il y a au moins 4 services

                    // Service 3 (index 2) - Caractéristique 2 (index 1)
                    val thirdService = services!![2]
                    if (thirdService.characteristics.size >= 2) {
                        notificationCharacteristic1 = thirdService.characteristics[1]
                        Log.d("Bluetooth", "Caractéristique 1 trouvée: ${notificationCharacteristic1?.uuid}")
                    }

                    // Service 4 (index 3) - Caractéristique 1 (index 0)
                    val fourthService = services!![3]
                    if (fourthService.characteristics.isNotEmpty()) {
                        notificationCharacteristic2 = fourthService.characteristics[0]
                        Log.d("Bluetooth", "Caractéristique 2 trouvée: ${notificationCharacteristic2?.uuid}")
                    }
                }
            }
        }

        //Méthode dépréciée pour Android < 13
        @SuppressLint("MissingPermission", "Deprecation")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            characteristic?.let { char ->
                handleCharacteristicChanged(gatt, char, char.value ?: byteArrayOf())
            }
        }

        //Méthode pour Android 13+
        @OptIn(ExperimentalStdlibApi::class)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicChanged(gatt, characteristic, value)
        }

        //Gestion des notifications reçues
        private fun handleCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            runOnUiThread {
                notificationCounter++ //Même compteur pour les deux sources

                //Identification de la source
                val sourceName = when (characteristic.uuid) {
                    notificationCharacteristic1?.uuid -> "Bouton 1 (Service 3)"
                    notificationCharacteristic2?.uuid -> "Bouton 3 (Service 4)"
                    else -> "Inconnu"
                }

                val hexValue = value.joinToString("") { "%02x".format(it) }
                Log.d("Bluetooth", "Notification de $sourceName: 0x$hexValue")

                Toast.makeText(
                    this@ConnectivityActivity,
                    "Notification de $sourceName\nCompteur: $notificationCounter",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeValueToCharacteristic(value: ByteArray) {
        service = gatt?.services?.getOrNull(2)
        val characteristic = service?.characteristics?.getOrNull(0)

        if (characteristic != null) {
            characteristic.value = value
            gatt?.writeCharacteristic(characteristic)
        }
    }

    // Implémentation de CheckboxListener1 (Bouton 1 - Service 3)
    override fun onCheckbox1Checked(checked: Boolean) {
        notificationCharacteristic1?.let { characteristic ->
            if (checked) {
                enableNotifications(characteristic)
                Toast.makeText(this, "Abonnement Bouton 1 activé", Toast.LENGTH_SHORT).show()
            } else {
                disableNotifications(characteristic)
                Toast.makeText(this, "Abonnement Bouton 1 désactivé", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Caractéristique Bouton 1 non trouvée", Toast.LENGTH_LONG).show()
        }
    }

    // Implémentation de CheckboxListener2 (Bouton 3 - Service 4)
    override fun onCheckbox2Checked(checked: Boolean) {
        notificationCharacteristic2?.let { characteristic ->
            if (checked) {
                enableNotifications(characteristic)
                Toast.makeText(this, "Abonnement Bouton 3 activé", Toast.LENGTH_SHORT).show()
            } else {
                disableNotifications(characteristic)
                Toast.makeText(this, "Abonnement Bouton 3 désactivé", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Caractéristique Bouton 3 non trouvée", Toast.LENGTH_LONG).show()
        }
    }

    // Méthode commune pour activer les notifications
    @SuppressLint("MissingPermission")
    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable()) {
            Toast.makeText(this, "Cette caractéristique ne supporte pas les notifications", Toast.LENGTH_LONG).show()
            return
        }

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        characteristic.getDescriptor(cccdUuid)?.let { descriptor ->
            gatt?.setCharacteristicNotification(characteristic, true)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt?.writeDescriptor(descriptor)
        } ?: run {
            Toast.makeText(this, "Descripteur CCCD introuvable", Toast.LENGTH_LONG).show()
        }
    }

    // Méthode commune pour désactiver les notifications
    @SuppressLint("MissingPermission")
    private fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        characteristic.getDescriptor(cccdUuid)?.let { descriptor ->
            gatt?.setCharacteristicNotification(characteristic, false)
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            gatt?.writeDescriptor(descriptor)
        }
    }


    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        gatt?.disconnect()
        gatt?.close()
    }

    @Composable
    fun DeviceDetailsScreen() {
        Text(
            text = if (isConnecting) "Connexion en cours" else "Connexion établie",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center,
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
    }

    private fun goBackToScan() {
        val intent = Intent(this, ScanActivity::class.java)
        startActivity(intent)
        finish()
    }
}