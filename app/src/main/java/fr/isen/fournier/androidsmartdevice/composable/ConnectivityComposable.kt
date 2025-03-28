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

interface CheckboxListener {
    fun onCheckboxChecked(checked: Boolean)
}

@Composable
fun DeviceDetails(deviceName: String, clickListener: ImageClickListener, listenercheckbox: CheckboxListener, counter: Int) {
    var isChecked by remember { mutableStateOf(false) }
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
            var clicked_image1 by remember { mutableStateOf(false) }
            var clicked_image2 by remember { mutableStateOf(false) }
            var clicked_image3 by remember { mutableStateOf(false) }
            Image(
                painter = imageResource,
                contentDescription = "LED 1",
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        clicked_image1 = !clicked_image1
                        clickListener.onImageClicked(ImageId.FIRST_IMAGE)
                    },
                colorFilter = if (clicked_image1) ColorFilter.tint(Color.Blue)
                else ColorFilter.tint(Color.Black)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Image(
                painter = imageResource,
                contentDescription = "LED 2",
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        clicked_image2 = !clicked_image2
                        clickListener.onImageClicked(ImageId.SECOND_IMAGE)
                    },
                colorFilter = if (clicked_image2) ColorFilter.tint(Color.Green)
                else ColorFilter.tint(Color.Black)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Image(
                painter = imageResource,
                contentDescription = "LED 3",
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        clicked_image3 = !clicked_image3
                        clickListener.onImageClicked(ImageId.THIRD_IMAGE)
                    },
                colorFilter = if (clicked_image3) ColorFilter.tint(Color.Red)
                else ColorFilter.tint(Color.Black)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Column {
            Text(
                text = "Abonnez-vous pour recevoir le nombre d'incrémentation",
                style = TextStyle(fontSize = 17.sp),
                textAlign = Center,
                modifier = Modifier.padding(8.dp)
            )
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        isChecked = checked
                        listenercheckbox.onCheckboxChecked(checked)
                    },
                    modifier = Modifier
                        .size(24.dp)
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