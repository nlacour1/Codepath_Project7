package com.example.codepath_project7

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.codepath_project7.ui.theme.CodepathProject7Theme
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodepathProject7Theme {
                PokemonApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonApp() {
    var pokemonList by remember { mutableStateOf(listOf<Pokemon>()) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pokémon List") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Button(
                onClick = {
                    isLoading = true
                    fetchPokemonList { list ->
                        pokemonList = list
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            ) {
                Text("Load Pokémon")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                PokemonList(pokemonList)
            }
        }
    }
}

@Composable
fun PokemonList(pokemonList: List<Pokemon>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(pokemonList) { pokemon ->
            PokemonItem(pokemon)
        }
    }
}

@Composable
fun PokemonItem(pokemon: Pokemon) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(pokemon.imageUrl),
            contentDescription = pokemon.name,
            modifier = Modifier
                .size(80.dp)
                .padding(end = 16.dp)
        )
        Column {
            Text(
                text = pokemon.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Type: ${pokemon.type}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

fun fetchPokemonList(onResult: (List<Pokemon>) -> Unit) {
    val client = AsyncHttpClient()
    val url = "https://pokeapi.co/api/v2/pokemon?limit=20"

    client.get(url, object : JsonHttpResponseHandler() {
        override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
            response?.let {
                val results = it.getJSONArray("results")
                val pokemonList = mutableListOf<Pokemon>()

                for (i in 0 until results.length()) {
                    val entry = results.getJSONObject(i)
                    val name = entry.getString("name")
                    val detailsUrl = entry.getString("url")

                    fetchPokemonDetails(name, detailsUrl) { pokemon ->
                        if (pokemon != null) {
                            pokemonList.add(pokemon)
                        }

                        if (pokemonList.size == results.length()) {
                            onResult(pokemonList)
                        }
                    }
                }
            }
        }

        override fun onFailure(statusCode: Int, headers: Array<out Header>?, throwable: Throwable?, errorResponse: JSONObject?) {
            onResult(emptyList())
        }
    })
}

fun fetchPokemonDetails(name: String, url: String, onResult: (Pokemon?) -> Unit) {
    val client = AsyncHttpClient()

    client.get(url, object : JsonHttpResponseHandler() {
        override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
            response?.let {
                val typesArray = it.getJSONArray("types")
                val types = StringBuilder()
                for (i in 0 until typesArray.length()) {
                    val type = typesArray.getJSONObject(i).getJSONObject("type").getString("name")
                    types.append(type).append(if (i < typesArray.length() - 1) ", " else "")
                }

                val imageUrl = it.getJSONObject("sprites").getString("front_default")

                val pokemon = Pokemon(name, types.toString(), imageUrl)
                onResult(pokemon)
            }
        }

        override fun onFailure(statusCode: Int, headers: Array<out Header>?, throwable: Throwable?, errorResponse: JSONObject?) {
            onResult(null)
        }
    })
}
