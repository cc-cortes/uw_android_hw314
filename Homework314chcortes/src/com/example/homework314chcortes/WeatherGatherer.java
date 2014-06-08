package com.example.homework314chcortes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

/*Gathers todays weather, as well as a forecast for the next three days */
public class WeatherGatherer {

	//Sample daily condition request: http://api.wunderground.com/api/68d5a3a8897f9bda/conditions/q/98122.xml
	//Sample 3 day forecast request: http://api.wunderground.com/api/68d5a3a8897f9bda/forecast/q/98122.xml
	
	
	//Members
	Handler completeHandler;
	public static String TODAYWEATHERLOADCOMPLETE = "todayWeatherLoadComplete";
	public static String FORECASTWEATHERLOADCOMPLETE = "forecastWeatherLoadComplete";
	public static String LOADERROR = "loadError";
	
		
	private String baseRequestUrlString = "http://api.wunderground.com/api/"; //Store the url for requests
	
	//API key removed for posting to github
	private String apiKey = "";
	private int zipcode;
	
	private String todayRequestUrlString;
	private String forecastRequestUrlString;
	
	private StringBuilder xmlBuilder; //String builder to put the xml together as it comes in from the web
	private static String USER_AGENT = "HelloHTTP/1.0"; //User agent for http requests
	private AndroidHttpClient httpClient; //http client for requests
	private Context context;
	
	//Doesn't scale very well. For future, could implement as array or put into DB instead
	WeatherDayInfo todaysWeather = new WeatherDayInfo();
	WeatherDayInfo dayOneWeather = new WeatherDayInfo();
	WeatherDayInfo dayTwoWeather = new WeatherDayInfo();
	WeatherDayInfo dayThreeWeather = new WeatherDayInfo();
	
	//Methods
	
	public WeatherGatherer() {
		// TODO Auto-generated constructor stub
	}
	
	public WeatherGatherer(Context appContext, Handler messageCompleteHandler, int zip) {
		setContext(appContext);
		
		setHandler(messageCompleteHandler);
		
		setZipcode(zip);
	}
	
	private void setHandler(Handler messageCompleteHandler) {
		completeHandler = messageCompleteHandler;
	}
	
	/** To be called when the loading of today's Weather data is complete. Passes message back to UI thread */
	private void callTodayWeatherLoadCompleteHandler(){
		
		Bundle data = new Bundle();
        data.putString(TODAYWEATHERLOADCOMPLETE, "true");
        Message msg = new Message();
        msg.setData(data);
        completeHandler.sendMessage(msg);
	}
	
	/** To be called when the loading of forecast Weather data is complete. Passes message back to UI thread */
	private void callForecastWeatherLoadCompleteHandler(){
		
		Bundle data = new Bundle();
        data.putString(FORECASTWEATHERLOADCOMPLETE, "true");
        Message msg = new Message();
        msg.setData(data);
        completeHandler.sendMessage(msg);
	}
	
	/** To be called when the loading of Weather data has errored out. Passes message back to UI thread */
	private void callLoadErrorHandler(){
		
		Bundle data = new Bundle();
        data.putString(LOADERROR, "true");
        Message msg = new Message();
        msg.setData(data);
        completeHandler.sendMessage(msg);
	}

	//Set the app's Context
	public void setContext(Context appContext){
		context = appContext;
	}
	
	/** get the context provided*/
	public Context getContext(){
		return context;
	}
	
	public void setZipcode(int zip){
		//Set the zipcode integer
		zipcode = zip;
		
		generateRequestUrls();
	}
	
	private void generateRequestUrls(){
		//generate the request for today's weather
		todayRequestUrlString = this.baseRequestUrlString + this.apiKey + "/conditions/q/" + String.valueOf(zipcode) + ".xml";
		
		//generate the request for the three day forecast
		forecastRequestUrlString = this.baseRequestUrlString + this.apiKey + "/forecast/q/" + String.valueOf(zipcode) + ".xml";
	}
	
	/**Load the feed into memory Asynchronously**/
	public void collectData(){
		//run two async tasks. One for today's weather and one for the three day forecast
		
		new GetAsyncTask().execute(todayRequestUrlString);
		
		new GetAsyncTask().execute(forecastRequestUrlString);
	}
	
	
	/*load from the DOM into an internal variable location */
	private void parseDomForTodaysWeather(Document document){
		
		//Fill in date as today
		Time now = new Time();
		now.setToNow();
		todaysWeather.date = now.format("%m/%e");
		
		//Get the Conditions
		//<current_observation><weather>
		NodeList weatherNodes = document.getElementsByTagName("weather");
		String weatherString = weatherNodes.item(0).getTextContent();
		todaysWeather.conditions = weatherString;
		
		//Get the Image
		//<current_observation><icon_url>
		NodeList iconUrlNodes = document.getElementsByTagName("icon_url");
		String iconUrlString = iconUrlNodes.item(0).getTextContent();
		todaysWeather.imageUrl = iconUrlString;
		
		//Get the temperature string (has both F and C)
		//<current_observation><temperature_string>
		NodeList temperatureNodes = document.getElementsByTagName("temperature_string");
		String temperatureString = temperatureNodes.item(0).getTextContent();
		todaysWeather.temperature = temperatureString;
		
		//Call Load Completed Handler
		callTodayWeatherLoadCompleteHandler();	
	}
	
	/* load the contents of the dom for the three day forecast into an internal variable location*/
	private void parseDomForForecastWeather(Document doc){
		
		//Day 1
		parseForecastDayWeather(doc, 1, dayOneWeather);
		
		//Day 2
		parseForecastDayWeather(doc, 2, dayTwoWeather);
		
		//Day 3
		parseForecastDayWeather(doc, 3, dayThreeWeather);
		
		//Call Load Completed Handler
		callForecastWeatherLoadCompleteHandler();
	}
	
	/*Loads the contents of the dom for the day indicator provided into the WeatherDayInfo provided*/
	private void parseForecastDayWeather(Document doc, int day, WeatherDayInfo dataContainer){
		//<forecast><simpleforecast><forecastdays><forecastday>
		//period starts at 0 for today's date, then increments by 1 for each day
		
		NodeList simpleForecastNodes = doc.getElementsByTagName("simpleforecast");
		Element simpleForecastElement = (Element) simpleForecastNodes.item(0);
		
		//Date in %m/%d
		//Month: <date><month>
		NodeList monthNodes = simpleForecastElement.getElementsByTagName("month");
		
		if(monthNodes.getLength() < day){
			return;
		}
		
		String monthString = monthNodes.item(day).getTextContent();
		
		//Day:  <date><day>
		NodeList dayNodes = simpleForecastElement.getElementsByTagName("day");
		
		if(dayNodes.getLength() < day){
			return;
		}
		
		String dayString = dayNodes.item(day).getTextContent();
		
		dataContainer.date = monthString + "/" + dayString;
		
		//Conditions
		NodeList conditionNodes = simpleForecastElement.getElementsByTagName("conditions");
		
		if(conditionNodes.getLength() < day){
			return;
		}
		
		String conditionsString = conditionNodes.item(day).getTextContent();
		dataContainer.conditions = conditionsString;
		
		//Image
		NodeList imageNodes = simpleForecastElement.getElementsByTagName("icon_url");
		
		if(imageNodes.getLength() < day){
			return;
		}
		
		String imageString = imageNodes.item(day).getTextContent();
		dataContainer.imageUrl = imageString;
		
		//Temperature
		//High in Celsius
		//<high><celsius>
		
		NodeList highTemperatureNodes = simpleForecastElement.getElementsByTagName("high");
		Element highTemperatureElement = (Element) highTemperatureNodes.item(day);
		NodeList highCelsiusNodes = highTemperatureElement.getElementsByTagName("celsius");
		String highCelsiusString = highCelsiusNodes.item(0).getTextContent();
		dataContainer.highCelsius = highCelsiusString;
		
		//Low in Celsius
		NodeList lowTemperatureNodes = simpleForecastElement.getElementsByTagName("low");
		Element lowTemperatureElement = (Element) lowTemperatureNodes.item(day);
		NodeList lowCelsiusNodes = lowTemperatureElement.getElementsByTagName("celsius");
		String lowCelsiusString = lowCelsiusNodes.item(0).getTextContent();
		dataContainer.lowCelsius = lowCelsiusString;
		
		return;
	}
	
	
	//https://github.com/uw/aad/blob/master/samples/HelloHTTP/src/aad/app/hello/http/MainActivity.java
	 /** An AsycTask used to update the retrieved HTTP header and content displays */
    private class GetAsyncTask extends AsyncTask<String, Void, Document> {
    	
        @Override
        protected Document doInBackground(String... urls) {


        	httpClient = AndroidHttpClient.newInstance(USER_AGENT);
             
             HttpResponse response = null;
             
             String urlString = urls[0];
             
             if (urlString == null) {
            	 Log.e("WeatherLoader Async Task", "No valid URL string provided");
            	 return null;
             }
             
             // Make a GET request and execute it to return the response 
             HttpGet request = new HttpGet(urlString);
             try {
                 response = httpClient.execute(request);
             }
             catch (IOException e) {
            	 //Connectivity Error. Unable to access internet
                 e.printStackTrace();
             }

            
            if (response == null) {
                Log.e("WeatherLoader Async Task", "Error accessing: " + urlString);
                return null;
            }
            
            // Get the content
            BufferedReader bf;
            StringBuilder sb = new StringBuilder();
            try {
                bf = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 8192);
                sb.setLength(0); // Reuse the StringBuilder
                String line;
                while ((line = bf.readLine()) != null) {
                    sb.append(line); //Read each line from the buffer and append it to the string builder
                }
                bf.close();
            }
            catch (IllegalStateException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            
            //Put contents of String Builder into the DOM
            
            Document doc;
            
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(false);
                dbf.setValidating(false);
                DocumentBuilder db = dbf.newDocumentBuilder();
                doc = db.parse(new InputSource(new StringReader(sb.toString())));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            
            httpClient.close();
			return doc;            
        }
        
        //After the pull is completed and contents in a dom, load into memory
        @Override
        protected void onPostExecute(Document doc) {


            if (doc == null) {
                Log.e("Get Async Task", "Error loading DOM");
                //Send load error message
                callLoadErrorHandler();
                return;
            }
            
            String featureString = parseDomForFeature(doc);
            
            if(featureString.contains("forecast")){//Retrieved the 3 day forecast
            	parseDomForForecastWeather(doc);
            }
            else if(featureString.contains("conditions")){ //retrieved the daily conditions
            	 parseDomForTodaysWeather(doc);
            }
                               
            super.onPostExecute(doc);
        }
        
        /*Get the string value from the feature tag */
        private String parseDomForFeature(Document doc){
        	NodeList featureNodes = doc.getElementsByTagName("feature");
        	
        	if(featureNodes.getLength() <= 0){
        		return "";
        	}
        	
        	String featureString = featureNodes.item(0).getTextContent();
        	
        	return featureString;
        }
        
    } //End of GetAsyncTask for getting XML

	



	
	

}
