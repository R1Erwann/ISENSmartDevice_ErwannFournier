package fr.isen.fournier.androidsmartdevice.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import fr.isen.fournier.androidsmartdevice.ImageClickListener
import fr.isen.fournier.androidsmartdevice.ImageId
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import fr.isen.fournier.androidsmartdevice.R

interface CheckboxListener1 {
    fun onCheckbox1Checked(checked: Boolean)
}

interface CheckboxListener2 {
    fun onCheckbox2Checked(checked: Boolean)
}

@Composable
fun DeviceDetails(
    deviceName: String,
    clickListener: ImageClickListener,
    listenercheckbox1: CheckboxListener1,
    listenercheckbox2: CheckboxListener2,
    counter: Int
) {
    var isChecked1 by remember { mutableStateOf(false) }
    var isChecked2 by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = deviceName,
            style = TextStyle(fontSize = 20.sp),
            color = Color(0xFF87CEEB),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Affichage des differentes LEDs",
            style = TextStyle(fontSize = 18.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val imageResource = painterResource(id = R.drawable.led)
            var clicked_fimage by remember { mutableStateOf(false) }
            var clicked_simage by remember { mutableStateOf(false) }
            var clicked_timage by remember { mutableStateOf(false) }

            Image(
                painter = imageResource,
                contentDescription = "LED 1",
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        clicked_fimage = !clicked_fimage
                        clickListener.onImageClicked(ImageId.FIRST_IMAGE)
                    },
                colorFilter = if (clicked_fimage) ColorFilter.tint(Color.Blue)
                else ColorFilter.tint(Color.Black)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Image(
                painter = imageResource,
                contentDescription = "LED 2",
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        clicked_simage = !clicked_simage
                        clickListener.onImageClicked(ImageId.SECOND_IMAGE)
                    },
                colorFilter = if (clicked_simage) ColorFilter.tint(Color.Green)
                else ColorFilter.tint(Color.Black)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Image(
                painter = imageResource,
                contentDescription = "LED 3",
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        clicked_timage = !clicked_timage
                        clickListener.onImageClicked(ImageId.THIRD_IMAGE)
                    },
                colorFilter = if (clicked_timage) ColorFilter.tint(Color.Red)
                else ColorFilter.tint(Color.Black)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Nouvelle Row pour les deux Checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox 1
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Bouton 1",
                    style = TextStyle(fontSize = 16.sp),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Checkbox(
                    checked = isChecked1,
                    onCheckedChange = { checked ->
                        isChecked1 = checked
                        listenercheckbox1.onCheckbox1Checked(checked)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Checkbox 2
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Bouton 3",
                    style = TextStyle(fontSize = 16.sp),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Checkbox(
                    checked = isChecked2,
                    onCheckedChange = { checked ->
                        isChecked2 = checked
                        listenercheckbox2.onCheckbox2Checked(checked)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text(
                text = "Nombre : ",
                style = TextStyle(fontSize = 17.sp),
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                text = counter.toString(),
                style = TextStyle(fontSize = 17.sp),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}