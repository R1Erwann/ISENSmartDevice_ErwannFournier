package fr.isen.fournier.androidsmartdevice

import android.annotation.SuppressLint
import android.annotation.TargetApi
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
import fr.isen.fournier.androidsmartdevice.composable.CheckboxListener
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

class ConnectivityActivity : ComponentActivity(), ImageClickListener, CheckboxListener {

    private var notificationCounter by mutableStateOf(0)

    private fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    private fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
        properties and property != 0

    private var gatt: BluetoothGatt? = null
    private var isConnecting by mutableStateOf(true)

    private var characteristicValue: ByteArray? = byteArrayOf()
    private var characteristic: BluetoothGattCharacteristic? = null

    private var services: List<BluetoothGattService>? = null
    private var service: BluetoothGattService? = null

    private var notificationCharacteristic: BluetoothGattCharacteristic? = null

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
                            listenercheckbox = this@ConnectivityActivity,
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
                if (services != null && services!!.size >= 3) {
                    val thirdService = services!![2]
                    if (thirdService.characteristics.size >= 2) {
                        notificationCharacteristic = thirdService.characteristics[1]
                        Log.d("Bluetooth", "Characteristic for notifications found")
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

        //Fonction commune de traitement
        private fun handleCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            // Ignorer les valeurs identiques reçues trop rapidement
            if (value.contentEquals(lastValue) &&
                System.currentTimeMillis() - lastCounterUpdateTime < 1500) {
                return
            }

            lastValue = value
            lastCounterUpdateTime = System.currentTimeMillis()

            runOnUiThread {
                notificationCounter++
                val hexValue = value.joinToString("") { "%02x".format(it) }

                Toast.makeText(
                    this@ConnectivityActivity,
                    "▼ Notification ▼\nCompteur: $notificationCounter\nValeur: 0x$hexValue",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("Bluetooth", "Notification reçue (${characteristic.uuid}): $hexValue")
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

    override fun onCheckboxChecked(checked: Boolean) {
        notificationCharacteristic?.let { characteristic ->
            if (checked) {
                enableNotifications(characteristic)
                Toast.makeText(this, "Abonnement activé", Toast.LENGTH_SHORT).show()
            } else {
                disableNotifications(characteristic)
                Toast.makeText(this, "Abonnement désactivé", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Caractéristique non trouvée", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable()) {
            Log.e("Bluetooth", "La caractéristique ne supporte pas les notifications")
            return
        }

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        characteristic.getDescriptor(cccdUuid)?.let { descriptor ->
            gatt?.setCharacteristicNotification(characteristic, true)
            Log.d("Bluetooth", "Notification activée sur ${characteristic.uuid}")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt?.writeDescriptor(descriptor)
        } ?: run {
            Log.e("Bluetooth", "Descripteur CCCD non trouvé")
        }
    }

    @SuppressLint("MissingPermission")
    private fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        characteristic.getDescriptor(cccdUuid)?.let { descriptor ->
            gatt?.setCharacteristicNotification(characteristic, false)
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            gatt?.writeDescriptor(descriptor)
        } ?: run {
            Log.e("Bluetooth", "Descripteur CCCD non trouvé")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        notificationCharacteristic?.let { disableNotifications(it) }
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