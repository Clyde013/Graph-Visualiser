package com.example.graphvisualiser.queryingapi

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.util.Xml
import com.example.graphvisualiser.R
import org.jsoup.Jsoup
import org.jsoup.UncheckedIOException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

abstract class RetrieveGraph: AsyncTask<GraphInput, Void, Graph>(), ClientInterface {

    override fun doInBackground(vararg params: GraphInput?): Graph? {
        val graphInput = params[0]!!
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()

        var stringUrl = "http://api.wolframalpha.com/v2/query?appid=${graphInput.appID}&input=fit"
        for (coordinate in graphInput.coordinates) {
            stringUrl = "$stringUrl+(${coordinate.first},${coordinate.second})"
        }

        val url = URL(stringUrl)
        val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
        try {
            parser.setInput(urlConnection.inputStream, "UTF-8")
        } finally {
            urlConnection.disconnect()
        }

        /*
        In XML files such as https://news.google.com/rss/search?q=vaccine&gl=US&hl=en-US&ceid=US:en
        an article is normally enclosed in the <item> tags and using the variable insideItem to
        track our input ensures that the rss feed name's <title> tag is not read and only reads one
        article/<item> at a time
         */

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
                            if (parser.getAttributeValue(null, "title") == "Least-squares best fits"){
                                // TODO iterate through subpods
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

}