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

private const val TAG = "BluetoothLeService"

class ConnectivityActivity : ComponentActivity(), ImageClickListener {

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

    private var characteristicrec: BluetoothGattCharacteristic? = null

    private var isFirstImageClicked: Boolean = false
    private var isSecondImageClicked: Boolean = false
    private var isThirdImageClicked: Boolean = false

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

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
                        DeviceDetails(deviceName, this@ConnectivityActivity)
                        if (device != null) {
                            connectToDevice(device)
                        }
                    }
                }
            }
        }
    }

    //Fonction pour exécuter les différent code pour chaque led
    override fun onImageClicked(imageId: ImageId) {
        when (imageId) {
            ImageId.FIRST_IMAGE -> {
                if (!isFirstImageClicked) {
                    val valueToWrite1 = byteArrayOf(0x01)
                    Log.e("write", "write")
                    writeValueToCharacteristic(valueToWrite1)
                    isFirstImageClicked = true
                }else{
                    val valueToWrite4 = byteArrayOf(0x00)
                    writeValueToCharacteristic(valueToWrite4)
                    isFirstImageClicked = false
                }
            }
            ImageId.SECOND_IMAGE -> {
                if (!isSecondImageClicked) {
                    val valueToWrite2 = byteArrayOf(0x02)
                    Log.e("write", "write")
                    writeValueToCharacteristic(valueToWrite2)
                    isSecondImageClicked = true
                }else{
                    val valueToWrite4 = byteArrayOf(0x00)
                    writeValueToCharacteristic(valueToWrite4)
                    isSecondImageClicked = false
                }
            }
            ImageId.THIRD_IMAGE -> {
                if (!isThirdImageClicked) {
                    val valueToWrite3 = byteArrayOf(0x03)
                    Log.e("write", "write")
                    writeValueToCharacteristic(valueToWrite3)
                    isThirdImageClicked = true
                }else{
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
            Log.e("callbackgatt", "Callback gatt")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("callbackgatt", "Callback gatt connected")
                isConnecting = false
                gatt?.discoverServices()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.e("onservices","OnServiceDiscovered")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                services = gatt?.services
                if (services != null && services!!.isNotEmpty()) {
                    Log.e("ServiceListSize", "Nombre de services découverts : ${services!!.size}")
                    if(services!!.size >= 3){
                        val thirdService = services!![2] //Troisième service
                        val characteristics = thirdService.characteristics
                        if (characteristics.isNotEmpty()) {
                            val firstCharacteristic = characteristics[0] //Première caractéristique
                            Log.e("charac","Acces à la première caractéristique du troisième service")
                            //Activation des notifications
                            gatt?.setCharacteristicNotification(firstCharacteristic, true)
                            val descriptor = firstCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt?.writeDescriptor(descriptor)


                        } else {
                            Log.e("charac","Aucune caractéristique dans le troisième service")
                        }
                    }
                } else {
                    Log.e("charac","L'appareil ne dispose pas de suffisamment de services")
                }
            } else {
                Log.e("charac","Erreur lors de la découverte des services")
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        @Deprecated("Deprecated for Android 13+")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: ${value.toHexString()}")
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val newValueHex = value.toHexString()
            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: $newValueHex")
            }
        }
    }

    //Fonction pour écrire dans la caractéristique
    @SuppressLint("MissingPermission")
    private fun writeValueToCharacteristic(value: ByteArray) {
        if (gatt != null) {
            Log.e("write1","write1")
            service = gatt?.services?.get(2)
            Log.e("write2","write2")

            if (service != null && service!!.characteristics.isNotEmpty()) {
                Log.e("write3","write3")
                val characteristic = service!!.characteristics[0]
                characteristicrec = service!!.characteristics[1]
                if (characteristic != null) {
                    Log.e("write4","write4")
                    characteristic.value = value
                    gatt?.writeCharacteristic(characteristic)
                } else {
                    Log.e("writeValueToCharacteristic", "Caractéristique non valide")
                }
            } else {
                Log.e("writeValueToCharacteristic", "Aucune caractéristique dans le troisième service")
            }
        } else {
            Log.e("writeValueToCharacteristic", "Connexion Bluetooth non établie")
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
        if (!isConnecting) {
            Text(
                text = "Connexion établie",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                )
            )
        } else {
            Text(
                text = "Connexion en cours",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        gatt?.let { gatt ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, payload)
            } else {
                //Passe à la version depreciée si la version Android < 13
                gatt.legacyDescriptorWrite(descriptor, payload)
            }
        } ?: error("Not connected to a BLE device!")
    }

    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.S)
    @Suppress("DEPRECATION")
    private fun BluetoothGatt.legacyDescriptorWrite(
        descriptor: BluetoothGattDescriptor,
        value: ByteArray
    ) {
        descriptor.value = value
        writeDescriptor(descriptor)
    }

    private fun goBackToScan() {
        val intent = Intent(this, ScanActivity::class.java)
        startActivity(intent)
        finish()
    }
}