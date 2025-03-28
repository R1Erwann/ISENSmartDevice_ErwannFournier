package fr.isen.fournier.androidsmartdevice

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import fr.isen.fournier.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme
import fr.isen.fournier.androidsmartdevice.composable.MainContentComponent



class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSmartDeviceTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Android Smart Device",
                                    fontSize = 20.sp,
                                    color = Color.Black
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { this.quit() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fermer",
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
                    }
                ) { innerPadding ->
                    MainContentComponent(
                        innerPadding = innerPadding,
                        onButtonClick = { this.goToScan() }
                    )
                }
            }
        }
    }

    private fun goToScan() {
        val intent = Intent(this, ScanActivity::class.java)
        startActivity(intent)
    }

    private fun quit() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Confirmer la sortie")
        dialogBuilder.setMessage("Êtes-vous sûr de vouloir quitter l'application ?")

        // Bouton "Oui" pour confirmer
        dialogBuilder.setPositiveButton("Oui") { _, _ ->
            this.finishAffinity()
        }

        // Bouton "Non" pour annuler
        dialogBuilder.setNegativeButton("Non") { dialog, _ ->
            dialog.dismiss()
        }

        // Créer et afficher la boîte de dialogue
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }
}