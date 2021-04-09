package com.example.graphvisualiser.queryingapi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

abstract class RetrieveGraph: AsyncTask<GraphInput, Void, Graph>(), ClientInterface {

    override fun doInBackground(vararg params: GraphInput?): Graph? {
        val graphInput = params[0]!!
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()

        var stringUrl = "https://api.wolframalpha.com/v2/query?appid=${graphInput.appID}&input=fit"
        for (coordinate in graphInput.coordinates) {
            stringUrl = "$stringUrl+(${coordinate.first},${coordinate.second})"
        }

        val url = URL(stringUrl)
        val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
        try {
            parser.setInput(urlConnection.inputStream, "UTF-8")
        } catch (e: Exception){
            e.printStackTrace()
            return null
        }

        var eventType = parser.eventType    // event type could be START_TAG or END_TAG, etc.
        val graph = Graph()

        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {    // while not tag for end of document
                if (eventType == XmlPullParser.START_TAG) {  // if start of tag
                    when (parser.name.toLowerCase()) {
                        "queryresult" -> {
                            graph.querySuccessful = parser.getAttributeValue(null, "success") == "true"
                        }
                        "pod" -> {
                            when (parser.getAttributeValue(null, "title")){
                                "Least-squares best fits" -> {
                                    parser.nextTag()    // pod start tag

                                    parser.nextTag()    // linear subpod start tag
                                    parser.nextTag()    // img tag is a single tag so only 1 tag
                                    parser.nextTag()    // plaintext start tag
                                    graph.linear = parser.nextText()
                                    parser.nextTag()    // plaintext end tag
                                    parser.nextTag()    // linear subpod end tag

                                    parser.nextTag()    // periodic subpod start tag
                                    parser.nextTag()    // img tag is a single tag so only 1 tag
                                    parser.nextTag()    // plaintext start tag
                                    graph.periodic = parser.nextText()
                                    parser.nextTag()    // plaintext end tag
                                    parser.nextTag()    // periodic subpod end tag

                                    parser.nextTag()    // logarithmic subpod start tag
                                    parser.nextTag()    // img tag is a single tag so only 1 tag
                                    parser.nextTag()    // plaintext end tag
                                    graph.logarithmic = parser.nextText()
                                    parser.nextTag()    // plaintext end tag
                                    parser.nextTag()    // logarithmic subpod end tag
                                }
                                "Plot" -> {
                                    parser.nextTag()    // subplot
                                    parser.nextTag()    // img
                                    graph.image = getBitmapFromURL(parser.getAttributeValue(null, "src"))
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()   // move onto next element
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace();
        } catch (e: XmlPullParserException) {
            e.printStackTrace();
        } catch (e: IOException) {
            e.printStackTrace();
        }

        return graph
    }

    override fun onPostExecute(result: Graph?) {
        onResponseReceived(result)
    }


    fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            // Log exception
            null
        }
    }
}