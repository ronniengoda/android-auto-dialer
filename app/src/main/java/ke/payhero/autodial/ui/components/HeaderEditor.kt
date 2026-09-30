package ke.payhero.autodial.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
fun HeaderEditor(
    headersJson: String,
    onHeadersChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val headersMap = try {
        val obj = JSONObject(headersJson)
        val map = mutableListOf<Pair<String, String>>()
        obj.keys().forEach { k -> map.add(k to obj.optString(k)) }
        if (map.isEmpty()) map.add("" to "")
        map
    } catch (_: Exception) {
        mutableListOf("" to "")
    }

    fun updateJson(newList: List<Pair<String, String>>) {
        val obj = JSONObject()
        newList.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) {
                obj.put(k.trim(), v.trim())
            }
        }
        onHeadersChanged(obj.toString())
    }

    Column(modifier = modifier) {
        Text(
            text = "Custom HTTP Headers",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        headersMap.forEachIndexed { index, (key, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { newKey ->
                        val updated = headersMap.toMutableList()
                        updated[index] = newKey to updated[index].second
                        updateJson(updated)
                    },
                    label = { Text("Header Key") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = { newVal ->
                        val updated = headersMap.toMutableList()
                        updated[index] = updated[index].first to newVal
                        updateJson(updated)
                    },
                    label = { Text("Header Value") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        val updated = headersMap.toMutableList()
                        updated.removeAt(index)
                        if (updated.isEmpty()) updated.add("" to "")
                        updateJson(updated)
                    }
                ) {
                    Icon(
                        imageVector = FeatherIcons.Trash,
                        contentDescription = "Remove Header",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                val updated = headersMap.toMutableList()
                updated.add("" to "")
                updateJson(updated)
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(FeatherIcons.Plus, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Header")
        }
    }
}
