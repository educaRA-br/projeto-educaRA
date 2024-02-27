@file:Suppress("DEPRECATION")
package edu.ifba.educa_ra.api

import android.os.AsyncTask
import android.util.Log
import edu.ifba.educa_ra.modelo.Aula
import edu.ifba.educa_ra.modelo.Conteudo
import edu.ifba.educa_ra.modelo.Disciplina
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile
import kotlin.reflect.KFunction1

//const val URL_SERVICOS = "http://10.0.2.2"
const val URL_SERVICOS = "http://192.168.15.39"
const val URL_ALIVE = "$URL_SERVICOS:3001/alive"
const val URL_DISCIPLINAS = "$URL_SERVICOS:3001/disciplinas"
const val URL_AULAS = "$URL_SERVICOS:3001/aulas"
const val URL_CONTEUDOS = "$URL_SERVICOS:3001/conteudos"

const val URL_OBJETO = "$URL_SERVICOS:3003"

fun toJson(stream: InputStream): String {
    val json = StringBuilder()

    val reader = BufferedReader(InputStreamReader(stream, "utf-8"))
    var linha: String?
    while (reader.readLine().also { linha = it } != null) {
        json.append(linha!!.trim { it <= ' ' })
    }

    return json.toString()
}

fun isAlive(): Boolean {
    var alive = false

    try {
        val url = URL(URL_ALIVE)
        val conn = url.openConnection() as HttpURLConnection
        val json = toJson(conn.inputStream)

        val resposta = JSONObject(json)
        alive = resposta.getBoolean("alive")
    } catch (e: Exception) {
        Log.e("isAlive()", e.toString())
    }

    return alive
}

class IsAlive(private val onAlive: KFunction1<Boolean, Unit>) :
    AsyncTask<Void, Void, Boolean>() {

    override fun doInBackground(vararg params: Void?): Boolean {
        var alive = false

        try {
            alive = isAlive()
        } catch (e: Exception) {
            Log.e("IsAlive()", e.toString())
        }

        return alive
    }

    override fun onPostExecute(alive: Boolean) {
        super.onPostExecute(alive)

        onAlive(alive)
    }

}

class GetDisciplinas(private val onDisciplinas: KFunction1<List<Disciplina>, Unit>) :
    AsyncTask<Void, Void, List<Disciplina>>() {

    private fun toDisciplinas(objetos: JSONArray): List<Disciplina> {
        val disciplinas = arrayListOf<Disciplina>()

        for (i in 0 until objetos.length()) {
            val objeto = objetos.get(i) as JSONObject

            val disciplina = Disciplina(
                objeto.getString("id"),
                objeto.getString("nome"),
                objeto.getString("detalhes"),
                arrayListOf<Aula>())

            disciplinas.add(disciplina)
        }

        return disciplinas
    }

    override fun doInBackground(vararg v: Void): List<Disciplina> {
        var disciplinas: List<Disciplina> = arrayListOf()

        try {
            if (isAlive()) {
                val url = URL(URL_DISCIPLINAS)
                val conn = url.openConnection() as HttpURLConnection
                val json = toJson(conn.inputStream)

                val objetos = JSONArray(json)
                disciplinas = toDisciplinas(objetos)
            }
        } catch (e: Exception) {
            Log.e("GetDisciplinas()", e.toString())
        }

        return disciplinas
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(disciplinas: List<Disciplina>) {
        super.onPostExecute(disciplinas)

        onDisciplinas(disciplinas)
    }
}

class GetAulas(private val idDisciplina: String, private val onAulas: KFunction1<List<Aula>, Unit>) :
    AsyncTask<Void, Void, List<Aula>>() {

    private fun toAulas(objetos: JSONArray): List<Aula> {
        val aulas = arrayListOf<Aula>()

        for (i in 0 until objetos.length()) {
            val objeto = objetos.get(i) as JSONObject

            val aula = Aula(
                objeto.getString("id"),
                objeto.getString("nome"),
                objeto.getString("detalhes"),
                arrayListOf<Conteudo>())

            aulas.add(aula)
        }

        return aulas
    }

    override fun doInBackground(vararg v: Void): List<Aula> {
        var aulas: List<Aula> = arrayListOf<Aula>()

        try {
            if (isAlive()) {
                val url = URL("$URL_AULAS/$idDisciplina")
                val conn = url.openConnection() as HttpURLConnection
                val json = toJson(conn.inputStream)

                val objetos = JSONArray(json)
                aulas = toAulas(objetos)
            }
        } catch (e: Exception) {
            Log.e("GetAulas()", e.toString())
        }

        return aulas
    }

    override fun onPostExecute(aulas: List<Aula>) {
        super.onPostExecute(aulas)

        onAulas(aulas)
    }

}

class GetConteudos(private val idAula: String, private val onConteudos: KFunction1<List<Conteudo>, Unit>) :
    AsyncTask<Void, Void, List<Conteudo>>() {

    private fun toConteudos(objetos: JSONArray): List<Conteudo> {
        val conteudos = arrayListOf<Conteudo>()

        for (i in 0 until objetos.length()) {
            val objeto = objetos.get(i) as JSONObject

            val conteudo = Conteudo(
                objeto.getString("id"),
                objeto.getString("nome"),
                objeto.getString("detalhes"),
                objeto.getString("objeto"),
                byteArrayOf(0)
            )

            conteudos.add(conteudo)
        }

        return conteudos
    }

    override fun doInBackground(vararg v: Void): List<Conteudo> {
        var conteudos: List<Conteudo> = arrayListOf<Conteudo>()

        try {
            if (isAlive()) {
                val url = URL("$URL_CONTEUDOS/$idAula")
                val conn = url.openConnection() as HttpURLConnection
                val json = toJson(conn.inputStream)

                val objetos = JSONArray(json)
                conteudos = toConteudos(objetos)
            }
        } catch (e: Exception) {
            Log.e("GetAulas()", e.toString())
        }

        return conteudos
    }

    override fun onPostExecute(conteudos: List<Conteudo>) {
        super.onPostExecute(conteudos)

        onConteudos(conteudos)
    }

}

class GetObjeto(private val conteudo: Conteudo,
                private val diretorioApp: File,
                private val onProgresso: KFunction1<Int, Unit>,
                private val onObjeto: KFunction1<String, Unit>) :
    AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg v: Void): String {
        var caminho = ""
        onProgresso(0)

        try {
            if (isAlive()) {
                val zip = getZip()
                caminho = unzip(zip)
            }
        } catch (e: Exception) {
            Log.e("GetObjeto()", e.toString())
        }

        return caminho
    }

    private fun getZip(): File {
        val url = URL("$URL_OBJETO/${conteudo.objeto}")
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("content-type", "binary/data");
        conn.connectTimeout = 60000
        val httpStream = conn.inputStream
        onProgresso(1)

        val zip = File("${diretorioApp.absolutePath}/objeto.${conteudo.id}.zip")
        if (zip.exists()) {
            zip.delete()
        }
        val zipStream = FileOutputStream(zip)
        onProgresso(2)

        var bytesLidos = -1;
        val buffer = ByteArray(4096)
        while ((httpStream.read(buffer).also { bytesLidos = it }) != -1) {
            zipStream.write(buffer, 0, bytesLidos)
        }
        onProgresso(3)

        httpStream.close();
        zipStream.close();

        return zip
    }

    private fun unzip(zip: File): String {
        val destino = "${diretorioApp.absolutePath}/objeto.${conteudo.id}"

        val dir = File(destino)
        if (dir.exists()) {
           dir.deleteRecursively()
        }
        dir.mkdir()
        onProgresso(4)

        ZipFile(zip.absolutePath).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    val arquivo = "$destino/${entry.name}"

                    if (!entry.isDirectory) {
                        extrairArquivo(input, arquivo)
                    } else {
                        val dir = File(arquivo)
                        dir.mkdir()
                    }
                }
            }
        }
        onProgresso(5)

        return destino
    }

    private fun extrairArquivo(inputStream: InputStream, destFilePath: String) {
        val bos = BufferedOutputStream(FileOutputStream(destFilePath))
        val bytesIn = ByteArray(1024)
        var read: Int
        while (inputStream.read(bytesIn).also { read = it } != -1) {
            bos.write(bytesIn, 0, read)
        }
        bos.close()
    }

    override fun onPostExecute(caminho: String) {
        super.onPostExecute(caminho)

        onObjeto(caminho)
    }

}